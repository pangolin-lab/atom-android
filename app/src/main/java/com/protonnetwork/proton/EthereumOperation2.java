package com.protonnetwork.proton;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import androidLib.AndroidLib;
import pub.devrel.easypermissions.AppSettingsDialog;

public class EthereumOperation2 extends Activity{

    AccountStatusChangedReceiver statusReceiver;
    IntentFilter intentFilter;
    private ProgressBar waitingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ethereum_operation2);
//        TextView tipsContentTv = findViewById(R.id.tipsContentTv);
//        SpannableStringBuilder info = SpannableStringUtils.getBuilder(getResources().getString(R.string.ethereumMoreTips1))
//                .append(getResources().getString(R.string.ethereumMoreTips2)).setBackgroundColor(
//                        getResources().getColor(R.color.color_ebe3f6)).append(getResources().getString(R.string.ethereumMoreTips3)).append(getResources().getString(R.string.ethereumMoreTips4)).setBackgroundColor(
//                        getResources().getColor(R.color.color_e6ecfd)).append(getResources().getString(R.string.ethereumMoreTips5)).create();
//
//        tipsContentTv.setText(info);
    }


}
