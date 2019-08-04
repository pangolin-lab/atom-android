package com.protonnetwork.proton.base;

import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

public class BaseActivity extends AppCompatActivity {

    public void showDialogFragment(@NonNull DialogFragment dialogFragment, @NonNull String tag) {
        if (null == dialogFragment || TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("DialogFragment not null & tag not null");
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment instanceof DialogFragment && fragment.isAdded()) {
            ((DialogFragment) fragment).dismissAllowingStateLoss();
        }

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(dialogFragment, tag);
        fragmentTransaction.commitAllowingStateLoss();
    }


    /**
     * dismiss
     */
    public void dismissDialogFragment(@NonNull String tag) {
        if (TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment instanceof DialogFragment && fragment.isAdded()) {
            ((DialogFragment) fragment).dismissAllowingStateLoss();
        }
    }
}
