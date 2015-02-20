/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.setupwizardlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.LayoutDirection;
import android.view.Gravity;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import com.android.setupwizardlib.R;

/**
 * Class to draw the illustration of setup wizard. The aspectRatio attribute determines the aspect
 * ratio of the top padding, which is leaving space for the illustration. Draws an illustration
 * (foreground) to fit the width of the view and fills the rest with the background.
 *
 * If an aspect ratio is set, then the aspect ratio of the source drawable is maintained. Otherwise
 * the the aspect ratio will be ignored, only increasing the width of the illustration.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Illustration extends FrameLayout {

    // Size of the baseline grid in pixels
    private float mBaselineGridSize;
    private Drawable mBackground;
    private Drawable mIllustration;
    private final Rect mViewBounds = new Rect();
    private final Rect mIllustrationBounds = new Rect();
    private float mScale = 1.0f;
    private float mAspectRatio = 0.0f;

    public Illustration(Context context) {
        this(context, null);
    }

    public Illustration(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Illustration(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public Illustration(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.SuwIllustration, 0, 0);
            mAspectRatio = a.getFloat(R.styleable.SuwIllustration_suwAspectRatio, 0.0f);
            a.recycle();
        }
        // Number of pixels of the 8dp baseline grid as defined in material design specs
        mBaselineGridSize = getResources().getDisplayMetrics().density * 8;
        setWillNotDraw(false);
    }

    /**
     * The background will be drawn to fill up the rest of the view. It will also be scaled by the
     * same amount as the foreground so their textures look the same.
     */
    @Override
    public void setBackground(Drawable background) {
        if (background == mBackground) {
            return;
        }
        mBackground = background;
        invalidate();
        requestLayout();
    }

    /**
     * Sets the drawable used as the illustration. The drawable is expected to have intrinsic
     * width and height defined and will be scaled to fit the width of the view.
     */
    public void setIllustration(Drawable illustration) {
        if (illustration == mIllustration) {
            return;
        }
        mIllustration = illustration;
        invalidate();
        requestLayout();
    }

    @Override
    @Deprecated
    public void setForeground(Drawable d) {
        setIllustration(d);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mAspectRatio != 0.0f) {
            int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
            int illustrationHeight = (int) (parentWidth / mAspectRatio);
            illustrationHeight -= illustrationHeight % mBaselineGridSize;
            setPadding(0, illustrationHeight, 0, 0);
        }
        setOutlineProvider(ViewOutlineProvider.BOUNDS);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;
        if (mIllustration != null) {
            int intrinsicWidth = mIllustration.getIntrinsicWidth();
            int intrinsicHeight = mIllustration.getIntrinsicHeight();
            final int layoutDirection = getLayoutDirection();

            mViewBounds.set(0, 0, layoutWidth, layoutHeight);
            if (mAspectRatio != 0f) {
                mScale = layoutWidth / (float) intrinsicWidth;
                intrinsicWidth = layoutWidth;
                intrinsicHeight = (int) (intrinsicHeight * mScale);
            }
            Gravity.apply(Gravity.FILL_HORIZONTAL | Gravity.TOP, intrinsicWidth,
                    intrinsicHeight, mViewBounds, mIllustrationBounds, layoutDirection);
            mIllustration.setBounds(mIllustrationBounds);
        }
        if (mBackground != null) {
            // Scale the background bounds by the same scale to compensate for the scale done to the
            // canvas in onDraw.
            mBackground.setBounds(0, 0, (int) Math.ceil(layoutWidth / mScale),
                    (int) Math.ceil((layoutHeight - mIllustrationBounds.height()) / mScale));
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public void onDraw(Canvas canvas) {
        final int layoutDirection = getLayoutDirection();
        if (mBackground != null) {
            // Draw the background filling parts not covered by the illustration
            canvas.save();
            canvas.translate(0, mIllustrationBounds.height());
            // Scale the background so its size matches the foreground
            canvas.scale(mScale, mScale, 0, 0);
            if (layoutDirection == LayoutDirection.RTL && mBackground.isAutoMirrored()) {
                // TODO: When Drawable.setLayoutDirection becomes public API, use that instead
                canvas.scale(-1, 1);
                canvas.translate(-mBackground.getBounds().width(), 0);
            }
            mBackground.draw(canvas);
            canvas.restore();
        }
        if (mIllustration != null) {
            canvas.save();
            if (layoutDirection == LayoutDirection.RTL && mIllustration.isAutoMirrored()) {
                // TODO: When Drawable.setLayoutDirection becomes public API, use that instead
                canvas.scale(-1, 1);
                canvas.translate(-mIllustrationBounds.width(), 0);
            }
            // Draw the illustration
            mIllustration.draw(canvas);
            canvas.restore();
        }
        super.onDraw(canvas);
    }
}
