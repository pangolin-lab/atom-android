package com.protonnetwork.proton.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.protonnetwork.proton.AccountStatusChangedReceiver;
import com.protonnetwork.proton.MainActivity;
import com.protonnetwork.proton.R;
import com.protonnetwork.proton.ProtonAccount;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import androidLib.AndroidLib;

public class ProtonService extends VpnService implements androidLib.VpnDelegate, Runnable {
    private static ConcurrentHashMap<onStatusChangedListener, Object> mOnStatusChangedListeners = new ConcurrentHashMap<onStatusChangedListener, Object>();
    public static boolean IsRunning = false;

    public static final long IDLE_INTERVAL_MS = TimeUnit.MILLISECONDS.toMillis(100);
    public static final String DATA_PASSWORD = "com.protonnetwork.proton.PassWord";
    public static final String TAG = "ProtonService";
    private static final String LOCAL_IP = "10.8.0.2";
    public static final String BootNodeSavePath =  "OutProtonBootNodes.dat";
//    public static final String OUT_TICKET = "https://raw.githubusercontent.com/proton-lab/quantum/master/seed_debug.quantum";
//    public static final String OUT_TICKET = "https://raw.githubusercontent.com/proton-lab/quantum/master/seed.quantum";
    public static final String OUT_TICKET = "https://gitee.com/protonlab/quantum/raw/master/seed.quantum";


    private Thread m_VPNThread;
    private ParcelFileDescriptor mInterface;
    private PendingIntent mConfigureIntent;
    private FileOutputStream mVpnOutputStream;
    private Handler m_Handler;

    public static void Stop() {
        IsRunning = false;
        AndroidLib.stopVpn();
    }

    public static void reloadSeedNodes(Context ctx) {
        String bootPath = ctx.getDir("Pronton", MODE_PRIVATE) + File.separator + BootNodeSavePath;
        AndroidLib.reloadSeedNodes(OUT_TICKET, bootPath);
    }

    public interface onStatusChangedListener {
        public void onStatusChanged(String status, Boolean isRunning);
    }

    @Override
    public void onCreate() {
        m_Handler = new Handler();
        mConfigureIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        super.onCreate();
    }

    public static void addOnStatusChangedListener(onStatusChangedListener listener) {
        if (!mOnStatusChangedListeners.containsKey(listener)) {
            mOnStatusChangedListeners.put(listener, 1);
        }
    }

    public static void removeOnStatusChangedListener(onStatusChangedListener listener) {
        if (mOnStatusChangedListeners.containsKey(listener)) {
            mOnStatusChangedListeners.remove(listener);
        }
    }

