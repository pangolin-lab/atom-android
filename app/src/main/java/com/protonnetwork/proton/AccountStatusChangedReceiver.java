package com.protonnetwork.proton;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

public class AccountStatusChangedReceiver extends BroadcastReceiver {
    public static final String SofaAccountChanged = "com.sofa.receiver.sofaAccount.changed";
    public static final String VPNStatusChanged = "com.sofa.receiver.vpnstatus.changed";

    public static final int SofaAccountChangedAction = 1;
    public static final int EthAccountChangedAction = 2;
    public static final int VpnStatusChangedAction = 3;

    public static final String ActionKey = "ActionKey";
    public static final String ActionMsg = "ActionMessage";

    private Handler handler = null;

    public AccountStatusChangedReceiver(){
    }
    public AccountStatusChangedReceiver(Handler handler){
        this.handler = handler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int action = intent.getIntExtra(ActionKey, 0);

        switch (action){
            case SofaAccountChangedAction:
            case EthAccountChangedAction:
                handler.sendEmptyMessage(action);
                break;
            case VpnStatusChangedAction:
                Message message = Message.obtain();
                message.what = VpnStatusChangedAction;
                message.obj = intent.getStringExtra(ActionMsg);
                handler.sendMessage(message);
                break;
        }
    }
}
