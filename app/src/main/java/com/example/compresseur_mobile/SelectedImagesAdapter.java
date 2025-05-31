package com.example.compresseur_mobile;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Uri uri);
    }

    private final List<Uri> images;
    private final Context context;
    private final OnItemClickListener listener;

    public SelectedImagesAdapter(Context ctx, List<Uri> images, OnItemClickListener listener) {
        this.context = ctx;
        this.images = images;
        this.listener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb;
        public ViewHolder(View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.img_thumbnail);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_images, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = images.get(position);
        Glide.with(context)
                .load(uri)
                .override(256, 256)
                .centerCrop()
                .into(holder.imgThumb);

        holder.imgThumb.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(uri);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }
}
