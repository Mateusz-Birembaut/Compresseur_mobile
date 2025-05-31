package com.example.compresseur_mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class SelectedImagesResultAdapter extends RecyclerView.Adapter<SelectedImagesResultAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Uri uri, int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Uri uri, int position);
    }

    private final List<Uri> images;
    private final Context context;
    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public SelectedImagesResultAdapter(
            Context ctx,
            List<Uri> images,
            OnItemClickListener clickListener,
            OnItemLongClickListener longClickListener
    ) {
        this.context = ctx;
        this.images = images;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgThumb;
        final View container;
        ViewHolder(View itemView) {
            super(itemView);
            container = itemView;
            imgThumb = itemView.findViewById(R.id.img_thumbnail);
        }
    }

    @NonNull
    @Override
    public SelectedImagesResultAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_images, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectedImagesResultAdapter.ViewHolder holder, int position) {
        Uri uri = images.get(position);

        Bitmap thumb = decodeSampledBitmapFromUri(context, uri, 256, 256);
        if (thumb != null) {
            Bitmap rotated = rotateBitmapIfRequired(context, thumb, uri);
            holder.imgThumb.setImageBitmap(rotated);
        }

        if (position == selectedPosition) {
            holder.container.setBackgroundResource(R.drawable.bg_selected_thumbnail);
        } else {
            holder.container.setBackground(null);
        }


        holder.imgThumb.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            if (prev != RecyclerView.NO_POSITION) {
                notifyItemChanged(prev);
            }
            notifyItemChanged(selectedPosition);

            if (clickListener != null) {
                clickListener.onItemClick(uri, selectedPosition);
            }
        });

        holder.imgThumb.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(uri, holder.getAdapterPosition());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public void setSelectedPosition(int pos) {
        if (pos == selectedPosition) return;
        int prev = selectedPosition;
        selectedPosition = pos;
        if (prev != RecyclerView.NO_POSITION) {
            notifyItemChanged(prev);
        }
        if (selectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedPosition);
        }
    }

    public void clearSelection() {
        int prev = selectedPosition;
        selectedPosition = RecyclerView.NO_POSITION;
        if (prev != RecyclerView.NO_POSITION) {
            notifyItemChanged(prev);
        }
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Bitmap decodeSampledBitmapFromUri(Context ctx, Uri uri, int reqWidth, int reqHeight) {
        try (InputStream input = ctx.getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        BitmapFactory.Options options2 = new BitmapFactory.Options();
        options2.inSampleSize = calculateInSampleSize(options2, reqWidth, reqHeight);
        options2.inJustDecodeBounds = false;

        try (InputStream input2 = ctx.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(input2, null, options2);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    private Bitmap rotateBitmapIfRequired(Context ctx, Bitmap img, Uri selectedImage) {
        try {
            InputStream input = ctx.getContentResolver().openInputStream(selectedImage);
            ExifInterface exif = new ExifInterface(input);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );
            input.close();

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return img;
        }
    }

    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(
                img, 0, 0, img.getWidth(), img.getHeight(), matrix, true
        );
    }
}
