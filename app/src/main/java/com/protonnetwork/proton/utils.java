package com.protonnetwork.proton;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.protonnetwork.ProtonApplication;

import java.io.OutputStream;

import pub.devrel.easypermissions.EasyPermissions;

interface AlertDialogOkCallBack{
    void OkClicked(String parameter);
}

public final class utils {

    public static final int RC_IMAGE_GALLARY_PERM = 123;
    public static final int RC_CAMERA_PERM = 124;
    public static final int RC_SELECT_FROM_GALLARY = 125;
    public static final int RC_VPN_RIGHT = 126;

    public static final String EthScanBaseUrl = "https://ropsten.etherscan.io/tx/";

    static Context appContext = ProtonApplication.getAppContext();
    static SharedPreferences sharedPref =  appContext.getSharedPreferences("ProtonaManager", Context.MODE_PRIVATE);

    public static void saveData(String key, String value){
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getString(String key, String defaultVal){
        return sharedPref.getString(key, defaultVal);
    }

    public static void ShowOkOrCancelAlert(Context context, String tittle, String message, final AlertDialogOkCallBack callBack){

        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(tittle);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        callBack.OkClicked("");
                    }
                });

        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
    static Toast toast = null;
    public static void ToastTips(String msg){
        Context context = ProtonApplication.getAppContext();
        try {
            if(toast!=null){
                toast.setText(msg);
            }else{
                toast= Toast.makeText(context, msg, Toast.LENGTH_LONG);
            }
            toast.show();
        } catch (Exception e) {
            Looper.prepare();
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }

    public static void showDoublePassWord(Context context, final AlertDialogOkCallBack callBack){
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View promptView = layoutInflater.inflate(R.layout.create_account, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptView);

        final EditText passwordET1 = (EditText) promptView.findViewById(R.id.accPassword1);
        final EditText passwordET2 = (EditText) promptView.findViewById(R.id.accPassword2);
        final String[] passwords = {""};

        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String pwd1 = passwordET1.getText().toString();
                        String pwd2 = passwordET2.getText().toString();
                        if (!pwd1.equals(pwd2)){
                            utils.ToastTips("两次密码输入不一致");
                            return;
                        }
                        dialog.dismiss();
                        callBack.OkClicked(pwd1);
                    }
                }) .setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    public static void showPassWord(Context context, final AlertDialogOkCallBack callBack){
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View passwordView = layoutInflater.inflate(R.layout.password, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(passwordView);

        final EditText passwordET = (EditText) passwordView.findViewById(R.id.passwordToUnlock);
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        callBack.OkClicked(passwordET.getText().toString());
                    }
                }).setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        alertDialogBuilder.create().show();
    }

    public static void CopyToMemory(String src) {
        ClipboardManager clipboard = (ClipboardManager) appContext.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Proton memory string", src);
        clipboard.setPrimaryClip(clip);
        utils.ToastTips("Copy success!");
    }

    public static void SaveStringQRCode(ContentResolver cr, String data, String fileName){

        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(data,
                    BarcodeFormat.QR_CODE,
                    400,
                    400);

            ContentValues values=new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "ProtonAccount");
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.DESCRIPTION,"The QRCode of proton account");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

            Uri path = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
            OutputStream imageOut = cr.openOutputStream(path);
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, imageOut);
            } finally {
                imageOut.close();
            }

            utils.ToastTips("成功保存到相册");
        } catch(Exception e) {
            utils.ToastTips(e.getLocalizedMessage());
        }
    }

    public static String ParseQRCodeFile(Uri uri, ContentResolver cr) throws  Exception{
            Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(source));
            Reader reader = new MultiFormatReader();
            Result r = reader.decode(bb);

            return r.getText();
    }

    public static  boolean validEthAddress(String ethAddr){
        return !ethAddr.equals("") && !ethAddr.equals("0x0000000000000000000000000000000000000000");
    }

    public static boolean checkStorage(Activity ctx){
        if (!EasyPermissions.hasPermissions(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            EasyPermissions.requestPermissions(
                    ctx,
                    ctx.getString(R.string.rationale_extra_write),
                    utils.RC_IMAGE_GALLARY_PERM,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return false;
        }
        return true;
    }

    public static  boolean checkCamera(Activity ctx){
        if (!EasyPermissions.hasPermissions(ctx, Manifest.permission.CAMERA)) {
            EasyPermissions.requestPermissions(
                    ctx,
                    ctx.getString(R.string.camera),
                    utils.RC_CAMERA_PERM,
                    Manifest.permission.CAMERA);
            return false;
        }
        return true;
    }

}
