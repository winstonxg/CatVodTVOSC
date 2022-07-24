package com.github.tvbox.osc.player.thirdparty;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import com.github.tvbox.osc.base.App;

import java.util.HashMap;

/***
 *
 * @author https://github.com/anaer
 *
 */
public class DangbeiPlayer {
    public static final String TAG = "ThirdParty.Dangbei";

    private static final String PACKAGE_NAME = "com.dangbei.lerad.videoposter";
    private static final String PLAYBACK_ACTIVITY = "com.dangbei.leradplayer.activity.PlayerActivity";

    private static class DangbeiPackageInfo {
        final String packageName;
        final String activityName;

        DangbeiPackageInfo(String packageName, String activityName) {
            this.packageName = packageName;
            this.activityName = activityName;
        }
    }

    private static final DangbeiPackageInfo[] PACKAGES = {
            new DangbeiPackageInfo(PACKAGE_NAME, PLAYBACK_ACTIVITY),
    };

    public static DangbeiPackageInfo getPackageInfo() {
        for (DangbeiPackageInfo pkg : PACKAGES) {
            try {
                ApplicationInfo info = App.getInstance().getPackageManager().getApplicationInfo(pkg.packageName, 0);
                if (info.enabled)
                    return pkg;
                else
                    Log.v(TAG, "Dangbei Player package `" + pkg.packageName + "` is disabled.");
            } catch (PackageManager.NameNotFoundException ex) {
                Log.v(TAG, "Dangbei Player package `" + pkg.packageName + "` does not exist.");
            }
        }
        return null;
    }

    /**
     * 判断是否可用.
     * @return
     */
    public static boolean isAvailable(){
        return getPackageInfo() != null;
    }

    public static boolean run(Activity activity, String url, String title, String subtitle, HashMap<String, String> headers) {
        DangbeiPackageInfo packageInfo = getPackageInfo();
        if (packageInfo == null)
            return false;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setPackage(packageInfo.packageName);
        intent.setComponent(new ComponentName(packageInfo.packageName, packageInfo.activityName));

        // 标题取的url最后一个/后的内容
        if(url.contains("?")){
            url = url+"&__=/"+title;
        }else{
            url = url+"?__=/"+title;
        }

        intent.setData(Uri.parse(url));
        intent.putExtra("title", title);
        try {
            activity.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Can't run Dangbei Player", ex);
            return false;
        }
    }
}