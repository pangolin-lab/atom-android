package com.protonnetwork.proton.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.protonnetwork.proton.R;


/**
 */
public abstract class BaseDialogFragment extends DialogFragment {

	public void setDismissListener(DialogFragmentDismissListener dismissListener) {
		this.dismissListener = dismissListener;
	}

	public DialogFragmentDismissListener dismissListener;

	protected Activity mActivity;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof Activity) {
			mActivity = (Activity) context;
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (null == savedInstanceState) {
			if (0 == getSelfTheme()) {
				setStyle(DialogFragment.STYLE_NO_FRAME, R.style.Theme_BaseDialog);
			} else {
				setStyle(DialogFragment.STYLE_NO_FRAME, getSelfTheme());
			}
		}
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		reThemeWindow();
		return dialog;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		if (null != dismissListener) {
			dismissListener.onDismiss(getTag());
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		reSizeWindow();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mActivity = null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//		MengdongApp.getInstance().getRefWatcher().watch(this);
	}

	protected abstract int getSelfTheme();

	/**
	 * 重新设置windowSize,因为window WindowManager.LayoutParams默认是match_parent
	 */
	protected abstract void reThemeWindow();

	protected abstract void reSizeWindow();

	public boolean isFragmentAdded(){
		return null!=getActivity()&&!getActivity().isFinishing()&&isAdded();
	}




	/**
	 * 显示Dialog
	 *
	 * @param activity
	 * @param tag      设置一个标签用来标记Dialog
	 */
	public void show(AppCompatActivity activity, String tag) {
		show(activity, null, tag);
	}

	/**
	 * 显示Dialog
	 *
	 * @param activity
	 * @param bundle   要传递给Dialog的Bundle对象
	 * @param tag      设置一个标签用来标记Dialog
	 */
	public void show(AppCompatActivity activity, Bundle bundle, @NonNull String tag) {
		if (activity == null || isShowing() || TextUtils.isEmpty(tag))
			return;
		FragmentManager fragmentManager = activity.getSupportFragmentManager();

		FragmentTransaction mTransaction = fragmentManager.beginTransaction();

		Fragment mFragment = fragmentManager.findFragmentByTag(tag);
		if (mFragment != null) {
//			//为了不重复显示dialog，在显示对话框之前移除正在显示的对话框
//			mTransaction.remove(mFragment);
			if (mFragment instanceof DialogFragment && mFragment.isAdded()) {
				((DialogFragment) mFragment).dismissAllowingStateLoss();
			}
		}
		if (bundle != null) {
			setArguments(bundle);
		}
		mTransaction.add(this, tag);
		mTransaction.commitAllowingStateLoss();
	}


	/**
	 * 是否显示
	 *
	 * @return false:isHidden  true:isShowing
	 */
	protected boolean isShowing() {
		if (this.getDialog() != null) {
			return this.getDialog().isShowing();
		} else {
			return false;
		}
	}

}
