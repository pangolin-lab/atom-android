package com.protonnetwork.proton;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;

import androidLib.AndroidLib;

public class EthereumAccount {
    protected String EthAddress;
    protected String CipherTxt;
    protected String ethBalance = "0.00";
    protected String protonBalance = "0.00";
    protected String boundNo = "0";
    private static EthereumAccount _bean = null;
    public  static final String ADDRESS_KEY = "address";
    private static final String KEY_FOR_ACC_ADDR = "KEY_FOR_ETH_ACC_ADDR";
    private static final String KEY_FOR_ACC_CIPHER = "KEY_FOR_ETH_ACC_CIPHER";

    public  static synchronized EthereumAccount Instance() {
        if (_bean == null){
            _bean = new EthereumAccount();
        }

        return  _bean;
    }

    private EthereumAccount(){
        this.EthAddress = utils.getString(KEY_FOR_ACC_ADDR, "");
        this.CipherTxt = utils.getString(KEY_FOR_ACC_CIPHER, "");
    }

    public void ReloadBalance() {

        if (this.EthAddress.equals("")) {
            return;
        }

        String balances = AndroidLib.ethBindings(this.EthAddress);
        String[] res = balances.split(AndroidLib.Separator);
        if (res.length != 3) {
            return;
        }

        this.ethBalance = res[0];
        this.protonBalance = res[1];
        this.boundNo = res[2];
    }

    public Boolean CreateNewAccount(Context context, String pwd) {
            File file = context.getCacheDir();
        if (file == null){
            return false;
        }

        String path = file.getAbsolutePath();
        String cipherTxt = AndroidLib.createEthAccount(pwd, path);
        if (cipherTxt.equals("")){
            return false;
        }

        JsonObject obj = new Gson().fromJson(cipherTxt, JsonObject.class);
        if (!obj.isJsonObject()){
            return false;
        }

        final String address = "0x" + obj.get(ADDRESS_KEY).getAsString();
        syncNewAccount(address, cipherTxt);

        Intent i = new Intent(AccountStatusChangedReceiver.ProtonAccountChanged);
        i.putExtra(AccountStatusChangedReceiver.ActionKey, AccountStatusChangedReceiver.EthAccountChangedAction);
        context.sendBroadcast(i);
        return true;
    }

    private void syncNewAccount(String address, String cipherTxt){
        this.EthAddress = address;
        this.CipherTxt = cipherTxt;

        utils.saveData(KEY_FOR_ACC_ADDR, this.EthAddress);
        utils.saveData(KEY_FOR_ACC_CIPHER, this.CipherTxt);
    }

    public void ParseAccount(final Context ctx, final String decodedText) throws Exception{
        JsonObject obj = new Gson().fromJson(decodedText, JsonObject.class);
        if (!obj.isJsonObject()){
            throw new Exception("无效的json字符串");
        }

        final String address = obj.get(ADDRESS_KEY).getAsString();

        utils.showPassWord(ctx, new AlertDialogOkCallBack() {
            @Override
            public void OkClicked(String password) {

                if (!AndroidLib.verifyEthAccount(decodedText, password)){
                    utils.ToastTips("解锁账号失败");
                    return;
                }

                syncNewAccount(address, decodedText);

                utils.ToastTips("导入成功");
                Intent i = new Intent(AccountStatusChangedReceiver.ProtonAccountChanged);
                i.putExtra(AccountStatusChangedReceiver.ActionKey, AccountStatusChangedReceiver.EthAccountChangedAction);
                ctx.sendBroadcast(i);
            }
        });
    }
}
