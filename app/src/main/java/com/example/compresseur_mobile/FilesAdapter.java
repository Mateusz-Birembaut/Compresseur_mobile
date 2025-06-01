package com.example.compresseur_mobile;


import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
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
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
                Toast.makeText(getContext(), getContext().getString(R.string.toast_delete_failure), Toast.LENGTH_SHORT).show();
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
                //android.util.Log.d("FilesAdapter", "Résultat de la décompression : width=" + result.width + ", height=" + result.height + ", quality=" + result.quality + ", method=" + result.method);

                fragment.requireActivity().runOnUiThread(() -> {

                });

                int rotation = CompressionFragment.getExifRotation(Uri.fromFile(file), getContext());
                Uri uri = CompressionFragment.byteArrayToUri(result, rotation ,  getContext());

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


        ImageButton sendButton = convertView.findViewById(R.id.send_button);
        sendButton.setOnClickListener(v -> {
            Context ctx = getContext();
            Uri contentUri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, contentUri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(share, "Envoyer l’image via"));
        });

        ImageButton exportButton = convertView.findViewById(R.id.export_button);
        exportButton.setOnClickListener(v -> {
            fragment.requireActivity().runOnUiThread(() -> {
                fragment.cardLoading.setVisibility(View.VISIBLE);
                fragment.v_loading_overlay.setVisibility(View.VISIBLE);
            });

            new Thread(() -> {
                String compressedPath = file.getAbsolutePath();
                CompressionResult result = CompressionFragment.decompressImageNative(getContext(), compressedPath);

                int rotation = CompressionFragment.getExifRotation(Uri.fromFile(file), getContext());
                Uri tempUri = CompressionFragment.byteArrayToUri(result, rotation, getContext());
                if (tempUri == null) {
                    fragment.requireActivity().runOnUiThread(() -> {
                        fragment.cardLoading.setVisibility(View.GONE);
                        fragment.v_loading_overlay.setVisibility(View.GONE);
                        Toast.makeText(getContext(), getContext().getString(R.string.toast_decompress_error), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                Context ctx = getContext();
                String mimeType = "image/png";
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName().replace(".compressed", ".png"));
                values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CompressedApp");
                Uri uriDest = ctx.getContentResolver()
                        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uriDest == null) {
                    fragment.requireActivity().runOnUiThread(() -> {
                        fragment.cardLoading.setVisibility(View.GONE);
                        fragment.v_loading_overlay.setVisibility(View.GONE);
                        Toast.makeText(ctx, getContext().getString(R.string.toast_media_entry_error), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                try (OutputStream out = ctx.getContentResolver().openOutputStream(uriDest);
                     InputStream in = ctx.getContentResolver().openInputStream(tempUri)) {

                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.flush();

                    File tmp = new File(tempUri.getPath());
                    if (tmp.exists()) tmp.delete();

                    fragment.requireActivity().runOnUiThread(() -> {
                        fragment.cardLoading.setVisibility(View.GONE);
                        fragment.v_loading_overlay.setVisibility(View.GONE);
                        Toast.makeText(ctx, getContext().getString(R.string.toast_export_success), Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    fragment.requireActivity().runOnUiThread(() -> {
                        fragment.cardLoading.setVisibility(View.GONE);
                        fragment.v_loading_overlay.setVisibility(View.GONE);
                        Toast.makeText(ctx, getContext().getString(R.string.toast_export_failure), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });


        return convertView;
    }

}

