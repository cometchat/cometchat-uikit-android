package com.cometchat.chatuikit.extensions.sticker.keyboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cometchat.chatuikit.R;
import com.cometchat.chatuikit.extensions.sticker.keyboard.adapter.StickersAdapter;
import com.cometchat.chatuikit.extensions.sticker.keyboard.listener.StickerClickListener;
import com.cometchat.chatuikit.extensions.sticker.keyboard.model.Sticker;
import com.cometchat.chatuikit.shared.resources.utils.recycler_touch.ClickListener;
import com.cometchat.chatuikit.shared.resources.utils.recycler_touch.RecyclerTouchListener;

import java.util.ArrayList;
import java.util.List;

public class StickerFragment extends Fragment {
    private static final String TAG = StickerFragment.class.getSimpleName();
    StickerClickListener stickerClickListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cometchat_fragment_stickers_view, container, false);
        RecyclerView rvStickers = view.findViewById(R.id.rvStickers);
        rvStickers.setLayoutManager(new GridLayoutManager(getContext(), 4));
        List<Sticker> list = new ArrayList<>();
        if (getArguments() != null) {
            list = getArguments().getParcelableArrayList("stickerList");
        }
        StickersAdapter adapter = new StickersAdapter(getContext(), list);
        rvStickers.setAdapter(adapter);

        rvStickers.addOnItemTouchListener(new RecyclerTouchListener(getContext(), rvStickers, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Sticker sticker = (Sticker) view.getTag(R.string.cometchat_sticker_lowercase);
                if (stickerClickListener != null) stickerClickListener.onClickListener(sticker);
            }
        }));
        return view;
    }

    public void setStickerClickListener(StickerClickListener stickerClickListener) {
        this.stickerClickListener = stickerClickListener;
    }
}
