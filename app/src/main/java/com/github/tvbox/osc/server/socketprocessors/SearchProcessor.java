package com.github.tvbox.osc.server.socketprocessors;

import android.content.Context;
import android.view.View;

import androidx.lifecycle.Observer;

import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.server.WebSocketServer;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.VodSearch;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class SearchProcessor {

    private static SearchProcessor instance;

    private SourceViewModel searchViewModel = new SourceViewModel();
    private VodSearch vodSearch = new VodSearch(searchViewModel);
    private String searchingTitle;

    public static SearchProcessor get() {
        if(instance == null)
            instance = new SearchProcessor();
        return instance;
    }

    public static void init() {
        if(instance != null)
            instance.destroy();
        SearchProcessor sp = get();
        EventBus.getDefault().register(instance);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_BACKSEARCH_RESULT) {
            try {
                searchData(event.obj == null ? null : (AbsXml) event.obj);
            } catch (Exception e) {
                searchData(null);
            }
        }
    }

    private void searchData(AbsXml absXml) {
        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
            List<Movie.Video> data = new ArrayList<>();
            for (Movie.Video video : absXml.movie.videoList) {
                if (video.name.contains(searchingTitle))
                    data.add(video);
            }
            JsonObject endJson = new JsonObject();
            endJson.addProperty("type", "back-search");
            endJson.add("results", JsonParser.parseString((new Gson()).toJson(data)));
            ControlManager.get().getSocketServer().sendToAll(endJson);
        }

        int count = vodSearch.getAllRunCount().decrementAndGet();
        if (count <= 0) {
            cancel();
        }
    }

    private void cancel() {
        OkGo.getInstance().cancelTag("back_search");
        JsonObject endJson = new JsonObject();
        endJson.addProperty("type", "search");
        endJson.addProperty("action", "end");
        WebSocketServer socketServer = ControlManager.get().getSocketServer();
        if(socketServer !=null)
            socketServer.sendToAll(endJson);
    }

    public void search(String searchTitle) {
        cancel();
        JsonObject startJson = new JsonObject();
        startJson.addProperty("type", "back-search");
        startJson.addProperty("action", "start");
        ControlManager.get().getSocketServer().sendToAll(startJson);
        searchingTitle = searchTitle;
        vodSearch.searchResult(searchTitle, true);
    }

    public void destroy() {
        cancel();
        vodSearch.destroy();
        EventBus.getDefault().unregister(instance);
        instance = null;
    }
}
