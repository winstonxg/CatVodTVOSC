package com.github.tvbox.osc.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;

import com.github.tvbox.osc.ui.dialog.UpdateDialog;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.request.GetRequest;
import com.orhanobut.hawk.Hawk;

import java.io.IOException;
import java.util.concurrent.Callable;

import okhttp3.Response;

public class AppUpdate {

    private static final String versionNo = "0.6.20220710";

    public static String getCurrentVersionNo() {
        return versionNo;
    }

    public void CheckLatestVersion(Context context, Boolean forceToShow, Callable<Void> hasNoUpdateCall) {
        GetRequest<String> request = OkGo.<String>get("https://raw.githubusercontent.com/kensonmiao/CatVodTVOSC_Release/main/latest_ver.txt");
        request.execute(new AbsCallback<String>() {

            @Override
            public String convertResponse(Response response) throws Throwable {
                return response.body().string();
            }

            @Override
            public void onSuccess(com.lzy.okgo.model.Response<String> response) {
                try {
                    String data = response.body();
                    String[] splitData = data.split("\n");
                    if (splitData.length > 0) {
                        String ignoredVersion = Hawk.get(HawkConfig.FORGET_NEW_VERSION, null);
                        if(!forceToShow && splitData[0].equals(ignoredVersion))
                            return;
                        String[] localVersionNo = versionNo.split("\\.");
                        String[] remoteVersionNo = splitData[0].split("\\.");
                        if(remoteVersionNo.length >localVersionNo.length) {
                            showUpdateDialog(context, splitData);
                            return;
                        }
                        for (int i = 0; i < localVersionNo.length && i < remoteVersionNo.length; i++) {
                            if (Integer.parseInt(remoteVersionNo[i]) > Integer.parseInt(localVersionNo[i])) {
                                showUpdateDialog(context, splitData);
                                return;
                            }
                        }
                    }
                }catch(Exception ex) {}
                if(hasNoUpdateCall != null) {
                    try {
                        hasNoUpdateCall.call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void showUpdateDialog(Context context, String[] splitData) {
        VersionInfo info = new VersionInfo();
        info.VersionNo = splitData[0];
        StringBuilder updatedInfoSB = new StringBuilder();
        if (splitData.length > 2) {
            for (int lineIndex = 2; lineIndex < splitData.length; lineIndex++) {
                updatedInfoSB.append(splitData[lineIndex]);
                updatedInfoSB.append("\n");
            }
        }
        info.UpdatedInfo = updatedInfoSB.toString();
        UpdateDialog dialog = new UpdateDialog(context, info);
        dialog.show();
    }

    public class VersionInfo {
        public String VersionNo;
        public String UpdatedInfo;
    }
}
