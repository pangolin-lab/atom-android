package com.protonnetwork.proton.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.common.StringUtils;
import com.protonnetwork.proton.R;
import com.protonnetwork.proton.base.BaseDialogFragment;

public class ProtonProgressDialog extends BaseDialogFragment {

	/**
	 * Sets whether this dialog is cancelable with the
	 * {@link KeyEvent#KEYCODE_BACK BACK} key.
	 */
	private boolean cancelable = true;

	private boolean canceledOnTouchOutside = true;

	private String contentText;

	private float dimAmount = 0.8f;

	private int mappingRes = -1;

	public ProtonProgressDialog() {
		super();
	}

	@Override
	public void show(FragmentManager manager, String tag) {
		super.show(manager, tag);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.dialog_proton_progress, container, false);

		TextView contentView = rootView.findViewById(R.id.content_tv);
		if (TextUtils.isEmpty(contentText)) {
			contentView.setVisibility(View.GONE);
		} else {
			contentView.setText(contentText);
		}
		LoadingProgressBar progressView = rootView.findViewById(R.id.mapping_progress_iv);
		ImageView mappingIv = rootView.findViewById(R.id.mapping_icon_iv);
	/*	AnimationDrawable animationDrawable = (AnimationDrawable) progressView.getDrawable();
		if (null != animationDrawable) {
			animationDrawable.start();
		}*/
		if(getDialog().getWindow()!=null){
			getDialog().getWindow().setWindowAnimations(R.style.AnimFade);
			getDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		}

		setCancelable(cancelable);

		if(mappingRes>0){
			progressView.setVisibility(View.GONE);
			mappingIv.setVisibility(View.VISIBLE);
			mappingIv.setImageResource(mappingRes);
		}

		return rootView;
	}

	public ProtonProgressDialog setMyCanceledOnTouchOutside(boolean canceledOnTouchOutside) {
		this.canceledOnTouchOutside = canceledOnTouchOutside;
		return this;
	}

	public ProtonProgressDialog setMyCancelable(boolean cancelable) {
		this.cancelable = cancelable;
		return this;
	}

	public ProtonProgressDialog setContentText(String contentText) {
		this.contentText = contentText;
		return this;
	}

	public ProtonProgressDialog setMyDimAmount(float dimAmount) {
		if (dimAmount >= 0 && dimAmount <= 1) {
			this.dimAmount = dimAmount;
		}
		return this;
	}

	public ProtonProgressDialog setMappingIcon(int res){
		this.mappingRes = res;
		return this;
	}

	@Override
	protected int getSelfTheme() {
		return 0;
	}

	@Override
	protected void reThemeWindow() {
		Dialog dialog = getDialog();
		if (null != dialog) {
			Window window = dialog.getWindow();
			if (null != window) {
				if (dimAmount != 0.8f) {
					dialog.getWindow().setDimAmount(dimAmount);
				}
			}
			dialog.setCanceledOnTouchOutside(canceledOnTouchOutside);
		}
	}

	@Override
	protected void reSizeWindow() {

	}
}
