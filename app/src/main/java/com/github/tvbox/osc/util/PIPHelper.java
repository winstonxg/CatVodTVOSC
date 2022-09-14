package com.github.tvbox.osc.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.RemoteAction;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.github.tvbox.osc.ui.activity.DetailActivity;

public class PIPHelper {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static RemoteAction generateRemoteAction(Activity activity, int iconResId, int actionCode, String title, String desc) {
        final PendingIntent intent =
                PendingIntent.getBroadcast(
                        activity,
                        actionCode,
                        new Intent("PIP_VOD_CONTROL").putExtra("action", actionCode),
                        0);
        final Icon icon = Icon.createWithResource(activity, iconResId);
        return (new RemoteAction(icon, title, desc, intent));
    }

    public static boolean supportsPiPMode() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }
}
