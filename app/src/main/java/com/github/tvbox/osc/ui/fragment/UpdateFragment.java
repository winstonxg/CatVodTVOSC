package com.github.tvbox.osc.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.github.tvbox.osc.BuildConfig;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;

import java.io.File;
import java.io.FileOutputStream;

public class UpdateFragment extends BaseLazyFragment {

    public static final String UPDATE_FRAGMENT_TAG = "_update_fragment";
    private String versionNo = null;

    public UpdateFragment(String versionNo) {
        this.versionNo = versionNo;
    }

    @Override
    protected int getLayoutResID() {
        return R.layout.fragment_update;
    }

    @Override
    protected void init() {
        if(versionNo == null || versionNo.length() <= 0)
            return;

        String packageName = mContext.getPackageName();
        File sdcardDir = Environment.getExternalStorageDirectory(); // /storage/emulated/0/
        String appHomeDir = sdcardDir + File.separator + "Android" + File.separator + "data" + File.separator + packageName + File.separator;
        File cache = new File(appHomeDir + "files/apk/update.apk");

        OkGo.<File>get("https://raw.githubusercontent.com/kensonmiao/CatVodTVOSC_Release/main/" + this.versionNo + ".apk").execute(new AbsCallback<File>() {

            @Override
            public File convertResponse(okhttp3.Response response) throws Throwable {
                File cacheDir = cache.getParentFile();
                if (!cacheDir.exists())
                    cacheDir.mkdirs();
                if (cache.exists())
                    cache.delete();
                FileOutputStream fos = new FileOutputStream(cache);
                fos.write(response.body().bytes());
                fos.flush();
                fos.close();
                return cache;
            }

            @Override
            public void onSuccess(Response<File> response) {
                if (response.body().exists()) {
                    Uri contentUri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".fileprovider", cache);
                    Intent installer = new Intent(Intent.ACTION_VIEW);
                    installer.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    installer.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    installer.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                    installer.setDataAndType(contentUri, "application/vnd.android.package-archive");
                    startActivity(installer);
                } else {
                    try {
                        Toast.makeText(mActivity, "下载失败，指定安装包不存在", Toast.LENGTH_SHORT).show();
                    }catch(Exception ex) {}
                }
            }

            @Override
            public void onError(Response<File> response) {
                super.onError(response);
                try {
                    Toast.makeText(mActivity, "下载失败，仓库连接失败", Toast.LENGTH_SHORT).show();
                }catch (Exception ex) {}
            }
        });
    }
}
