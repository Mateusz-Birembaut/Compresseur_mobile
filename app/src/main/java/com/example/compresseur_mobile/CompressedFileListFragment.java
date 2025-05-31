package com.example.compresseur_mobile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class CompressedFileListFragment extends Fragment {


    private File[] compressedFileList;

    public MaterialCardView cardLoading;
    public View v_loading_overlay;


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
            Toast.makeText(requireContext(), "Vos images compréssés apparaitront ici !", Toast.LENGTH_SHORT).show();
            return view;
        }

        for (File file : compressedFileList) {
            Log.d("CompressedFile", "Nom: " + file.getName() + ", Date: " + new java.util.Date(file.lastModified()));
        }

        //on trie les fichiers, plus récent en premier
        Arrays.sort(compressedFileList, (f1, f2)    ->Long.compare(f2.lastModified(), f1.lastModified()));
        FilesAdapter adapter = new FilesAdapter(requireContext(), new ArrayList<>(Arrays.asList(compressedFileList)), this   );
        listView.setAdapter(adapter);




        return view;
    }


}
