package com.protonnetwork.proton;

import android.Manifest;
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
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import androidLib.AndroidLib;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class EthereumOperation extends Activity implements View.OnClickListener{

    EditText curEthAccountTxt, ethBalanceTxt,sofaBalanceTxt,operatedSofaAccountTxt,boundEthAddrTxt, boundNoTxt;
    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == AccountStatusChangedReceiver.EthAccountChangedAction) {
                reloadEthInfo();
            }
        }
    };

    AccountStatusChangedReceiver statusReceiver;
    IntentFilter intentFilter;
    private ProgressBar waitingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ethereum_operation);

        curEthAccountTxt = findViewById(R.id.addressOfEthereumAccount);
        ethBalanceTxt = findViewById(R.id.ethereumBalance);
        sofaBalanceTxt = findViewById(R.id.sofaBalance);
        operatedSofaAccountTxt = findViewById(R.id.SofaAddrInEthAccount);
        boundEthAddrTxt = findViewById(R.id.addressOfEthereumAccountForSeach);
        boundNoTxt = findViewById(R.id.boundNum);

        waitingBar = findViewById(R.id.waitingTips);
        waitingBar.setVisibility(View.GONE);

        statusReceiver = new AccountStatusChangedReceiver(mHandler);
        intentFilter = new IntentFilter(AccountStatusChangedReceiver.SofaAccountChanged);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadEthInfo();
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

            loadEthAccountFromGalleryQRCode(data.getData());

        } else{
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
                EthereumAccount.Instance().ParseAccount(this, decodedText);
            }catch (Exception ex){
                utils.ToastTips("导入账号失败:" + ex.getLocalizedMessage());
            }
        }
    }

    void loadEthAccountFromGalleryQRCode(Uri uri){
        if (null == uri) {
            utils.ToastTips("没有导入正确图片");
            return;
        }
        try {
            showWaitingRing();
            String decodedText = utils.ParseQRCodeFile(uri, getContentResolver());
            EthereumAccount.Instance().ParseAccount(this, decodedText);
        } catch (Exception e) {
            utils.ToastTips("导入账号二维码失败:"+e.getLocalizedMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {

        switch (v.getTag().toString()) {
            case "1":
                String ethAcc = curEthAccountTxt.getText().toString();
                if (ethAcc.equals("")){
                    return;
                }
                utils.CopyToMemory(ethAcc);
                break;

            case "9":
                ethAcc = boundEthAddrTxt.getText().toString();
                if (ethAcc.equals("")){
                    return;
                }
                utils.CopyToMemory(ethAcc);
                break;

            case "2":
                reloadEthInfo();
                break;

            case "3":
                createNewEthAccount();
                break;

            case "4":
                importEthAddress();
                break;

            case "5":
                exportEthAddress();
                break;

            case "6":
                bindSofaAddress();
                break;
            case "7":
                unbindSofaAddress();
                break;
            case "8":
                searchBindings();
                break;
        }
    }

    boolean checkSofaAndEthAddress(){
        String sofaAddress = operatedSofaAccountTxt.getText().toString();
        if (sofaAddress.equals("")){
            utils.ToastTips("sofa地址不能为空");
            return false;
        }

        String ethAddress = curEthAccountTxt.getText().toString();
        if (ethAddress.equals("")){
            utils.ToastTips("请设置本操作需要用到的以太坊地址");
            return false;
        }
        return true;
    }

    void searchBindings(){

        final String sofaAddress = operatedSofaAccountTxt.getText().toString();
        if (sofaAddress.equals("")){
            utils.ToastTips("sofa地址不能为空");
            return;
        }

        showWaitingRing();
        Thread th = new Thread(){
            @Override
            public void  run(){
                final String ethAddr = AndroidLib.loadEthAddrBySofaAddr(sofaAddress);
                hideWaitingRing();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        boundEthAddrTxt.setText(ethAddr);
                    }
                });
            }
        };
        th.start();
    }

    void unbindSofaAddress(){

        if(!checkSofaAndEthAddress()){
            return;
        }

        final String sofaAddress = operatedSofaAccountTxt.getText().toString();
        final String CipherTxt = EthereumAccount.Instance().CipherTxt;

        utils.showPassWord(EthereumOperation.this, new AlertDialogOkCallBack() {
            @Override
            public void OkClicked(String password) {
                String ret = AndroidLib.unbindSofaAddress(sofaAddress, CipherTxt, password);
                if (!ret.startsWith("0x")){
                    utils.ToastTips("解除绑定失败:" + ret);
                    hideWaitingRing();
                    return;
                }

                utils.ToastTips("解除绑定成功，正在打包:" + ret);
                updateAndShow( utils.EthScanBaseUrl + ret);
            }
        });
    }

    void bindSofaAddress(){
        if(!checkSofaAndEthAddress()){
            return;
        }

        final String sofaAddress = operatedSofaAccountTxt.getText().toString();
        showWaitingRing();

        final String ethAddr = AndroidLib.loadEthAddrBySofaAddr(sofaAddress);
        if (utils.validEthAddress(ethAddr)){
            utils.ToastTips("该地址已经被["+ethAddr+"]绑定");
            hideWaitingRing();
            return;
        }

        final String CipherTxt = EthereumAccount.Instance().CipherTxt;
        utils.showPassWord(EthereumOperation.this, new AlertDialogOkCallBack() {
            @Override
            public void OkClicked(String password) {
                String ret = AndroidLib.bindSofaAddress(sofaAddress, CipherTxt, password);
                if (!ret.startsWith("0x")){
                    utils.ToastTips("绑定失败:" + ret);
                    hideWaitingRing();
                    return;
                }

                utils.ToastTips("绑定成功，正在打包:" + ret);
                updateAndShow( utils.EthScanBaseUrl + ret);
            }
        });
    }

    void updateAndShow(String url){

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);

        Intent i = new Intent(AccountStatusChangedReceiver.SofaAccountChanged);
        i.putExtra(AccountStatusChangedReceiver.ActionKey, AccountStatusChangedReceiver.EthAccountChangedAction);
        sendBroadcast(i);
    }

    void importEthAddress(){

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

        String ethAcc = curEthAccountTxt.getText().toString();
        if (!ethAcc.equals("")){
            utils.ShowOkOrCancelAlert(this,"确定要替换吗？",
                    "导入账号会替换当前以太坊账号，请确保您已经保存好当前账号",callBack);
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
                    IntentIntegrator ii = new IntentIntegrator(EthereumOperation.this);
                    ii.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                    ii.setCaptureActivity(ScanActivity.class);
                    ii.setPrompt("请扫描Sofa账号二维码"); //底部的提示文字，设为""可以置空
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

    void exportEthAddress(){
        if (!EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.rationale_extra_write),
                    utils.RC_IMAGE_GALLARY_PERM,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return;
        }

        final String accountData = EthereumAccount.Instance().CipherTxt;
        if (accountData.equals("")) {
            utils.ToastTips("空账号无需保存");
            return;
        }

        showWaitingRing();
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                utils.SaveStringQRCode(getContentResolver(), accountData, "以太坊账号");
                hideWaitingRing();
            }
        });
        th.start();
    }

    void createNewEthAccount(){
        final AlertDialogOkCallBack callBack = new AlertDialogOkCallBack(){
            @Override
            public void OkClicked(final String pwd) {
                showWaitingRing();

                Thread th = new Thread(new Runnable() {
                    @Override
                    public void run(){
                        Boolean ret = EthereumAccount.Instance().CreateNewAccount(EthereumOperation.this, pwd);
                        if (!ret){
                            utils.ToastTips("创建以太坊账号失败");
                        }
                        utils.ToastTips("创建以太坊账号成功");
                    }
                });
                th.start();
            }
        };

        String ethAcc = curEthAccountTxt.getText().toString();
        if (!ethAcc.equals("")){
            utils.ShowOkOrCancelAlert(this,"确定要替换吗？",
                    "创建账号会替换当前以太坊账号，请确保您已经保存好当前账号",
                    new AlertDialogOkCallBack(){
                        @Override
                        public void OkClicked(String parameter) {
                            utils.showDoublePassWord(EthereumOperation.this, callBack);
                        }
                    });
            return;
        }

        utils.showDoublePassWord(this, callBack);
    }

    void refreshUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String address = EthereumAccount.Instance().EthAddress;
                curEthAccountTxt.setText(address);
                ethBalanceTxt.setText(EthereumAccount.Instance().ethBalance);
                sofaBalanceTxt.setText(EthereumAccount.Instance().sofaBalance);
                boundNoTxt.setText(EthereumAccount.Instance().boundNo);

            }
        });
    }

    void reloadEthInfo(){
        showWaitingRing();
        new Thread(new Runnable() {
            @Override
            public void run() {
                EthereumAccount.Instance().ReloadBalance();
                hideWaitingRing();
                refreshUI();
            }
        }).start();
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
}
