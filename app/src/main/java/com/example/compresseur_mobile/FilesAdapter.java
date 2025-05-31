package com.example.compresseur_mobile;


import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FilesAdapter extends ArrayAdapter<File> {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy HH:mm", Locale.getDefault());

    CompressedFileListFragment fragment;
    public FilesAdapter(Context context, List<File> files, CompressedFileListFragment fragment) {
        super(context, 0, files);
        this.fragment = fragment;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.compressed_file_single, parent, false);
        }

        File file = getItem(position);
        if (file != null) {
            TextView filenameView = convertView.findViewById(R.id.filename_text);
            TextView dateView = convertView.findViewById(R.id.date_text);

            filenameView.setText(file.getName());
            dateView.setText(dateFormat.format(new Date(file.lastModified())));
        }

        ImageButton deleteButton = convertView.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(v -> {
            if (file.delete()) {
                remove(file);
                notifyDataSetChanged();
            } else {
                Toast.makeText(getContext(), "Impossible de supprimer le fichier", Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton decompressButton = convertView.findViewById(R.id.decompress_button);
        decompressButton.setOnClickListener(v -> {

            String filePath = file.getAbsolutePath();

            fragment.requireActivity().runOnUiThread(() -> {
                fragment.cardLoading.setVisibility(View.VISIBLE);
                fragment.v_loading_overlay.setVisibility(View.VISIBLE);
            });


            new Thread(() -> {
                CompressionResult result = CompressionFragment.decompressImageNative(getContext(), filePath);
                android.util.Log.d("FilesAdapter", "Résultat de la décompression : width=" + result.width + ", height=" + result.height + ", quality=" + result.quality + ", method=" + result.method);

                fragment.requireActivity().runOnUiThread(() -> {

                });

                Uri uri = CompressionFragment.byteArrayToUri(result, getContext());

                //on affiche l'image zoomée
                fragment.requireActivity().runOnUiThread(() -> {
                    CompressionViewModel vm = new ViewModelProvider(fragment.requireActivity()).get(CompressionViewModel.class);
                    vm.setZoomUri(uri);
                    fragment.requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new ImageZoomFragment())
                            .addToBackStack(null)
                            .commit();

                    fragment.cardLoading.setVisibility(View.GONE);
                    fragment.v_loading_overlay.setVisibility(View.GONE);
                });


            }).start();


        });

        return convertView;
    }
}

