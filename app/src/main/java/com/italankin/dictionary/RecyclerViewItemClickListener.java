package com.italankin.dictionary;

import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public abstract class RecyclerViewItemClickListener implements RecyclerView.OnItemTouchListener {

    private GestureDetector mGestureDetector;

    public RecyclerViewItemClickListener(final RecyclerView recyclerView) {
        mGestureDetector = new GestureDetector(recyclerView.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (childView != null) {
                    onItemClick(childView, recyclerView.getChildAdapterPosition(childView), true);
                }
            }
        });
    }

    @Override
    public final boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        int x = Math.round(e.getX());
        int y = Math.round(e.getY());
        View childView = view.findChildViewUnder(x, y);
        if (childView != null && mGestureDetector.onTouchEvent(e)) {
            onItemClick(childView, view.getChildAdapterPosition(childView), false);
        }
        return false;
    }

    @Override
    public final void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    @Override
    public final void onTouchEvent(RecyclerView view, MotionEvent motionEvent) {
    }

    public abstract void onItemClick(View childView, int position, boolean isLongClick);

}