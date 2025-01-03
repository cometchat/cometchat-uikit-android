package com.cometchat.chatuikit.shared.resources.utils.sticker_header;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StickyHeaderDecoration extends RecyclerView.ItemDecoration {
    private static final String TAG = StickyHeaderDecoration.class.getSimpleName();
    public static final long NO_HEADER_ID = -1L;
    private final Map<Long, RecyclerView.ViewHolder> mHeaderCache;
    private final StickyHeaderAdapter mAdapter;
    private final boolean mRenderInline;

    public StickyHeaderDecoration(StickyHeaderAdapter stickyHeaderAdapter) {
        this(stickyHeaderAdapter, false);
    }

    public StickyHeaderDecoration(StickyHeaderAdapter stickyHeaderAdapter, boolean flag) {
        this.mAdapter = stickyHeaderAdapter;
        this.mHeaderCache = new HashMap<>();
        this.mRenderInline = flag;
    }

    public void getItemOffsets(@NonNull Rect rect, @NonNull View view, RecyclerView recyclerView, @NonNull RecyclerView.State state) {
        int var5 = recyclerView.getChildAdapterPosition(view);
        int var6 = 0;
        if (var5 != -1 && this.hasHeader(var5) && this.showHeaderAboveItem(var5)) {
            View var7 = this.getHeader(recyclerView, var5).itemView;
            var6 = this.getHeaderHeightForLayout(var7);
        }

        rect.set(0, var6, 0, 0);
    }

    private boolean showHeaderAboveItem(int var1) {
        if (var1 == 0) {
            return true;
        } else {
            return this.mAdapter.getHeaderId(var1 - 1) != this.mAdapter.getHeaderId(var1);
        }
    }

    public void clearHeaderCache() {
        this.mHeaderCache.clear();
    }

    private boolean hasHeader(int var1) {
        return this.mAdapter.getHeaderId(var1) != -1L;
    }

    private RecyclerView.ViewHolder getHeader(RecyclerView var1, int var2) {
        long var3 = this.mAdapter.getHeaderId(var2);
        if (this.mHeaderCache.containsKey(var3)) {
            return this.mHeaderCache.get(var3);
        } else {
            RecyclerView.ViewHolder var5 = this.mAdapter.onCreateHeaderViewHolder(var1);
            View var6 = var5.itemView;
            this.mAdapter.onBindHeaderViewHolder(var5, var2, var3);
            int var7 = View.MeasureSpec.makeMeasureSpec(var1.getMeasuredWidth(), View.MeasureSpec.EXACTLY);
            int var8 = View.MeasureSpec.makeMeasureSpec(var1.getMeasuredHeight(), View.MeasureSpec.UNSPECIFIED);
            int var9 = ViewGroup.getChildMeasureSpec(var7, var1.getPaddingLeft() + var1.getPaddingRight(), var6.getLayoutParams().width);
            int var10 = ViewGroup.getChildMeasureSpec(var8, var1.getPaddingTop() + var1.getPaddingBottom(), var6.getLayoutParams().height);
            var6.measure(var9, var10);
            var6.layout(0, 0, var6.getMeasuredWidth(), var6.getMeasuredHeight());
            this.mHeaderCache.put(var3, var5);
            return var5;
        }
    }

    public void onDrawOver(@NonNull Canvas canvas, RecyclerView var2, @NonNull RecyclerView.State state) {
        int var4 = var2.getChildCount();
        long var5 = -1L;
        for (int var7 = 0; var7 < var4; ++var7) {
            View var8 = var2.getChildAt(var7);
            int var9 = var2.getChildAdapterPosition(var8);
            if (var9 != -1 && this.hasHeader(var9)) {
                long var10 = this.mAdapter.getHeaderId(var9);
                if (var10 != var5) {
                    var5 = var10;
                    View var12 = this.getHeader(var2, var9).itemView;
                    canvas.save();
                    int var13 = var8.getLeft();
                    int var14 = this.getHeaderTop(var2, var8, var12, var9, var7);
                    canvas.translate((float) var13, (float) var14);
                    var12.setTranslationX((float) var13);
                    var12.setTranslationY((float) var14);
                    var12.draw(canvas);
                    canvas.restore();
                }
            }
        }
    }

    private int getHeaderTop(RecyclerView var1, View var2, View var3, int var4, int var5) {
        int var6 = this.getHeaderHeightForLayout(var3);
        int var7 = (int) var2.getY() - var6;
        if (var5 == 0) {
            int var8 = var1.getChildCount();
            long var9 = this.mAdapter.getHeaderId(var4);

            for (int var11 = 1; var11 < var8; ++var11) {
                int var12 = var1.getChildAdapterPosition(var1.getChildAt(var11));
                if (var12 != -1) {
                    long var13 = this.mAdapter.getHeaderId(var12);
                    if (var13 != var9) {
                        View var15 = var1.getChildAt(var11);
                        int var16 = (int) var15.getY() - (var6 + this.getHeader(var1, var12).itemView.getHeight());
                        if (var16 < 0) {
                            return var16;
                        }
                        break;
                    }
                }
            }

            var7 = Math.max(0, var7);
        }

        return var7;
    }

    private int getHeaderHeightForLayout(View var1) {
        return this.mRenderInline ? 0 : var1.getHeight();
    }
}
