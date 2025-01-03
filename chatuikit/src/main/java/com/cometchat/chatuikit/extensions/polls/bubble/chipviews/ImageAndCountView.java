package com.cometchat.chatuikit.extensions.polls.bubble.chipviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.shared.resources.utils.Utils;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ImageAndCountView extends MaterialCardView {
    private static final String TAG = ImageAndCountView.class.getSimpleName();
    private ImageAndCountAdapter adapter;
    private TextView countView;

    public ImageAndCountView(Context context) {
        super(context);
        init(context);
    }

    public ImageAndCountView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImageAndCountView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        Utils.initMaterialCard(this);
        View view = View.inflate(context, R.layout.cometchat_image_and_count_layout, null);
        RecyclerView recyclerView = view.findViewById(R.id.image_res_view);
        adapter = new ImageAndCountAdapter(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, true));
        recyclerView.setAdapter(adapter);
        countView = view.findViewById(R.id.count_view);
        addView(view);
    }

    public void setData(List<ImageTextPoJo> list, int count) {
        adapter.setList(list);
        String c = count + "";
        countView.setText(c);
    }

    public void setAvatarStyle(@StyleRes int style) {
        adapter.setAvatarStyle(style);
    }

    public void setCountTextAppearance(@StyleRes int style) {
        countView.setTextAppearance(style);
    }

    public void setCountTextColor(@ColorInt int color) {
        countView.setTextColor(color);
    }
}
