package com.protonnetwork.proton;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.ReplacementSpan;

public class RoundRectBackgroundSpan extends ReplacementSpan {


	private int textWidth;
	private int backgroundColor;
	private int radius;
	private int textColor;
	private int textSize;
	private int horizontalSpace;

	public RoundRectBackgroundSpan(int color, int radius, int textColor, int textSize, int horizontalSpace) {
		this.backgroundColor = color;
		this.radius = radius;
		this.textColor = textColor;
		this.textSize = textSize;
		this.horizontalSpace = horizontalSpace;

	}

	@Override
	public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
		textWidth = (int) (paint.measureText(text, start, end));
		return textWidth+horizontalSpace*2;
	}

	@Override
	public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom,
			@NonNull Paint paint) {
		paint.setColor(this.backgroundColor);
		paint.setAntiAlias(true);
		x = x+horizontalSpace;
		float centerY = getCenterY(paint);
		RectF oval = new RectF(x, y+paint.ascent(), x+ textWidth, y+paint.descent());
		canvas.drawRoundRect(oval,radius,radius,paint);
		paint.setColor(textColor);
		paint.setTextSize(textSize);
		float tx =  paint.measureText(text, start, end);
		float cY = getCenterY(paint);
		canvas.drawText(text,start,end,x+(textWidth-tx)/2,y-(centerY-cY),paint);
	}

	private float getCenterY(Paint paint){
		return (paint.descent()-paint.ascent())/2-paint.descent();
	}
}