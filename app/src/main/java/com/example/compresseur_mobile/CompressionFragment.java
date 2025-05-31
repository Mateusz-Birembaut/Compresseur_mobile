package com.example.compresseur_mobile;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.support.media.ExifInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

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
    public native CompressionResult compressImageNative(Context context, byte[] imageBytes, int quality, int method, String imgName);

    private CompressionViewModel vm;
    private SeekBar qualityBar;
    private RadioGroup compressionMethodsGroup;
    private MaterialCardView cardLoading;
    private Button bt_compress;
    private View v_loading_overlay;

    private List<Uri> listeUris = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(CompressionViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.compression, container, false);

        compressionMethodsGroup = view.findViewById(R.id.rg_compression_methods);
        qualityBar = view.findViewById(R.id.seekBar_quality);
        TextView qualityValue = view.findViewById(R.id.tv_quality_value);
        ImageButton back = view.findViewById(R.id.bt_back_to_menu);
        bt_compress = view.findViewById(R.id.bt_compress);
        cardLoading = view.findViewById(R.id.card_loading);
        v_loading_overlay = view.findViewById(R.id.overlay_loading);

        RecyclerView rv = view.findViewById(R.id.rv_selected_images);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 3);
        rv.setLayoutManager(gridLayoutManager);

        SelectedImagesAdapter adapter = new SelectedImagesAdapter(requireContext(), listeUris,
                uri -> {
                    vm.setZoomUri(uri);
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new ImageZoomFragment())
                            .addToBackStack(null)
                            .commit();
                }
        );
        rv.setAdapter(adapter);

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        vm.clearResults();
                        vm.clearImgUris();
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new HomeMenuFragment())
                                .commit();
                    }
                }
        );

        vm.getImgUris().observe(getViewLifecycleOwner(), uris -> {
            listeUris.clear();
            if (uris != null) {
                listeUris.addAll(uris);
            }
            adapter.notifyDataSetChanged();
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
            int method = getArguments().getInt("selected_method", -1);
            if (method != -1) {
                vm.setSelectedMethod(method);
            }
            int quality = getArguments().getInt("quality", -1);
            if (quality != -1) {
                vm.setQuality(quality);
            }
        }

        back.setOnClickListener(v -> {
            vm.clearResults();
            vm.clearImgUris();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeMenuFragment())
                    .commit();
        });

        qualityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                qualityValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        bt_compress.setOnClickListener(v -> startCompressionMultiple());

        return view;
    }


    private void startCompressionMultiple() {
        if (listeUris.isEmpty()) {
            return;
        }

        bt_compress.setEnabled(false);
        v_loading_overlay.setVisibility(View.VISIBLE);
        cardLoading.setVisibility(View.VISIBLE);

        vm.clearResults();

        final int selectedMethod  = getSelectedCompressionMethod();
        final int selectedQuality = qualityBar.getProgress();

        //thread pour separer ui / calcul
        new Thread(() -> {
            for (int idx = 0; idx < listeUris.size(); idx++) {
                Uri currentUri = listeUris.get(idx);

                int rotation = getExifRotation(currentUri);

                try {
                    InputStream inputStream = requireContext()
                            .getContentResolver()
                            .openInputStream(currentUri);

                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[4096];
                    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    buffer.flush();
                    byte[] imageBytes = buffer.toByteArray();
                    long oldSize = imageBytes.length;

                    String imgName = "image_multi_" + System.currentTimeMillis() + "_" + idx + ".compressed";

                    CompressionResult result = compressImageNative(
                            requireContext(),
                            imageBytes,
                            selectedQuality,
                            selectedMethod,
                            imgName
                    );

                    if (result == null || result.compressedImage == null) {
                        Log.e("CompressionFragment", "Compression native null pour URI " + currentUri);
                        continue;
                    }


                    result.oldSize = oldSize;
                    result.compressionRatio = (float) result.oldSize / result.newSize;

                    Uri compressedUri = byteArrayToUri(result, rotation);
                    if (compressedUri == null) {
                        Log.e("CompressionFragment", "Impossible de créer l'URI de sortie pour l'image #" + idx);
                        continue;
                    }

                    vm.addResultUri(compressedUri);
                    vm.addTaux(result.compressionRatio);
                    vm.addPsnr(result.psnr);
                    vm.addOldSize(result.oldSize);
                    vm.addNewSize(result.newSize);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                v_loading_overlay.setVisibility(View.GONE);
                cardLoading.setVisibility(View.GONE);
                bt_compress.setEnabled(true);

                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ResultCompressionFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }).start();
    }


    private int getSelectedCompressionMethod() {
        int selectedId = compressionMethodsGroup.getCheckedRadioButtonId();
        return (selectedId == R.id.rb_method_1) ? 1 : 2;
    }


    private Uri byteArrayToUri(CompressionResult result, int rotation) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(
                    result.width,
                    result.height,
                    Bitmap.Config.ARGB_8888
            );
            int totalPixels = result.width * result.height;
            int[] rawData = new int[totalPixels];
            for (int i = 0; i < totalPixels; i++) {
                int r = result.compressedImage[i * 3] & 0xFF;
                int g = result.compressedImage[i * 3 + 1] & 0xFF;
                int b = result.compressedImage[i * 3 + 2] & 0xFF;
                rawData[i] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
            bitmap.setPixels(rawData, 0, result.width, 0, 0, result.width, result.height);

            // rotation sinon les images de l'appareil sont pas dans le bon sens
            Bitmap rotatedBitmap = bitmap;
            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                rotatedBitmap = Bitmap.createBitmap(
                        bitmap,
                        0, 0,
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        matrix,
                        true
                );
                bitmap.recycle();
            }

            File tempFile = File.createTempFile("compressed_rotated_", ".png", requireContext().getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
            return Uri.fromFile(tempFile);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private int getExifRotation(@NonNull Uri uri) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            if (is == null) {
                return 0;
            }
            ExifInterface exif = new ExifInterface(is);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


}
