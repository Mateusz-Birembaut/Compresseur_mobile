package com.example.compresseur_mobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class CompressedFileListFragment extends Fragment {


    private File[] compressedFileList;

    public MaterialCardView cardLoading;
    public View v_loading_overlay;

    private static final int PICK_COMPRESSED_FILE = 2001;

    private FilesAdapter adapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.compressed_file_list, container, false);

        ListView listView = view.findViewById(R.id.list_view);
        cardLoading = view.findViewById(R.id.card_loading_2);
        v_loading_overlay = view.findViewById(R.id.overlay_loading_2);

        //on recup les fichiers compressés dans le stockage interne
        File dir = requireContext().getFilesDir();
        compressedFileList = dir.listFiles(file -> file.getName().endsWith(".compressed"));

        if (compressedFileList == null || compressedFileList.length == 0) {
            Toast.makeText(requireContext(), getString(R.string.toast_no_compressed_files), Toast.LENGTH_SHORT).show();
        }

        /*
        for (File file : compressedFileList) {
            Log.d("CompressedFile", "Nom: " + file.getName() + ", Date: " + new java.util.Date(file.lastModified()));
        }
         */

        //on trie les fichiers, plus récent en premier
        Arrays.sort(compressedFileList, (f1, f2)    ->Long.compare(f2.lastModified(), f1.lastModified()));
        adapter = new FilesAdapter(requireContext(), new ArrayList<>(Arrays.asList(compressedFileList)), this   );
        listView.setAdapter(adapter);


        Button importButton = view.findViewById(R.id.btn_import_compressed);
        importButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_COMPRESSED_FILE);
        });

        ImageButton backButton = view.findViewById(R.id.bt_back_to_menu);
        backButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeMenuFragment())
                    .commit();});

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_COMPRESSED_FILE && resultCode == Activity.RESULT_OK) {
            Uri pickedUri = data.getData();

            if (pickedUri == null) return;

            String name = null;

            try (Cursor cursor = requireContext().getContentResolver()
                    .query(pickedUri,
                            new String[]{OpenableColumns.DISPLAY_NAME},
                            null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    name = cursor.getString(cursor.getColumnIndexOrThrow(
                            OpenableColumns.DISPLAY_NAME));
                }
            }

            if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".compressed")) {
                Toast.makeText(requireContext(),
                        getString(R.string.toast_invalid_file),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Context ctx = requireContext();
            String filename = "imported_" + System.currentTimeMillis() + ".compressed";
            File dest = new File(ctx.getFilesDir(), filename);

            try (InputStream in = ctx.getContentResolver().openInputStream(pickedUri);
                 FileOutputStream out = new FileOutputStream(dest)) {

                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }

                out.flush();
                adapter.add(dest);
                adapter.notifyDataSetChanged();
                Toast.makeText(ctx, getString(R.string.toast_import_success), Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(ctx, getString(R.string.toast_import_failure), Toast.LENGTH_SHORT).show();
            }
        }
    }



}
