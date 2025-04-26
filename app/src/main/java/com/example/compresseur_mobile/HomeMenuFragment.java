package com.example.compresseur_mobile;


import android.content.pm.PackageManager;
import android.os.Bundle;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class HomeMenuFragment extends Fragment {

    private ActivityResultLauncher<String> requestCameraPermission;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private ActivityResultLauncher<Uri> takePicture;
    private Uri photoURI;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_menu, container, false);

        requestCameraPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        takePhoto();
                    } else {
                        Toast.makeText(requireContext(), "Permission caméra refusée", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: " + uri);
            } else {
                Log.d("PhotoPicker", "No media selected");
            }
        });

        takePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
            if (result) {
                Log.d("PhotoPicker", "Photo taken successfully: " + photoURI);
            } else {
                Log.d("PhotoPicker", "Failed to take photo");
            }
        });


        ImageButton btnChooseImg = view.findViewById(R.id.btn_choose_photo);
        ImageButton btnTakeImg= view.findViewById(R.id.btn_take_photo);
        ImageButton btnUploadImg = view.findViewById(R.id.btn_view_photo);

        btnChooseImg.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnTakeImg.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                requestCameraPermission.launch(android.Manifest.permission.CAMERA);
            }
        });

        btnUploadImg.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });


        return view;
    }

    private void takePhoto() {
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.e("PhotoPicker", "Error occurred while creating the File");
        }
        if (photoFile != null) {
            photoURI = FileProvider.getUriForFile(requireContext(), "com.example.compresseur_mobile.fileprovider", photoFile);
            takePicture.launch(photoURI);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void intentToCompress() {

    }

    private void intentToDecompress() {

    }

}
