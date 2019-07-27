package com.protonnetwork.proton;

import android.content.Context;
import android.content.Intent;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import androidLib.AndroidLib;

public class ProtonAccount {
    private static ProtonAccount _bean = null;
    public  static final String ADDRESS_KEY = "Address";
    public  static final String CIPHER_KEY = "CipherTxt";

    public  static synchronized ProtonAccount Instance() {
        if (_bean == null){
            _bean = new ProtonAccount();
        }

        return  _bean;
    }

    private static final String KEY_FOR_ACC_ADDR = "KEY_FOR_PROTON_ACC_ADDR";
    private static final String KEY_FOR_ACC_CIPHER = "KEY_FOR_PROTON_ACC_CIPHER";

    public String ProtonAddress, CipherTxt, TmpBoundEthAddress;

    private ProtonAccount(){
        this.ProtonAddress = utils.getString(KEY_FOR_ACC_ADDR, "");
        this.CipherTxt = utils.getString(KEY_FOR_ACC_CIPHER, "");
    }

    public Boolean CreateNewAccount(Context ctx, String pwd) {
        String addrCipher = AndroidLib.createAccount(pwd);
        if (addrCipher == ""){
            return false;
        }

        String[] result = addrCipher.split(AndroidLib.Separator);
        if (result.length != 2){
            return false;
        }

        syncNewAccount(result[0], result[1]);

        Intent i = new Intent(AccountStatusChangedReceiver.ProtonAccountChanged);
        i.putExtra(AccountStatusChangedReceiver.ActionKey, AccountStatusChangedReceiver.ProtonAccountChangedAction);
        ctx.sendBroadcast(i);
        return  true;
    }

    public String AccountToJson(){
        if (this.ProtonAddress == ""){
            return "";
        }

        JsonObject obj = new JsonObject();
        obj.addProperty(ADDRESS_KEY, this.ProtonAddress);
        obj.addProperty(CIPHER_KEY, this.CipherTxt);

        return obj.toString();
    }

    public void ParseAccount(final Context ctx, String jsonStr) throws Exception{
        JsonObject obj = new Gson().fromJson(jsonStr, JsonObject.class);
        if (!obj.isJsonObject()){
            throw new Exception("无效的json字符串");
        }

        final String address = obj.get(ADDRESS_KEY).getAsString();
        final String cipherTxt = obj.get(CIPHER_KEY).getAsString();
        if (!AndroidLib.isProtonAddress(address)){
            throw new Exception("这不是一个有效的proton地址");
        }
        utils.showPassWord(ctx, new AlertDialogOkCallBack() {
            @Override
            public void OkClicked(String password) {
                if (!AndroidLib.verifyAccount(address, cipherTxt, password)){
                    utils.ToastTips("解锁账号失败");
                    return;
                }
                utils.ToastTips("导入成功:"+address);

                syncNewAccount(address, cipherTxt);
                Intent i = new Intent(AccountStatusChangedReceiver.ProtonAccountChanged);
                i.putExtra(AccountStatusChangedReceiver.ActionKey, AccountStatusChangedReceiver.ProtonAccountChangedAction);
                ctx.sendBroadcast(i);
            }
        });
    }

    private void syncNewAccount(String address, String cipherTxt){
        this.ProtonAddress = address;
        this.CipherTxt = cipherTxt;

        utils.saveData(KEY_FOR_ACC_ADDR, this.ProtonAddress);
        utils.saveData(KEY_FOR_ACC_CIPHER, this.CipherTxt);
    }

    public boolean unlockAccount(String password){
            return AndroidLib.verifyAccount(this.ProtonAddress, this.CipherTxt, password);
    }

    public void ReloadBoundEth() {
        if (this.ProtonAddress.equals("")){
            return;
        }
        this.TmpBoundEthAddress = AndroidLib.loadEthAddrByProtonAddr(this.ProtonAddress);
    }
}
