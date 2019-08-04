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
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.protonnetwork.proton.base.BaseActivity;
import com.protonnetwork.proton.dialog.ProtonProgressDialog;

import androidLib.AndroidLib;
import pub.devrel.easypermissions.AppSettingsDialog;

public class EthereumOperation extends BaseActivity implements View.OnClickListener {
    private ProtonProgressDialog mProtonProgressDialog;
    EditText curEthAccountTxt, ethBalanceTxt, operatedProtonAccountTxt, boundEthAddrTxt, boundNoTxt;
    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == AccountStatusChangedReceiver.EthAccountChangedAction) {
                reloadEthInfo();
            }
        }
    };

    AccountStatusChangedReceiver statusReceiver;
    IntentFilter intentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ethereum_operation);

        curEthAccountTxt = findViewById(R.id.addressOfEthereumAccount);
        ethBalanceTxt = findViewById(R.id.ethereumBalance);
        operatedProtonAccountTxt = findViewById(R.id.ProtonAddrInEthAccount);
        boundEthAddrTxt = findViewById(R.id.addressOfEthereumAccountForSeach);
        boundNoTxt = findViewById(R.id.boundNum);


        statusReceiver = new AccountStatusChangedReceiver(mHandler);
        intentFilter = new IntentFilter(AccountStatusChangedReceiver.ProtonAccountChanged);
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
        } else if (utils.RC_SELECT_FROM_GALLARY == requestCode) {
            if (resultCode != RESULT_OK || null == data) {
                utils.ToastTips("未读取数据");
                return;
            }

            loadEthAccountFromGalleryQRCode(data.getData());

        } else {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result == null) {
                return;
            }
            if (result.getContents() == null) {
                utils.ToastTips("无效扫码");
                return;
            }
            try {
                String decodedText = result.getContents();
                EthereumAccount.Instance().ParseAccount(this, decodedText);
            } catch (Exception ex) {
                utils.ToastTips("导入账号失败:" + ex.getLocalizedMessage());
            }
        }
    }

    void loadEthAccountFromGalleryQRCode(Uri uri) {
        if (null == uri) {
            utils.ToastTips("没有导入正确图片");
            return;
        }
        try {
            showWaitingRing();
            String decodedText = utils.ParseQRCodeFile(uri, getContentResolver());
            EthereumAccount.Instance().ParseAccount(this, decodedText);
        } catch (Exception e) {
            utils.ToastTips("导入账号二维码失败:" + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {

        switch (v.getTag().toString()) {
            case "1":
                String ethAcc = curEthAccountTxt.getText().toString();
                if (ethAcc.equals("")) {
                    return;
                }
                utils.CopyToMemory(ethAcc);
                break;

            case "9":
                ethAcc = boundEthAddrTxt.getText().toString();
                if (ethAcc.equals("")) {
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
                bindProtonAddress();
                break;
            case "7":
                unbindProtonAddress();
                break;
            case "8":
                searchBindings();
                break;
        }
    }

    boolean checkProtonAndEthAddress() {
        String protonAddress = operatedProtonAccountTxt.getText().toString();
        if (protonAddress.equals("")) {
            utils.ToastTips("Proton地址不能为空");
            return false;
        }

        String ethAddress = curEthAccountTxt.getText().toString();
        if (ethAddress.equals("")) {
            utils.ToastTips("请设置本操作需要用到的以太坊地址");
            return false;
        }

        return true;
    }

    void searchBindings() {

        final String protonAddress = operatedProtonAccountTxt.getText().toString();
        if (protonAddress.equals("")) {
            utils.ToastTips("Proton地址不能为空");
            return;
        }

        showWaitingRing();
        Thread th = new Thread() {
            @Override
            public void run() {
                final String ethAddr = AndroidLib.loadEthAddrByProtonAddr(protonAddress);
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

    void unbindProtonAddress() {

        if (!checkProtonAndEthAddress()) {
            return;
        }

        final String protonAddress = operatedProtonAccountTxt.getText().toString();
        final String CipherTxt = EthereumAccount.Instance().CipherTxt;

        utils.showPassWord(EthereumOperation.this, new AlertDialogOkCallBack() {
            @Override
            public void OkClicked(String password) {
                String ret = AndroidLib.unbindProtonAddress(protonAddress, CipherTxt, password);
                if (!ret.startsWith("0x")) {
                    utils.ToastTips("解除绑定失败:" + ret);
                    hideWaitingRing();
                    return;
                }

                utils.ToastTips("解除绑定成功，正在打包:" + ret);
                updateAndShow(utils.EthScanBaseUrl + ret);
            }
        });
    }

    void bindProtonAddress() {
        if (!checkProtonAndEthAddress()) {
            return;
        }

        final String protonAddress = operatedProtonAccountTxt.getText().toString();
        showWaitingRing();

        final String ethAddr = AndroidLib.loadEthAddrByProtonAddr(protonAddress);
        if (utils.validEthAddress(ethAddr)) {
            utils.ToastTips("该地址已经被[" + ethAddr + "]绑定");
            hideWaitingRing();
            return;
        }

        final String CipherTxt = EthereumAccount.Instance().CipherTxt;
        utils.showPassWord(EthereumOperation.this, new AlertDialogOkCallBack() {
            @Override
            public void OkClicked(String password) {
                String ret = AndroidLib.bindProtonAddress(protonAddress, CipherTxt, password);
                if (!ret.startsWith("0x")) {
                    utils.ToastTips("绑定失败:" + ret);
                    hideWaitingRing();
                    return;
                }

                utils.ToastTips("绑定成功，正在打包:" + ret);
                updateAndShow(utils.EthScanBaseUrl + ret);
            }
        });
    }

    void updateAndShow(String url) {

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);

        Intent i = new Intent(AccountStatusChangedReceiver.ProtonAccountChanged);
        i.putExtra(AccountStatusChangedReceiver.ActionKey, AccountStatusChangedReceiver.EthAccountChangedAction);
        sendBroadcast(i);
    }

    void importEthAddress() {
        AlertDialogOkCallBack callBack = new AlertDialogOkCallBack() {
            @Override
            public void OkClicked(String parameter) {
                showImportQRChoice();
            }
        };

        String ethAcc = curEthAccountTxt.getText().toString();
        if (!ethAcc.equals("")) {
            utils.ShowOkOrCancelAlert(this, "确定要替换吗？",
                    "导入账号会替换当前以太坊账号，请确保您已经保存好当前账号", callBack);
            return;
        }
        showImportQRChoice();
    }

    void showImportQRChoice() {
        final String[] listItems = {"扫描二维码读取", "从相册读取", "取消"};
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle("请选择导入方式");

        mBuilder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if (0 == i) {
                    if (!utils.checkCamera(EthereumOperation.this)) {
                        return;
                    }

                    IntentIntegrator ii = new IntentIntegrator(EthereumOperation.this);
                    ii.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                    ii.setCaptureActivity(ScanActivity.class);
                    ii.setPrompt("请扫描Proton账号二维码"); //底部的提示文字，设为""可以置空
                    ii.setCameraId(0); //前置或者后置摄像头
                    ii.setBarcodeImageEnabled(true);
                    ii.initiateScan();

                } else if (1 == i) {
                    if (!utils.checkStorage(EthereumOperation.this)) {
                        return;
                    }

                    Intent pi = new Intent(Intent.ACTION_GET_CONTENT,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pi, utils.RC_SELECT_FROM_GALLARY);
                }
                dialogInterface.dismiss();
            }
        });

        AlertDialog mDialog = mBuilder.create();
        mDialog.show();
    }

    void exportEthAddress() {
        if (!utils.checkStorage(this)) {
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

    void createNewEthAccount() {
        final AlertDialogOkCallBack callBack = new AlertDialogOkCallBack() {
            @Override
            public void OkClicked(final String pwd) {
                showWaitingRing();

                Thread th = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Boolean ret = EthereumAccount.Instance().CreateNewAccount(EthereumOperation.this, pwd);
                        if (!ret) {
                            utils.ToastTips("创建以太坊账号失败");
                        }
                        utils.ToastTips("创建以太坊账号成功");
                    }
                });
                th.start();
            }
        };

        String ethAcc = curEthAccountTxt.getText().toString();
        if (!ethAcc.equals("")) {
            utils.ShowOkOrCancelAlert(this, "确定要替换吗？",
                    "创建账号会替换当前以太坊账号，请确保您已经保存好当前账号",
                    new AlertDialogOkCallBack() {
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
                boundNoTxt.setText(EthereumAccount.Instance().boundNo);

            }
        });
    }

    void reloadEthInfo() {
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

    void showWaitingRing() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProtonProgressDialog == null) {
                    mProtonProgressDialog = new ProtonProgressDialog();
                }
                showDialogFragment(mProtonProgressDialog, "loading");
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        });
    }

    void hideWaitingRing() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dismissDialogFragment("loading");
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            }
        });
    }
}
