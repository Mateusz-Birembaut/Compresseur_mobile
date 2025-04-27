package com.example.compresseur_mobile;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.tabs.TabLayout;

public class ResultCompressionFragment extends Fragment {

    private CompressionViewModel vm;

    // Champs pour stocker les deux URI
    private Uri compressedUri;
    private Uri originalUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(CompressionViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.result_compression, container, false);


        ImageView ivImg       = view.findViewById(R.id.iv_img);
        TabLayout tabLayout   = view.findViewById(R.id.tab_layout_images);
        TextView tvTaux       = view.findViewById(R.id.tv_taux);
        TextView tvPsnr       = view.findViewById(R.id.tv_psnr);
        TextView tvOldSize    = view.findViewById(R.id.tv_old_size);
        TextView tvNewSize    = view.findViewById(R.id.tv_new_size);
        ImageButton btBack    = view.findViewById(R.id.bt_back_to_menu);

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new CompressionFragment())
                                .commit();
                    }
                }
        );
        btBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new CompressionFragment())
                .commit()
        );

        vm.getSelectedTab().observe(getViewLifecycleOwner(), index -> {
            TabLayout.Tab t = tabLayout.getTabAt(index);
            if (t != null && !t.isSelected()) {
                t.select();
            }
        });

        vm.getResultUri().observe(getViewLifecycleOwner(), uri -> {
            compressedUri = uri;
            if (tabLayout.getSelectedTabPosition() == 0) {
                ivImg.setImageURI(compressedUri);
            }
        });

        vm.getImgUri().observe(getViewLifecycleOwner(), uri -> {
            originalUri = uri;
            if (tabLayout.getSelectedTabPosition() == 1) {
                ivImg.setImageURI(originalUri);
            }
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                vm.setSelectedTab(pos);
                if (pos == 0 && compressedUri != null) {
                    ivImg.setImageURI(compressedUri);
                } else if (pos == 1 && originalUri != null) {
                    ivImg.setImageURI(originalUri);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        ivImg.setOnClickListener(v -> navigateToZoom(
                (tabLayout.getSelectedTabPosition() == 0) ? compressedUri : originalUri
        ));

        vm.getTaux().observe(getViewLifecycleOwner(), t -> tvTaux.setText(t + "×"));
        vm.getPsnr().observe(getViewLifecycleOwner(), p -> tvPsnr.setText(p + " dB"));
        vm.getOldSize().observe(getViewLifecycleOwner(), s -> tvOldSize.setText(formatSize(s)));
        vm.getNewSize().observe(getViewLifecycleOwner(), s -> tvNewSize.setText(formatSize(s)));

        return view;
    }

    private void navigateToZoom(Uri uri) {
        vm.setZoomUri(uri);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ImageZoomFragment())
                .addToBackStack(null)
                .commit();
    }

    private String formatSize(long bytes) {
        if (bytes < 1_000_000) {
            return (bytes / 1_000) + " KB";
        } else {
            return (bytes / 1_000_000) + " MB";
        }
    }
}