    private void onStatusChanged(final String status, final boolean isRunning) {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<onStatusChangedListener, Object> entry : mOnStatusChangedListeners.entrySet()) {
                    entry.getKey().onStatusChanged(status, isRunning);
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (m_VPNThread != null && m_VPNThread.isAlive()){
            return START_NOT_STICKY;
        }
        m_VPNThread = new Thread(this, "VPNServiceThread");

        startNotification();

        final String password = intent.getStringExtra(DATA_PASSWORD);
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean setup = establishVPN(password);
                if (!setup){
                    ProtonService.Stop();
                    disconnectVPN();
                }
            }
        }).start();


        return super.onStartCommand(intent, flags, startId);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId , String channelName){
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(chan);
        return channelId;
    }

    void startNotification(){

        Notification.Builder builder = new Notification.Builder(this.getApplicationContext()); //获取一个Notification构造器
        builder.setContentIntent(mConfigureIntent)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),R.mipmap.ic_launcher))
                .setContentTitle("质子区块链网络")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("proton BlockChain network")
                .setWhen(System.currentTimeMillis());

        String channelId = "com.protonnetwork.protonservcie.channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel(channelId, "com.protonnetwork.protonservcie");
            builder.setChannelId(channelId);
        }

        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音

        startForeground(110, notification);
    }

    public void disconnectVPN(){
        try {

            mVpnOutputStream.close();
            mVpnOutputStream = null;

            if (mInterface != null) {
                mInterface.close();
                mInterface = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        onStatusChanged("Disconnected", false);
        IsRunning = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public boolean establishVPN(String password) {

        try {
            InputStream ipInput = getResources().openRawResource(R.raw.bypass);
            String chineseIPs = IOUtils.toString(ipInput);

            String bootNodes = loadSavedBootNode();

            String addr = ProtonAccount.Instance().ProtonAddress;
            String cipher = ProtonAccount.Instance().CipherTxt;
            AndroidLib.initVPN(addr, cipher,
                    OUT_TICKET, bootNodes, chineseIPs, this);

            VpnService.Builder builder = new Builder();
            builder.addAddress(LOCAL_IP, 32);
            builder.addRoute("0.0.0.0", 0);
//            String pk = getPackageName();
//            builder.addDisallowedApplication(pk);
//            builder.addDnsServer("8.8.8.8");
//            builder.addDnsServer("8.8.4.4");
            builder.setConfigureIntent(mConfigureIntent);
            mInterface = builder.establish();

            FileInputStream inputStream = new FileInputStream(mInterface.getFileDescriptor());
            mVpnOutputStream = new FileOutputStream(mInterface.getFileDescriptor());
            AndroidLib.setupVpn(password,  LOCAL_IP + ":51080");//TODO::port

            IsRunning = true;
            PacketReader reader = new PacketReader(inputStream);
            Thread m_ReadingThread = new Thread(reader);
            m_ReadingThread.start();

            m_VPNThread.start();

            onStatusChanged("Connected Success", true);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Intent i = new Intent(AccountStatusChangedReceiver.VPNStatusChanged);
            i.putExtra(AccountStatusChangedReceiver.ActionKey, AccountStatusChangedReceiver.VpnStatusChangedAction);
            i.putExtra(AccountStatusChangedReceiver.ActionMsg, "链接失败:" + e.getLocalizedMessage());
            sendBroadcast(i);
            return false;
        }
    }

    @Override
    public synchronized void run() {
        AndroidLib.proxying();
        Log.w("ProtonService", "------>Proxying thread exit......");
        this.disconnectVPN();
    }


//TODO::move this function to manager.
    public String loadSavedBootNode(){try {
        File resFile = new File(getBootPath());
        if (!resFile.exists()){
            resFile.createNewFile();
            return "";
        }
        FileInputStream fis  = new FileInputStream(resFile);
        String bootNodes = IOUtils.toString(fis);

        return bootNodes;

        }catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public boolean byPass(int fd) {
        Log.i(TAG, "bypass: fd=" + fd);
        return this.protect(fd);
    }

    @Override
    public String getBootPath() {
        return getDir("Pronton", MODE_PRIVATE) + File.separator + BootNodeSavePath;
    }

    @Override
    public void log(String str) {
        Log.i(TAG, str);
    }

    @Override
    public long write(byte[] b) throws Exception {
        mVpnOutputStream.write(b);
//        String encodedString = Base64.encodeToString(b, Base64.DEFAULT);
//            Log.e(TAG, "Base64--"+b.length+"-->" + encodedString);
        return b.length;
    }
}
class PacketReader implements Runnable{

    FileInputStream mReader;
    private static final int MAX_PACKET_SIZE = Short.MAX_VALUE;
    private ByteBuffer readerBuf = ByteBuffer.allocate(MAX_PACKET_SIZE);
    PacketReader(FileInputStream fi){
        mReader = fi;
    }

    @Override
    public void run() {
        try {
            while (ProtonService.IsRunning) {
                int length = mReader.read(readerBuf.array());
                if (length == 0) {
                    Thread.sleep(ProtonService.IDLE_INTERVAL_MS);
                    continue;
                }
                readerBuf.limit(length);
                byte[] dst = Arrays.copyOf(readerBuf.array(), length);
                AndroidLib.inputPacket(dst);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.w("ProtonService", "------>Packet reading thread exit......");
        //TODO::stop notification
    }
}