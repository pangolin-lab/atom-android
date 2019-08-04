package com.protonnetwork.proton.dialog;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import com.protonnetwork.proton.R;


public class LoadingProgressBar extends ProgressBar {

	public LoadingProgressBar(Context context) {
		super(context);
	}

	public LoadingProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.LoadingProgressBar);

		int color = ta.getColor(R.styleable.LoadingProgressBar_color, Color.WHITE);

		ta.recycle();
		setProgressBarColor(color);
	}

	public LoadingProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setProgressBarColor(int color) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			Drawable wrapDrawable = DrawableCompat.wrap(getIndeterminateDrawable());
			DrawableCompat.setTint(wrapDrawable, color);
			setIndeterminateDrawable(DrawableCompat.unwrap(wrapDrawable));
		} else {
			getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
		}
		invalidate();
	}

}
