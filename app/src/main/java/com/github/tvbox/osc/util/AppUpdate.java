package com.github.tvbox.osc.util;

import android.content.Context;

import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.ui.dialog.UpdateDialog;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.request.GetRequest;
import com.orhanobut.hawk.Hawk;

import java.util.concurrent.Callable;

import okhttp3.Response;

public class AppUpdate {

    private static final String[] updateUrls = new String[] {
            "https://raw.githubusercontent.com/kensonmiao/CatVodTVOSC_Release/main/",
            "https://codeberg.org/kensonlogin55/CatVodTVOSC_Release/raw/branch/main/"
    };

    public static String getCurrentVersionNo() {
        return BuildConfig.VERSION_NAME;
    }

    public void CheckLatestVersion(Context context, Boolean forceToShow, Callable<Void> hasNoUpdateCall) {
        InternalCheckVersion(context, forceToShow, hasNoUpdateCall, 0);
    }

    private void InternalCheckVersion(Context context, Boolean forceToShow, Callable<Void> hasNoUpdateCall, int sourceIndex) {
        GetRequest<String> request = OkGo.<String>get(updateUrls[sourceIndex] + "latest_ver.txt");
        request.execute(new AbsCallback<String>() {

            @Override
            public String convertResponse(Response response) throws Throwable {
                return response.body().string();
            }

            @Override
            public void onError(com.lzy.okgo.model.Response<String> response) {
                super.onError(response);
                if(sourceIndex + 1 < updateUrls.length)
                    InternalCheckVersion(context, forceToShow, hasNoUpdateCall, sourceIndex+1);
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
                        String[] localVersionNo = getCurrentVersionNo().split("\\.");
                        String[] remoteVersionNo = splitData[0].split("\\.");
                        for (int i = 0; i < localVersionNo.length && i < remoteVersionNo.length; i++) {
                            if (Integer.parseInt(remoteVersionNo[i]) > Integer.parseInt(localVersionNo[i])) {
                                showUpdateDialog(context, updateUrls[sourceIndex], splitData);
                                return;
                            }
                            if (Integer.parseInt(remoteVersionNo[i]) < Integer.parseInt(localVersionNo[i])) {
                                return;
                            }
                        }
                        if(remoteVersionNo.length > localVersionNo.length) {
                            showUpdateDialog(context, updateUrls[sourceIndex], splitData);
                            return;
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

    private void showUpdateDialog(Context context, String source, String[] splitData) {
        VersionInfo info = new VersionInfo();
        info.VersionNo = splitData[0];
        info.Source = source;
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
        public String Source;
        public String UpdatedInfo;
    }
}
