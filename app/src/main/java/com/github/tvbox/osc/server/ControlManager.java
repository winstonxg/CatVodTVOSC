package com.github.tvbox.osc.server;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.receiver.SearchReceiver;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.activity.SettingActivity;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.HawkConfig;
import com.google.gson.JsonObject;
import com.orhanobut.hawk.Hawk;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * @author pj567
 * @date :2021/1/4
 * @description:
 */
public class ControlManager {
    private static ControlManager instance;
    private RemoteServer mServer = null;
    private WebSocketServer socketServer = null;
    public static Context mContext;

    private ControlManager() {

    }

    public static ControlManager get() {
        if (instance == null) {
            synchronized (ControlManager.class) {
                if (instance == null) {
                    instance = new ControlManager();
                }
            }
        }
        return instance;
    }

    public static void init(Context context) {
        mContext = context;
    }

    public String getAddress(boolean local) {
        return local ? mServer.getLoadAddress() : mServer.getServerAddress();
    }

    public void startServer() {
        if (mServer != null) {
            return;
        }
        do {
            mServer = new RemoteServer(RemoteServer.serverPort, mContext);
            mServer.setDataReceiver(new DataReceiver() {
                @Override
                public void onTextReceived(String text) {
                    if (!TextUtils.isEmpty(text)) {
                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("title", text);
                        intent.setAction(SearchReceiver.action);
                        intent.setPackage(mContext.getPackageName());
                        intent.setComponent(new ComponentName(mContext, SearchReceiver.class));
                        intent.putExtras(bundle);
                        mContext.sendBroadcast(intent);
                    }
                }

                @Override
                public void onApiReceived(String url) {
                    BaseActivity currentActivity = (BaseActivity) AppManager.getInstance().currentActivity();
                    if(currentActivity instanceof SettingActivity) {
                        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_API_URL_CHANGE, url));
                    } else if(!StringUtils.isEmpty(url)){
                        if (!url.equals(Hawk.get(HawkConfig.API_URL, ""))) {
                            AppManager.getInstance().backActivity(HomeActivity.class);
                            currentActivity = (BaseActivity) AppManager.getInstance().currentActivity();
                            Bundle bundle = new Bundle();
                            bundle.putString("newApi", url);
                            currentActivity.jumpActivity(SettingActivity.class, bundle);
                        }
                    }
                }

                @Override
                public void onPushReceived(String url) {
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_PUSH_URL, url));
                }
            });
            try {
                mServer.start();
                IjkMediaPlayer.setDotPort(Hawk.get(HawkConfig.DOH_URL, 0) > 0, RemoteServer.serverPort);
                break;
            } catch (IOException ex) {
                RemoteServer.serverPort++;
                mServer.stop();
            }
        } while (RemoteServer.serverPort < 9999);
        if (socketServer != null) {
            return;
        }
        do {
            socketServer = new WebSocketServer(WebSocketServer.serverPort, mContext);
            try {
                socketServer.start(60000, true);
                break;
            } catch (IOException ex) {
                WebSocketServer.serverPort++;
                socketServer.stop();
            }
        } while (RemoteServer.serverPort < 9899);
    }

    public WebSocketServer getSocketServer() {
        return socketServer;
    }

    public void stopServer() {
        if (mServer != null && mServer.isStarting()) {
            mServer.stop();
        }
        if(socketServer != null && socketServer.wasStarted()) {
            socketServer.stop();
        }
    }
}