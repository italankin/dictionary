package com.italankin.dictionary.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import com.italankin.dictionary.R;

public class CustomLayout extends ViewGroup {

    private final Rect mTmpContainerRect = new Rect();
    private final Rect mTmpChildRect = new Rect();

    /**
     * Maximum height of the child views.
     */
    private int mChildMaxHeight = 0;
    /**
     * Additional spacing between items.
     */
    private int mChildSpacing = 0;

    public CustomLayout(Context context) {
        super(context);
    }

    public CustomLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomLayout);
        try {
            mChildSpacing = a.getDimensionPixelSize(R.styleable.CustomLayout_childSpacing, 0);
        } finally {
            a.recycle();
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mChildMaxHeight = 0;

        final int padding = getPaddingLeft() + getPaddingRight();
        final int count = getChildCount();
        int parentWidth = getMeasuredWidth();
        int childState = 0;
        int rowWidth = 0;
        int rows = 0;

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                childState = combineMeasuredStates(childState, child.getMeasuredState());

                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final int height = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
                final int width = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;

                // determine max height
                if (height > mChildMaxHeight) {
                    mChildMaxHeight = height;
                }

                // check if child does not fit into current row
                if (rowWidth + width + padding + mChildSpacing > parentWidth && i != 0) {
                    rowWidth = 0;
                    rows++;
                }
                // if child is not first item in the row, we should add extra spacing
                if (rowWidth > 0) {
                    rowWidth += mChildSpacing;
                }
                rowWidth += width;
            }
        }

        // compute parent height based on rows
        int parentHeight = mChildMaxHeight * (rows + 1) + mChildSpacing * rows;
        parentHeight += getPaddingTop() + getPaddingBottom();

        parentHeight = Math.max(parentHeight, getSuggestedMinimumHeight());
        parentWidth = Math.max(parentWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSizeAndState(parentWidth, widthMeasureSpec, childState),
                resolveSizeAndState(parentHeight, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingLeft();
        int top = getPaddingTop();

        final int maxWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        final int gravity = Gravity.TOP | Gravity.START;
        final int count = getChildCount();
        int rowWidth = 0;

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                // check if child does not fit into current row
                if (rowWidth + width + lp.leftMargin + lp.rightMargin + mChildSpacing > maxWidth
                        && i != 0) {
                    top += mChildMaxHeight + mChildSpacing;
                    rowWidth = 0;
                }
                // if child is not first item in the row, we should add extra spacing
                if (rowWidth > 0) {
                    rowWidth += mChildSpacing;
                }

                // form child rect
                mTmpContainerRect.left = left + rowWidth + lp.leftMargin;
                mTmpContainerRect.top = top + lp.topMargin;
                mTmpContainerRect.bottom = top + mChildMaxHeight;
                mTmpContainerRect.right = mTmpContainerRect.left + width + lp.rightMargin;
                Gravity.apply(gravity, width, height, mTmpContainerRect, mTmpChildRect);
                child.layout(mTmpChildRect.left, mTmpChildRect.top, mTmpChildRect.right,
                        mTmpChildRect.bottom);

                rowWidth += mTmpContainerRect.width();
            }
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CustomLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    public static class LayoutParams extends MarginLayoutParams {
        public LayoutParams() {
            this(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

}
