package com.katy.telegram.Utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ReplacementSpan;

public class CenteredImageSpan extends ReplacementSpan {
    private final Drawable drawable;

    public CenteredImageSpan(final Drawable drawable) {
        this.drawable = drawable;
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    }

    @Override
    public int getSize(final Paint paint, final CharSequence text, final int start, final int end, final Paint.FontMetricsInt fm) {
        return drawable.getIntrinsicWidth();
    }

    @Override
    public void draw(final Canvas canvas, final CharSequence text, final int start, final int end, final float x, final int top, final int y, final int bottom, final Paint paint) {
        final Paint.FontMetrics metrics = paint.getFontMetrics();
        final float padding = (metrics.descent - metrics.ascent - drawable.getIntrinsicHeight()) / 2f;
        final float transY = bottom - drawable.getIntrinsicHeight() - padding;
        canvas.save();
        canvas.translate(x, transY);
        drawable.draw(canvas);
        canvas.restore();
    }
}