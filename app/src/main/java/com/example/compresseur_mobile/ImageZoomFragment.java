package com.example.compresseur_mobile;

import android.net.Uri;
import android.os.Bundle;
import android.support.media.ExifInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.Observer;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.IOException;
import java.io.InputStream;

public class ImageZoomFragment extends Fragment {

    private CompressionViewModel viewModel;
    private SubsamplingScaleImageView imgZoomView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CompressionViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.image_zoom, container, false);
        imgZoomView = view.findViewById(R.id.iv_img);
        ImageButton btBack = view.findViewById(R.id.bt_back_to_menu);

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        navigateBack();
                    }
                }
        );

        btBack.setOnClickListener(v -> navigateBack());

        viewModel.getZoomUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null) {
                int rotation = getImgRotation(uri);
                imgZoomView.setImage(ImageSource.uri(uri));
                imgZoomView.setOrientation(rotation);
            }
        });

        return view;
    }

    private void navigateBack() {
        requireActivity()
                .getSupportFragmentManager()
                .popBackStack();
    }

    private int getImgRotation(Uri uri) {
        int rotation = SubsamplingScaleImageView.ORIENTATION_0;
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            assert in != null;
            ExifInterface exif = new ExifInterface(in);
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = SubsamplingScaleImageView.ORIENTATION_90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = SubsamplingScaleImageView.ORIENTATION_180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = SubsamplingScaleImageView.ORIENTATION_270;
                    break;
                default:
            }
        } catch (IOException e) {
            return 0;
        }
        return rotation;
    }
}
