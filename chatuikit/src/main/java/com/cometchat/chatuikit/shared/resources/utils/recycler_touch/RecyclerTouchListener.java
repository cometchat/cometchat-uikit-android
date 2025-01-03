package com.cometchat.chatuikit.shared.resources.utils.recycler_touch;

import android.content.Context;
import android.graphics.Canvas;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerTouchListener extends ItemTouchHelper.SimpleCallback implements RecyclerView.OnItemTouchListener {
    private static final String TAG = RecyclerTouchListener.class.getSimpleName();
    private ClickListener clickListener;
    private GestureDetector gestureDetector;
    private RecyclerItemSwipeListener swipeListener;

    public RecyclerTouchListener(RecyclerView recyclerView, int dragDirs, int swipeDirs, RecyclerItemSwipeListener listener) {
        super(dragDirs, swipeDirs);
        this.swipeListener = listener;
    }

    public RecyclerTouchListener(Context var1, final RecyclerView recyclerView, final ClickListener var3) {
        super(0, 0);
        this.clickListener = var3;
        this.gestureDetector = new GestureDetector(var1, new GestureDetector.SimpleOnGestureListener() {
            public boolean onSingleTapUp(@NonNull MotionEvent mVar) {
                return true;
            }

            public void onLongPress(@NonNull MotionEvent mVar) {
                View view = recyclerView.findChildViewUnder(mVar.getX(), mVar.getY());
                if (view != null && var3 != null) {
                    var3.onLongClick(view, recyclerView.getChildAdapterPosition(view));
                }
            }
        });
    }

    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        View view = recyclerView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());
        if (view != null && this.clickListener != null && this.gestureDetector.onTouchEvent(motionEvent)) {
            this.clickListener.onClick(view, recyclerView.getChildAdapterPosition(view));
            return true;
        }
        return false;
    }

    public void onTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent motionEvent) {

    }

    public void onRequestDisallowInterceptTouchEvent(boolean var1) {

    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return true;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {

    }

    @Override
    public void onChildDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        //swipeListener.onSwiped(viewHolder, direction, viewHolder.getAdapterPosition());
        swipeListener.onSwiped(viewHolder, direction, viewHolder.getAbsoluteAdapterPosition());
    }

    @Override
    public int convertToAbsoluteDirection(int flags, int layoutDirection) {
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    public interface RecyclerItemSwipeListener {
        void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position);
    }
}
