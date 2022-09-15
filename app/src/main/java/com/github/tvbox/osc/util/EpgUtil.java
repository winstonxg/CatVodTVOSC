package com.github.tvbox.osc.util;

import com.github.tvbox.osc.base.App;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.GetRequest;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EpgUtil {

    private static JXDocument epgDoc = null;

    public static void init() {
        if(epgDoc != null)
            return;

        String cachePath = App.getInstance().getCacheDir().getAbsolutePath() + "/epglist.html";
        File cacheEpg = new File(cachePath);
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, -10);
        if (cacheEpg.exists()) {
            if(cacheEpg.lastModified() > c.getTime().getTime()) {
                readCachedEpgList(cacheEpg);
            }
        }

        GetRequest<String> request = OkGo.<String>get("http://epg.51zmt.top:8000/");
        request.headers("User-Agent", UA.random());
        request.execute(new AbsCallback<String>() {
            @Override
            public void onSuccess(Response<String> response) {
                JSONObject returnedData = new JSONObject();
                try {
                    String pageStr = response.body();
                    Document doc = Jsoup.parse(pageStr);
                    Element mainTable = doc.body().getElementsByClass("table-primary").first();
                    epgDoc = JXDocument.create(mainTable.outerHtml());
                    if(cacheEpg.exists())
                        cacheEpg.delete();
                    FileOutputStream fos = new FileOutputStream(cacheEpg);
                    fos.write(mainTable.outerHtml().getBytes("UTF-8"));
                    fos.flush();
                    fos.close();
                } catch (Exception ex) { }
            }

            @Override
            public void onError(Response<String> response) {
                super.onError(response);
                if(cacheEpg.exists()) {
                    readCachedEpgList(cacheEpg);
                }
            }

            @Override
            public void onFinish() {
                super.onFinish();
            }

            @Override
            public String convertResponse(okhttp3.Response response) throws Throwable {
                return response.body().string();
            }
        });
    }

    private static void readCachedEpgList(File cacheEpg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String content = "";
                FileInputStream fin = null;
                BufferedReader reader = null;
                try {
                    fin = new FileInputStream(cacheEpg);
                    reader = new BufferedReader(new InputStreamReader(fin));
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    content = sb.toString();
                    epgDoc = JXDocument.create(content);
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (fin != null) {
                        try {
                            fin.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    public static String[] getEpgInfo(String channelName) {
        JXNode rowTd = epgDoc.selNOne("//td/a[text()='" + channelName + "']");
        Element row = null;
        if(rowTd != null)
            row = rowTd.asElement().parent().parent();
        else {
            rowTd = epgDoc.selNOne("//td[text()='" + channelName + "']");
            if(rowTd != null)
                row = rowTd.asElement().parent();
        }
        if (row != null)
        {
            String icon = row.select("img").attr("data-original");
            String epgTag = row.select("td:nth-child(4)").text();
            return new String[] {icon, epgTag};
        }
        return null;
    }
}
