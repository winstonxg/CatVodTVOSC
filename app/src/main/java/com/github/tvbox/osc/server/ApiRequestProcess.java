package com.github.tvbox.osc.server;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.ParseBean;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.server.socketprocessors.SearchProcessor;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.activity.LivePlayActivity;
import com.github.tvbox.osc.ui.activity.PlayActivity;
import com.github.tvbox.osc.ui.fragment.PlayerFragment;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.orhanobut.hawk.GsonParser;
import com.orhanobut.hawk.Hawk;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import okio.Utf8;
import xyz.doikki.videoplayer.player.VideoView;

public class ApiRequestProcess implements RequestProcess {

    protected Context mContext;
    private Handler mHandler;
    private Object syncLock = new Object();

    public ApiRequestProcess(Context context) {
        this.mContext = context;
        mHandler = new Handler(mContext.getMainLooper());
    }

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String fileName) {
        return false;
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String fileName, Map<String, String> params, Map<String, String> files) {
        switch (session.getMethod()) {
            case GET:
                return doGet(session, fileName, params, files);
            case POST:
                return doPost(session, fileName, params, files);
            case OPTIONS:
                return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.OK, "");
            default:
                return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED,
                        NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED.getDescription());
        }
    }

    private NanoHTTPD.Response doGet(NanoHTTPD.IHTTPSession session, String fileName, Map<String, String> params, Map<String, String> files) {
        if(params.containsKey("type")) {
            String type = params.get("type");
            if(type.equalsIgnoreCase("api-list")) {
                SourceBean homeSource = ApiConfig.get().getHomeSourceBean();
                List<SourceBean> sources = ApiConfig.get().getSourceBeanList();
                JsonObject returnedData = new JsonObject();
                returnedData.addProperty("homeKey", homeSource.getKey());
                JsonArray sourceArr = new JsonArray();
                for (SourceBean source : sources) {
                    JsonObject jSource = new JsonObject();
                    jSource.addProperty("key", source.getKey());
                    jSource.addProperty("name", source.getName());
                    sourceArr.add(jSource);
                }
                returnedData.add("sources", sourceArr);
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, returnedData.toString());
            } else if(type.equalsIgnoreCase("category-list")) {
                String sourceKey = params.get("sourceKey");
                SourceViewModel model = new SourceViewModel();
                SourceBean bean = ApiConfig.get().getSource(sourceKey);
                if(bean == null)
                    return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "");
                this.executeObserver(model, model.sortResult, new Runnable() {
                    @Override
                    public void run() {
                        model.getSort(bean.getKey());
                    }
                });
                AbsSortXml categoryData = model.sortResult.getValue();
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, (new Gson()).toJson(categoryData));
            } else if(type.equalsIgnoreCase("category-content")) {
                String sourceKey = params.get("sourceKey");
                String pageStr = params.get("page");
                String filterStr = params.get("filters");
                String id = params.get("id");
                HashMap<String, String> filters = new HashMap<>();
                if(!StringUtils.isEmpty(filterStr)) {
                    String decodedFilterStr = new String(Base64.decode(filterStr, 0), StandardCharsets.UTF_8);
                    JsonArray jFilters = JsonParser.parseString(decodedFilterStr).getAsJsonArray();
                    for(JsonElement jFilter : jFilters) {
                        JsonObject jFilterObj = jFilter.getAsJsonObject();
                        filters.put(jFilterObj.get("k").getAsString(), jFilterObj.get("v").getAsString());
                    }
                }
                SourceViewModel model = new SourceViewModel();
                SourceBean bean = ApiConfig.get().getSource(sourceKey);
                if(bean == null)
                    return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "");
                this.executeObserver(model, model.listResult, new Runnable() {
                    @Override
                    public void run() {
                        MovieSort.SortData sortData = new MovieSort.SortData();
                        sortData.id = id;
                        sortData.filterSelect = filters;
                        int page = 1;
                        if(!StringUtils.isEmpty(pageStr))
                            page = Integer.parseInt(pageStr);
                        model.getList(sortData, page, bean);
                    }
                });
                AbsXml data = model.listResult.getValue();
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, (new Gson()).toJson(data));
            } else if(type.equalsIgnoreCase("playing-info")) {
                AppManager appManager = AppManager.getInstance();
                DetailActivity detailActivity = (DetailActivity) appManager.getActivity(DetailActivity.class);
                if(detailActivity != null && detailActivity.getVodInfo() != null) {
                    String jsonStr = (new Gson()).toJson(detailActivity.getVodInfo());
                    JsonObject jObj = JsonParser.parseString(jsonStr).getAsJsonObject();
                    PlayerFragment player = DetailActivity.getManagedPlayerFragment();
                    if(player != null) {
                        jObj.addProperty("playState", player.getVideoView().getCurrentPlayState());
                        jObj.addProperty("fullscreen", appManager.currentActivity() instanceof PlayActivity);
                    } else {
                        jObj.addProperty("playState", VideoView.STATE_IDLE);
                        jObj.addProperty("fullscreen", false);
                    }
                    jObj.add("ijkCodes", JsonParser.parseString((new Gson()).toJson(ApiConfig.get().getIjkCodes()).toString()));
                    return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, jObj.toString());
                } else {
                    return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.NOT_FOUND, "{}");
                }
            } else if (type.equalsIgnoreCase("parser-flags")) {
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, (new Gson()).toJson(ApiConfig.get().getVipParseFlags()));
            } else if (type.equalsIgnoreCase("parsers")) {
                ParseBean pb = ApiConfig.get().getDefaultParse();
                JsonObject parserObj = new JsonObject();
                parserObj.addProperty("selected", pb.getName());
                parserObj.add("parsers", JsonParser.parseString((new Gson()).toJson(ApiConfig.get().getParseBeanList())));
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, parserObj.toString());
            } else if (type.equalsIgnoreCase("live")) {
                List<LiveChannelGroup> list = ApiConfig.get().getChannelGroupList();
                if (list.size() == 1 && list.get(0).getGroupName().startsWith("http://127.0.0.1")) {
                    try {
                        String liveData = OkGo.get(list.get(0).getGroupName()).execute().body().string();
                        JsonArray livesArray = new Gson().fromJson(liveData, JsonArray.class);
                        ApiConfig.get().loadLives(livesArray);
                        list = ApiConfig.get().getChannelGroupList();
                    }catch (Exception ex) {}
                }
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, (new Gson()).toJson(list));
            } else if(type.equalsIgnoreCase("live-last-channel")) {
                String lastChannelName = Hawk.get(HawkConfig.LIVE_CHANNEL, "");
                return RemoteServer.createPlainTextResponse(NanoHTTPD.Response.Status.OK, lastChannelName);
            }
        }
        return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, "{}");
    }

    private <T> void executeObserver(SourceViewModel model, MutableLiveData<T> result, Runnable runnable) {
        Observer<T> obs = new Observer<T>() {
            @Override
            public void onChanged(T xml) {
                try {
                    synchronized (syncLock) {
                        syncLock.notifyAll();
                    }
                }catch (Exception ex) {}
                result.removeObserver(this);
            }
        };
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                result.observeForever(obs);
                mHandler.post(runnable);
            }
        });
        synchronized (syncLock) {
            try {
                syncLock.wait(30000);
            } catch (Exception ex) {
            }
        }
    }

    private NanoHTTPD.Response doPost(NanoHTTPD.IHTTPSession session, String fileName, Map<String, String> params, Map<String, String> files) {
        if(params.containsKey("type")) {
            String type = params.get("type");
            if(type.equalsIgnoreCase("play")) {
                try {
                    final HashMap<String, String> map = new HashMap<>();
                    session.parseBody(map);
                    final String json = map.get("postData");
                    JsonObject jObj = JsonParser.parseString(json).getAsJsonObject();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                AppManager appManager = AppManager.getInstance();
                                Activity detailActivity = appManager.getActivity(DetailActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putString("id", jObj.get("id").getAsString());
                                String sourceKey = jObj.get("sourceKey").getAsString();
                                List<SourceBean> sourceBeans = ApiConfig.get().getSourceBeanList();
                                boolean hasSource = false;
                                for(SourceBean sb : sourceBeans) {
                                    if(sb.getKey().equals(sourceKey)) {
                                        hasSource = true;
                                        break;
                                    }
                                }
                                if(!hasSource)
                                    return;
                                bundle.putString("sourceKey", sourceKey);
                                if(detailActivity != null) {
                                    Activity currentActivity = appManager.currentActivity();
                                    if(currentActivity instanceof PlayActivity)
                                        ((PlayActivity)currentActivity).onBackPressed();
                                    else
                                        appManager.backActivity(DetailActivity.class);
                                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_VOD_PLAY, bundle));
                                } else {
                                    ((BaseActivity) AppManager.getInstance().currentActivity()).jumpActivity(DetailActivity.class, bundle);
                                }
                            } catch (Exception ex) {
                            }
                        }
                    });
                }catch (Exception ex) {
                    return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, "{\"succeeded\":false}");
                }
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, "{\"succeeded\": true}");
            }
            else if(type.equalsIgnoreCase("search")) {
                try {
                    String title = params.get("title");
                    if (title != null && !title.isEmpty()) {
                        SearchProcessor.get().search(title);
                        return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, "{\"succeeded\": true}");
                    }
                }catch (Exception ex) {
                    ex.printStackTrace();
                }
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, "{\"succeeded\":false}");
            }
            else if(type.equalsIgnoreCase("play-live")) {
                try {
                    final HashMap<String, String> map = new HashMap<>();
                    session.parseBody(map);
                    final String json = map.get("postData");
                    JsonObject jObj = JsonParser.parseString(json).getAsJsonObject();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                AppManager appManager = AppManager.getInstance();
                                Activity liveActivity = appManager.getActivity(LivePlayActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putInt("groupIndex", jObj.get("groupIndex").getAsInt());
                                bundle.putInt("channelIndex", jObj.get("channelIndex").getAsInt());
                                if(liveActivity != null) {
                                    appManager.backActivity(LivePlayActivity.class);
                                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_LIVEPLAY_UPDATE, bundle));
                                } else {
                                    ((BaseActivity) AppManager.getInstance().currentActivity()).jumpActivity(LivePlayActivity.class, bundle);
                                }
                            } catch (Exception ex) {
                            }
                        }
                    });
                }catch (Exception ex) {
                    return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, "{\"succeeded\":false}");
                }
                return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, "{\"succeeded\": true}");
            }
        }
        return RemoteServer.createJSONResponse(NanoHTTPD.Response.Status.OK, "{}");
    }
}
