package com.protonnetwork.proton;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.protonnetwork.proton.service.ProtonService;
import java.util.List;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity implements View.OnClickListener, EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks, ProtonService.onStatusChangedListener{
    private ImageButton serviceBtn;
    private ProgressBar waitingBar;

    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == AccountStatusChangedReceiver.ProtonAccountChangedAction) {
                reloadBoundEth();
            }else if (msg.what == AccountStatusChangedReceiver.VpnStatusChangedAction){
                if ( msg.obj != null){
                    String tips = (String)msg.obj;
                    utils.ToastTips(tips);
                }
            }
        }
    };

    EditText curProtonAddrTxt, boundEthAddrTxt;
    AccountStatusChangedReceiver statusReceiver;
    IntentFilter intentFilter;
    private String ProtonAccountPassword = "";

    TextView statusTips;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceBtn = findViewById(R.id.serviceSwitch);
        serviceBtn.setOnClickListener(this);

        waitingBar = findViewById(R.id.waitingTips);
        waitingBar.setVisibility(View.GONE);

        curProtonAddrTxt = findViewById(R.id.protonAddressCurrent);
        boundEthAddrTxt = findViewById(R.id.protonBindEthAddr);

        statusTips = findViewById(R.id.ProtonNetworkStatus);

        statusReceiver = new AccountStatusChangedReceiver(mHandler);
        intentFilter = new IntentFilter(AccountStatusChangedReceiver.ProtonAccountChanged);
        intentFilter.addAction(AccountStatusChangedReceiver.VPNStatusChanged);

        ProtonService.addOnStatusChangedListener(this);
    }

    @Override
    protected void onDestroy(){
        ProtonService.removeOnStatusChangedListener(this);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadBoundEth();
        registerReceiver(statusReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(statusReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
           utils.ToastTips("returned from setting!");
        }else if (utils.RC_SELECT_FROM_GALLARY == requestCode){
            if (resultCode != RESULT_OK || null == data){
                utils.ToastTips("未读取数据");
                return;
            }

            loadAccountFromGalleryQRCode(data.getData());

        } else if (utils.RC_VPN_RIGHT == requestCode){
            if (resultCode != RESULT_OK){
                utils.ToastTips("取消Vpn授权");
                return;
            }
            showWaitingRing();
            final Intent intent = new Intent(this, ProtonService.class);
            intent.putExtra(ProtonService.DATA_PASSWORD, this.ProtonAccountPassword);
            startService(intent);

        }else{
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if(result == null) {
                return;
            }
            if(result.getContents() == null) {
                utils.ToastTips("无效扫码");
                return;
            }
            try {
                String decodedText = result.getContents();
                ProtonAccount.Instance().ParseAccount(this, decodedText);
            }catch (Exception ex){
                utils.ToastTips("导入账号失败:" + ex.getLocalizedMessage());
            }
        }
    }

    void loadAccountFromGalleryQRCode(Uri uri) {

        if (null == uri) {
            utils.ToastTips("没有导入正确图片");
            return;
        }
        try {
            String decodedText = utils.ParseQRCodeFile(uri, getContentResolver());
            ProtonAccount.Instance().ParseAccount(this, decodedText);
        } catch (Exception e) {
            utils.ToastTips("导入账号二维码失败:"+e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    void reloadBoundEth(){
        if (ProtonService.IsRunning){
            utils.ToastTips("请关闭VPN之后查询区块链信息");
            return;
        }
        final String protonAddr = ProtonAccount.Instance().ProtonAddress;
        curProtonAddrTxt.setText(protonAddr);
        if (protonAddr.equals("")){
            return;
        }
        showWaitingRing();
        new Thread(new Runnable() {
                @Override
                public void run() {
                    ProtonAccount.Instance().ReloadBoundEth();
                    hideWaitingRing();
                    refreshUI();
                }
            }).start();
    }

    void refreshUI(){
        final String protonAddr = ProtonAccount.Instance().ProtonAddress;
        final String ethAddr = ProtonAccount.Instance().TmpBoundEthAddress;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                curProtonAddrTxt.setText(protonAddr);
                boundEthAddrTxt.setText(ethAddr);
                if (ProtonService.IsRunning) {
                    serviceBtn.setImageResource(R.drawable.running);
                    serviceBtn.setTag(17);
                    statusTips.setText("使用中");
                } else {
                    serviceBtn.setImageResource(R.drawable.down);
                    serviceBtn.setTag(7);
                    statusTips.setText("未接入");
                }
            }
        });
    }

      @Override
    public void onClick(View v) {
        switch (v.getTag().toString()) {
            case "1":
                createNewAccount();
                break;

            case "2":
                ImportAccount();
                break;

            case "3":
                QrCodeGenerateTask();
                break;

            case "4":
                String protonAddr = curProtonAddrTxt.getText().toString();
                if (protonAddr.equals("")){
                    return;
                }
                utils.CopyToMemory(protonAddr);
                break;

            case "5":
                reloadBoundEth();
                break;

            case "7":
                startVpnService();
                break;

            case "8":
                String ethAddr = boundEthAddrTxt.getText().toString();
                if (ethAddr.equals("")){
                    return;
                }
                utils.CopyToMemory(ethAddr);
                break;

            case "17":
                stopVpnService();
                break;

            case "6":
                startEthereumOpt();
                break;
        }
    }


    void ImportAccount(){
        if (!EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.rationale_extra_write),
                    utils.RC_IMAGE_GALLARY_PERM,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.camera),
                    utils.RC_CAMERA_PERM,
                    Manifest.permission.CAMERA);
        }

        AlertDialogOkCallBack callBack = new AlertDialogOkCallBack(){

            @Override
            public void OkClicked(String parameter) {
                showImportQRChoice();
            }
        };

        String accAddr = ProtonAccount.Instance().ProtonAddress;
        if (!accAddr.equals("")){
            utils.ShowOkOrCancelAlert(this,"确定要替换吗？",
                    "导入账号会替换当前proton账号，请确保您已经保存好当前账号",callBack);
            return;
        }
        showImportQRChoice();
    }
    void showImportQRChoice(){
        final String[] listItems = {"扫描二维码读取","从相册读取", "取消"};
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle("请选择导入方式");

        mBuilder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if (0 == i){
                    IntentIntegrator ii = new IntentIntegrator(MainActivity.this);
                    ii.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                    ii.setCaptureActivity(ScanActivity.class);
                    ii.setPrompt("请扫描Proton账号二维码"); //底部的提示文字，设为""可以置空
                    ii.setCameraId(0); //前置或者后置摄像头
                    ii.setBarcodeImageEnabled(true);
                    ii.initiateScan();

                }else if (1 == i){
                    Intent pi = new Intent(Intent.ACTION_GET_CONTENT,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pi , utils.RC_SELECT_FROM_GALLARY);
                }
                dialogInterface.dismiss();
            }
        });

        AlertDialog mDialog = mBuilder.create();
        mDialog.show();
    }

    @AfterPermissionGranted(utils.RC_IMAGE_GALLARY_PERM)
    void QrCodeGenerateTask(){
        if (!EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.rationale_extra_write),
                    utils.RC_IMAGE_GALLARY_PERM,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }
            final String accountData = ProtonAccount.Instance().AccountToJson();
        if (accountData.equals("")) {
            utils.ToastTips("空账号");
            return;
        }

        showWaitingRing();
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                utils.SaveStringQRCode(getContentResolver(), accountData, "Proton账号");
                hideWaitingRing();
            }
        });
        th.start();
    }

    void createNewAccount(){

        final AlertDialogOkCallBack callBack = new AlertDialogOkCallBack(){
            @Override
            public void OkClicked(String parameter) {
                Boolean ret = ProtonAccount.Instance().CreateNewAccount(MainActivity.this, parameter);
                if (!ret){
                    utils.ToastTips("创建账号失败");
                    return;
                }
                utils.ToastTips("创建账号成功");
            }
        };

        String accAddr = ProtonAccount.Instance().ProtonAddress;
        if (!accAddr.equals("")){
            utils.ShowOkOrCancelAlert(this,"确定要替换吗？",
                    "创建账号会替换当前Proton账号，请确保您已经保存好当前账号",
                    new AlertDialogOkCallBack(){
                        @Override
                        public void OkClicked(String parameter) {
                            utils.showDoublePassWord(MainActivity.this, callBack);
                        }
                    });
            return;
        }

        utils.showDoublePassWord(this, callBack);
    }

    void showWaitingRing(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                waitingBar.setVisibility(View.VISIBLE);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            }
        });
    }

    void hideWaitingRing(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                waitingBar.setVisibility(View.GONE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        });
    }

    void startVpnService(){
        String protonAddress = ProtonAccount.Instance().ProtonAddress;
        if (protonAddress.equals("")){
            utils.ToastTips("请首先创建proton网络账号");
            return;
        }

        String ethAddr = boundEthAddrTxt.getText().toString();
        if (!utils.validEthAddress(ethAddr)){
            utils.ToastTips("该Proton账号尚未被以太坊地址绑定");
            return;
        }


        AlertDialogOkCallBack callBack = new AlertDialogOkCallBack(){
            @Override
            public void OkClicked(String password) {
                if (!ProtonAccount.Instance().unlockAccount(password)){
                    utils.ToastTips("解锁Proton账号失败");
                    return;
                }
                ProtonAccountPassword = password;

                Intent intent = VpnService.prepare(MainActivity.this);
                if (intent != null) {
                    startActivityForResult(intent, utils.RC_VPN_RIGHT);
                } else {
                    onActivityResult(utils.RC_VPN_RIGHT, RESULT_OK, null);
                }
            }
        };

        utils.showPassWord(this, callBack);
    }

    void stopVpnService(){
        showWaitingRing();
        ProtonService.Stop();
    }

    void startEthereumOpt(){
        Intent intent = new Intent(this, EthereumOperation.class);
        this.startActivity(intent);
    }

    private static String TAG = "MAIN ACTIVITY";
    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onRationaleAccepted(int requestCode) {
        Log.d(TAG, "onRationaleAccepted:" + requestCode);
    }

    @Override
    public void onRationaleDenied(int requestCode) {
        Log.d(TAG, "onRationaleDenied:" + requestCode);
    }

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        utils.ToastTips(status);
        hideWaitingRing();
        refreshUI();
    }
}
