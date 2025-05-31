package com.example.compresseur_mobile;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class ResultCompressionFragment extends Fragment {

    private CompressionViewModel vm;

    private TabLayout tabLayout;
    private RecyclerView rvCompressed, rvOriginal;
    private TextView tvTaux, tvPsnr, tvOldSize, tvNewSize;
    private ImageButton btBack;
    private int selectedIndex = 0;

    private SelectedImagesResultAdapter compressedAdapter;
    private SelectedImagesResultAdapter originalAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(CompressionViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.result_compression, container, false);

        btBack = view.findViewById(R.id.bt_back_to_menu);
        tabLayout = view.findViewById(R.id.tab_layout_images);

        rvCompressed = view.findViewById(R.id.rv_compressed);
        rvOriginal   = view.findViewById(R.id.rv_original);

        tvTaux    = view.findViewById(R.id.tv_taux);
        tvPsnr    = view.findViewById(R.id.tv_psnr);
        tvOldSize = view.findViewById(R.id.tv_old_size);
        tvNewSize = view.findViewById(R.id.tv_new_size);

        // ––––– BACK BUTTON –––––
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

        // ––––– RECYCLERVIEW –––––
        GridLayoutManager gridLayoutManager1 = new GridLayoutManager(requireContext(), 3);
        rvCompressed.setLayoutManager(gridLayoutManager1);
        GridLayoutManager gridLayoutManager2 = new GridLayoutManager(requireContext(), 3);
        rvOriginal.setLayoutManager(gridLayoutManager2);

        List<Uri> compressedUris = new ArrayList<>();
        List<Uri> originalUris   = new ArrayList<>();

        compressedAdapter = new SelectedImagesResultAdapter(requireContext(), compressedUris,
                // appuie court
                (uri, pos) -> {
                    selectedIndex = pos;
                    updateInfoCard();
                    compressedAdapter.setSelectedPosition(pos);
                    originalAdapter.clearSelection();
                },
                // appuie long
                (uri, pos) -> {
                    vm.setZoomUri(uri);
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new ImageZoomFragment())
                            .addToBackStack(null)
                            .commit();
                }
        );
        originalAdapter = new SelectedImagesResultAdapter(requireContext(), originalUris,
                (uri, pos) -> {
                    selectedIndex = pos;
                    updateInfoCard();
                    originalAdapter.setSelectedPosition(pos);
                    compressedAdapter.clearSelection();
                },
                (uri, pos) -> {
                    vm.setZoomUri(uri);
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new ImageZoomFragment())
                            .addToBackStack(null)
                            .commit();
                }
        );

        rvCompressed.setAdapter(compressedAdapter);
        rvOriginal.setAdapter(originalAdapter);

        vm.getResultUris().observe(getViewLifecycleOwner(), resultUris -> {
            if (resultUris != null) {
                compressedUris.clear();
                compressedUris.addAll(resultUris);
                compressedAdapter.notifyDataSetChanged();
                if (!compressedUris.isEmpty()) {
                    selectedIndex = 0;
                    compressedAdapter.setSelectedPosition(0);
                }
                updateInfoCard();
            }
        });
        vm.getImgUris().observe(getViewLifecycleOwner(), originalList -> {
            if (originalList != null) {
                originalUris.clear();
                originalUris.addAll(originalList);
                originalAdapter.notifyDataSetChanged();
                if (!originalUris.isEmpty()) {
                    selectedIndex = 0;
                    originalAdapter.setSelectedPosition(0);
                }
                updateInfoCard();
            }
        });

        vm.getTauxList().observe(getViewLifecycleOwner(), tauxList -> {
            if (tauxList != null && selectedIndex < tauxList.size()) {
                updateInfoCard();
            }
        });
        vm.getPsnrList().observe(getViewLifecycleOwner(), psnr -> updateInfoCard());
        vm.getOldSizes().observe(getViewLifecycleOwner(), oldS -> updateInfoCard());
        vm.getNewSizes().observe(getViewLifecycleOwner(), newS -> updateInfoCard());



        // ––––– TABS –––––
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 0) {
                    rvCompressed.setVisibility(View.VISIBLE);
                    rvOriginal.setVisibility(View.GONE);
                    compressedAdapter.setSelectedPosition(selectedIndex);
                } else {
                    rvCompressed.setVisibility(View.GONE);
                    rvOriginal.setVisibility(View.VISIBLE);
                    originalAdapter.setSelectedPosition(selectedIndex);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        TabLayout.Tab defaultTab = tabLayout.getTabAt(0);
        if (defaultTab != null) defaultTab.select();

        updateInfoCard();

        return view;
    }


    @SuppressLint("SetTextI18n")
    private void updateInfoCard() {
        List<Float> tauxList   = vm.getTauxList().getValue();
        List<Float> psnrList   = vm.getPsnrList().getValue();
        List<Long> oldSizes      = vm.getOldSizes().getValue();
        List<Long> newSizes      = vm.getNewSizes().getValue();

        DecimalFormat df = new DecimalFormat("#,##0");

        if (tauxList != null && selectedIndex < tauxList.size()) {
            tvTaux.setText(getString(R.string.tv_result_compression_rate) + " : " + tauxList.get(selectedIndex) + "×");
        } else {
            tvTaux.setText(getString(R.string.tv_result_compression_rate) + " : -");
        }
        if (psnrList != null && selectedIndex < psnrList.size()) {
            tvPsnr.setText(getString(R.string.tv_result_PSNR) + " : " + psnrList.get(selectedIndex) + " dB");
        } else {
            tvPsnr.setText(getString(R.string.tv_result_PSNR) + " : -");
        }
        if (oldSizes != null && selectedIndex < oldSizes.size()) {
            long bytes = oldSizes.get(selectedIndex);
            if (bytes < 1_000_000) {
                tvOldSize.setText(getString(R.string.tv_result_input_size) + " : " + df.format(bytes / 1_000) + " KB");
            } else {
                tvOldSize.setText(getString(R.string.tv_result_input_size) + " : " + df.format(bytes / 1_000_000) + " MB");
            }
        } else {
            tvOldSize.setText(getString(R.string.tv_result_input_size) + " : -");
        }
        if (newSizes != null && selectedIndex < newSizes.size()) {
            long bytes = newSizes.get(selectedIndex);
            if (bytes < 1_000_000) {
                tvNewSize.setText(getString(R.string.tv_result_output_size) + " : " + df.format(bytes / 1_000) + " KB");
            } else {
                tvNewSize.setText(getString(R.string.tv_result_output_size) + " : " + df.format(bytes / 1_000_000) + " MB");
            }
        } else {
            tvNewSize.setText(getString(R.string.tv_result_output_size) + " : -");
        }
    }
}
