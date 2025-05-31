package com.example.compresseur_mobile;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class HomeMenuFragment extends Fragment {

    private ActivityResultLauncher<String> requestCameraPermission;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia;
    private ActivityResultLauncher<Uri> takePicture;
    private Uri imgURI;

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

        pickMultipleMedia = registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(9),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        CompressionViewModel vm = new ViewModelProvider(requireActivity()).get(CompressionViewModel.class);
                        vm.setImgUris(uris);
                        goToCompressionMultiple();
                    } else {
                        Toast.makeText(requireContext(), "Aucune image sélectionnée", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        takePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
            if (result) {
                Log.d("PhotoPicker", "Photo taken successfully: " + imgURI);
                CompressionViewModel vm = new ViewModelProvider(requireActivity()).get(CompressionViewModel.class);
                vm.clearImgUris();
                vm.addImgUri(imgURI);
                goToCompressionMultiple();
            } else {
                Log.d("PhotoPicker", "Failed to take photo");
            }
        });


        ImageButton btnChooseImg = view.findViewById(R.id.btn_choose_photo);
        ImageButton btnTakeImg= view.findViewById(R.id.btn_take_photo);
        ImageButton btnUploadImg = view.findViewById(R.id.btn_view_photo);

        btnChooseImg.setOnClickListener(v -> {
            pickMultipleMedia.launch(
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build()
            );
        });

        btnTakeImg.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA);
            }
        });

        btnUploadImg.setOnClickListener(v -> {
            /*
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());

             */
        });



        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        List<Language> langs = new ArrayList<>();
        langs.add(new Language("fr", R.drawable.fr, getString(R.string.lang_fr)));
        langs.add(new Language("en", R.drawable.gb, getString(R.string.lang_en)));
        langs.add(new Language("es", R.drawable.es, getString(R.string.lang_es)));
        langs.add(new Language("de", R.drawable.de, getString(R.string.lang_de)));
        langs.add(new Language("pl", R.drawable.pl, getString(R.string.lang_pl)));
        langs.add(new Language("pt", R.drawable.pt, getString(R.string.lang_pt)));

        LanguageAdapter adapter = new LanguageAdapter(requireContext(), langs);
        AutoCompleteTextView dropdown = view.findViewById(R.id.language_dropdown);
        dropdown.setAdapter(adapter);
        dropdown.setThreshold(0);
        dropdown.setOnClickListener(v -> {
            dropdown.showDropDown();
        });

        dropdown.setOnItemClickListener((parent, itemView, position, id) -> {
            Language sel = adapter.getItem(position);
            if (sel != null) {
                androidx.preference.PreferenceManager
                        .getDefaultSharedPreferences(requireContext())
                        .edit()
                        .putString("app_lang", sel.code)
                        .apply();
                AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(sel.code)
                );
                dropdown.setText(sel.displayName, false);
                dropdown.setCompoundDrawablesWithIntrinsicBounds(sel.flag, 0, 0, 0);

            }
        });

        String current = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getString("app_lang", Locale.getDefault().getLanguage());
        for (int i = 0; i < langs.size(); i++) {
            if (langs.get(i).code.equals(current)) {
                dropdown.setText(langs.get(i).displayName, false);
                dropdown.setCompoundDrawablesWithIntrinsicBounds(langs.get(i).flag, 0, 0, 0);
                break;
            }
        }

    }

    private void takePhoto() {
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.e("PhotoPicker", "Error occurred while creating the File");
        }
        if (photoFile != null) {
            imgURI = FileProvider.getUriForFile(requireContext(), "com.example.compresseur_mobile.fileprovider", photoFile);
            takePicture.launch(imgURI);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }


    private void goToCompressionMultiple() {
        CompressionFragment compressionFragment = new CompressionFragment();

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, compressionFragment)
                .commit();
    }

    private void intentToDecompress() {
    }

}
