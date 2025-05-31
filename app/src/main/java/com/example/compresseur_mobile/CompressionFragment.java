package com.example.compresseur_mobile;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

public class CompressionFragment extends Fragment {

    private CompressionViewModel vm;
    private Uri imgURI;
    List<Uri> listeUris = new ArrayList<>();
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

        RecyclerView rv = view.findViewById(R.id.rv_selected_images);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 3);
        rv.setLayoutManager(gridLayoutManager);

        List<Uri> listeUris = new ArrayList<>();
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
            if (uris != null) {
                listeUris.clear();
                listeUris.addAll(uris);
                adapter.notifyDataSetChanged();
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
            //String imgUriString = getArguments().getString("img_uri");
            //if (imgUriString != null) vm.setImgUri(Uri.parse(imgUriString));
            int method = getArguments().getInt("selected_method", -1);
            if (method != -1) vm.setSelectedMethod(method);
            int quality = getArguments().getInt("quality", -1);
            if (quality != -1) vm.setQuality(quality);
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
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                qualityValue.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        bt_compress.setOnClickListener(v -> startFakeCompression());

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

            //vm.setResultUri(fakeResultUri);
            //vm.setTaux(fakeTaux);
            //vm.setPsnr(fakePSNR);
            //vm.setOldSize(oldSize);
            //vm.setNewSize(newSize);

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
