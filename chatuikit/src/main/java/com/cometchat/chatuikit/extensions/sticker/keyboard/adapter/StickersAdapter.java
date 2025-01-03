package com.cometchat.chatuikit.extensions.sticker.keyboard.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.extensions.sticker.keyboard.model.Sticker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Purpose - UserListAdapter is a subclass of RecyclerView Adapter which is used
 * to display the list of users. It helps to organize the users in recyclerView.
 *
 * <p>
 * Created on - 20th December 2019
 *
 * <p>
 * Modified on - 23rd March 2020
 */
public class StickersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = StickersAdapter.class.getSimpleName();
    private final Context context;

    private List<Sticker> stickerArrayList = new ArrayList<>();

    private static final int STICKER_IMAGE = 1;

    /**
     * It is a constructor which is used to initialize wherever we needed.
     *
     * @param context is a object of Context.
     */
    public StickersAdapter(Context context) {
        this.context = context;
    }

    /**
     * It is constructor which takes stickerArrayList as parameter and bind it with
     * stickerArrayList in adapter.
     *
     * @param context          is a object of Context.
     * @param stickerArrayList is a list of stickers used in this adapter.
     */
    public StickersAdapter(Context context, List<Sticker> stickerArrayList) {
        setStickerList(stickerArrayList);
        this.context = context;
    }

    private void setStickerList(List<Sticker> stickerArrayList) {
        stickerArrayList.sort(Comparator.comparing(Sticker::getSetName));
        this.stickerArrayList = stickerArrayList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.cometchat_stickers_row, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        setStickerData((ImageViewHolder) viewHolder, i);
    }

    private void setStickerData(ImageViewHolder viewHolder, int i) {
        Sticker sticker = stickerArrayList.get(i);
        if (sticker != null && sticker.getUrl() != null) {
            if (sticker.getUrl().contains("gif")) {
                loadGifInToImageView(sticker.getUrl(), viewHolder.progressBar, viewHolder.imageView);
            } else {
                loadBitmapIntoImageView(sticker.getUrl(), viewHolder.progressBar, viewHolder.imageView);
            }
        }
        viewHolder.itemView.setTag(R.string.cometchat_sticker_lowercase, sticker);
    }

    /**
     * Loads a bitmap image into the ImageView from a URL.
     *
     * <p>
     * This method uses Glide to load a bitmap image from the specified URL into the
     * associated ImageView. It handles caching, placeholder, and visibility of the
     * progress bar during the loading process.
     *
     * @param url The URL of the image to be loaded
     */
    private void loadBitmapIntoImageView(String url, ProgressBar progressBar, ImageView imageView) {
        RequestBuilder<Bitmap> builder = Glide.with(context).asBitmap();
        builder.diskCacheStrategy(DiskCacheStrategy.DATA).placeholder(0).error(R.drawable.cometchat_image_placeholder).skipMemoryCache(false).load(url).addListener(new RequestListener<Bitmap>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object o, @NonNull Target<Bitmap> target, boolean b) {
                progressBar.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(@NonNull Bitmap bitmap, @NonNull Object o, Target<Bitmap> target, @NonNull DataSource dataSource, boolean b) {
                progressBar.setVisibility(View.GONE);
                return false;
            }
        }).into(imageView);
    }

    /**
     * Loads a GIF image into the ImageView from a URL.
     *
     * <p>
     * This method uses Glide to load a GIF image from the specified URL into the
     * associated ImageView. It handles caching, placeholder, and visibility of the
     * progress bar during the loading process.
     *
     * @param imageUrl The URL of the GIF to be loaded, or null to load from a file.
     */
    private void loadGifInToImageView(String imageUrl, ProgressBar progressBar, ImageView imageView) {
        RequestBuilder<GifDrawable> gifDrawableRequestBuilder = Glide.with(context).asGif();
        gifDrawableRequestBuilder.diskCacheStrategy(DiskCacheStrategy.DATA).placeholder(0).error(R.drawable.cometchat_image_placeholder).skipMemoryCache(false).load(imageUrl).addListener(new RequestListener<GifDrawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<GifDrawable> target, boolean isFirstResource) {
                progressBar.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(@NonNull GifDrawable resource, @NonNull Object model, Target<GifDrawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                progressBar.setVisibility(View.GONE);
                return false;
            }
        }).into(imageView);
    }

    @Override
    public int getItemCount() {
        return stickerArrayList.size();
    }

    public void updateStickerList(@NonNull List<Sticker> stickerList) {
        setStickerList(stickerList);
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final ProgressBar progressBar;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }
    }
}
