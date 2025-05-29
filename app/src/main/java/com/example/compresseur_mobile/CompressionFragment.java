package com.example.compresseur_mobile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

class CompressionResult {
    public byte[] compressedImage;
    public int quality;
    public int method;
    public int width;
    public int height;

    public long oldSize;
    public long newSize;
    public float compressionRatio;
    public float psnr;
}

public class CompressionFragment extends Fragment {

    static {
        System.loadLibrary("native-lib");
    }

    public native String stringFromJNI();
    public native CompressionResult compressImageNative(Context context, byte[] imageBytes, int quality, int method);

    private CompressionViewModel vm;
    private Uri imgURI;
    private SeekBar qualityBar;
    private RadioGroup compressionMethodsGroup;
    private MaterialCardView cardLoading;
    private Button bt_compress;
    private View v_loading_overlay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(CompressionViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.compression, container, false);

        compressionMethodsGroup = view.findViewById(R.id.rg_compression_methods);
        qualityBar = view.findViewById(R.id.seekBar_quality);
        TextView qualityValue = view.findViewById(R.id.tv_quality_value);
        ImageButton back = view.findViewById(R.id.bt_back_to_menu);
        bt_compress = view.findViewById(R.id.bt_compress);
        cardLoading = view.findViewById(R.id.card_loading);
        v_loading_overlay = view.findViewById(R.id.overlay_loading);
        ImageView img = view.findViewById(R.id.iv_img);

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        vm.clearResults();
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new HomeMenuFragment())
                                .commit();
                    }
                }
        );

        vm.getImgUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                imgURI = uri;
                img.setImageURI(uri);
                img.setOnClickListener(v -> goToImgZoom());
            }
        });

        vm.getSelectedMethod().observe(getViewLifecycleOwner(), method -> {
            if (method != null) {
                compressionMethodsGroup.check(
                        method == 1 ? R.id.rb_method_1 : R.id.rb_method_2
                );
            }
        });

        vm.getQuality().observe(getViewLifecycleOwner(), q -> {
            if (q != null) {
                qualityBar.setProgress(q);
                qualityValue.setText(String.valueOf(q));
            }
        });

        if (getArguments() != null) {
            String imgUriString = getArguments().getString("img_uri");
            if (imgUriString != null) vm.setImgUri(Uri.parse(imgUriString));
            int method = getArguments().getInt("selected_method", -1);
            if (method != -1) vm.setSelectedMethod(method);
            int quality = getArguments().getInt("quality", -1);
            if (quality != -1) vm.setQuality(quality);
        }

        back.setOnClickListener(v -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HomeMenuFragment())
                .commit()
        );

        qualityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                qualityValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        bt_compress.setOnClickListener(v -> startCompression());

        return view;
    }

    private void goToImgZoom() {
        vm.setSelectedMethod(getSelectedCompressionMethod());
        vm.setQuality(qualityBar.getProgress());
        vm.setZoomUri(imgURI);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ImageZoomFragment())
                .addToBackStack(null)
                .commit();
    }

    private int getSelectedCompressionMethod() {
        int selectedId = compressionMethodsGroup.getCheckedRadioButtonId();
        return selectedId == R.id.rb_method_1 ? 1 : 2;
    }

    private void startCompression(){

        bt_compress.setEnabled(false);
        v_loading_overlay.setVisibility(View.VISIBLE);
        cardLoading.setVisibility(View.VISIBLE);

        new Thread(() -> { //thread obligatoire sinon crash car l'appli ne répond plus
            try {
                //lit les octets de l'image depuis l'URI
                InputStream inputStream = requireContext().getContentResolver().openInputStream(imgURI);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[4096];
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                byte[] imageBytes = buffer.toByteArray();

                int method = getSelectedCompressionMethod();
                int quality = qualityBar.getProgress();

                //lance la compression en c++
                CompressionResult result = compressImageNative(requireContext(), imageBytes, quality, method);

                result.oldSize = imageBytes.length;
                //result.newSize = 0;

                requireActivity().runOnUiThread(() -> {
                    if (result == null || result.compressedImage == null) {
                        android.util.Log.d("CompressionFragment", "Return data/image is null !!");
                    } else {
                        android.util.Log.d("CompressionFragment", "Compression finished: quality=" + quality +
                                ", method=" + method +
                                ", oldSize=" + result.oldSize +
                                ", newSize=" + result.newSize +
                                ", result=[" + result.width + "x" + result.height + "], PSNR=" + result.psnr);


                        result.compressionRatio = (float) result.oldSize / result.newSize;


                        //for (int i = 0; i < 10 && result.compressedImage != null && i < result.compressedImage.length; i++) {
                        //    android.util.Log.d("CompressionFragment", "compressedImage[" + i + "] = " + (result.compressedImage[i] ));
                        //}

                        displayCompressedImage(result);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    //affiche l'image après compression
    private void displayCompressedImage(CompressionResult result) {
        if (result == null || result.compressedImage == null) {
            android.util.Log.d("CompressionFragment", "Return data/image is null !!");
            return;
        }

        android.util.Log.d("CompressionFragment", "converting and displaying compressed image");

        Uri imageUri = byteArrayToUri(result);

        vm.setResultUri(imageUri);
        vm.setTaux( result.compressionRatio);
        vm.setPsnr(result.psnr);
        vm.setOldSize(result.oldSize);
        vm.setNewSize(result.newSize);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ResultCompressionFragment())
                .addToBackStack(null)
                .commit();

        v_loading_overlay.setVisibility(View.GONE);
        cardLoading.setVisibility(View.GONE);
        bt_compress.setEnabled(true);

    }

    //convertit le tableau d'octets en URI
    private Uri byteArrayToUri(CompressionResult result){
        try {

            android.util.Log.d("CompressionFragment", "compressedImage length: " + (result.compressedImage != null ? result.compressedImage.length : "null"));

            //on convertit le tableau d'octets rgb , j'ai pas trouve comment faire autrement
            Bitmap bitmap = Bitmap.createBitmap(result.width, result.height, Bitmap.Config.ARGB_8888);

            int[] rawData = new int[result.width * result.height];

            for(int i = 0; i < result.width * result.height; i++){

                int r = result.compressedImage[i*3] & 0xFF; // en java les byte sont forcemment signé ???? donc AND 0xFF pour passer de byte signé à int non signé,
                int g = result.compressedImage[i*3+1] & 0xFF;
                int b = result.compressedImage[i*3+2] & 0xFF;
                rawData[i] = (0xFF << 24) | (r << 16) | (g << 8) | b; // alpha, red, green, blue
            }

            bitmap.setPixels(rawData, 0, result.width, 0, 0, result.width, result.height);
            android.util.Log.d("CompressionFragment", "image decoded ");

            //on save l'image dans le cache en png juste pour pouvoir l'afficher
            java.io.File tempFile = java.io.File.createTempFile("compressed", ".png", requireContext().getCacheDir());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Uri uri = Uri.fromFile(tempFile);
            return uri;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private void startFakeCompression() {
        bt_compress.setEnabled(false);
        v_loading_overlay.setVisibility(View.VISIBLE);
        cardLoading.setVisibility(View.VISIBLE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Uri fakeResultUri = uriFromDrawable(R.drawable.bt_arrow_back);
            int fakeTaux = 2;
            int fakePSNR = 40;
            long oldSize = 10_000_000;
            long newSize = 5_000_000;

            android.util.Log.d("CompressionFragment", "stringFromJNI() retourne : " + stringFromJNI());

            vm.setResultUri(fakeResultUri);
            vm.setTaux(fakeTaux);
            vm.setPsnr(fakePSNR);
            vm.setOldSize(oldSize);
            vm.setNewSize(newSize);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ResultCompressionFragment())
                    .addToBackStack(null)
                    .commit();

            v_loading_overlay.setVisibility(View.GONE);
            cardLoading.setVisibility(View.GONE);
            bt_compress.setEnabled(true);
        }, 2000);
    }

    private Uri uriFromDrawable(int resId) {
        return Uri.parse("android.resource://" +
                requireContext().getPackageName() + "/" + resId);
    }

}
