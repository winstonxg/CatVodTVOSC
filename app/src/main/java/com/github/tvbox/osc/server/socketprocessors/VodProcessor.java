package com.github.tvbox.osc.server.socketprocessors;

import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.server.WebSocketServer;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.PlayActivity;
import com.github.tvbox.osc.ui.fragment.PlayerFragment;
import com.github.tvbox.osc.util.AppManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.util.List;

public class VodProcessor {
    public void seek(JsonObject backData) {
        ControlManager.get().getSocketServer().getHandler().post(new Runnable() {
            @Override
            public void run() {
                AppManager appManager = AppManager.getInstance();
                DetailActivity detailActivity = (DetailActivity) appManager.getActivity(DetailActivity.class);
                if(detailActivity != null) {
                    PlayerFragment player = DetailActivity.getManagedPlayerFragment();
                    if(player != null) {
                        player.getVodController().setProgress(backData.get("percent").getAsDouble());
                    }
                }
            }
        });
    }

    public void close(JsonObject backData) {
        ControlManager.get().getSocketServer().getHandler().post(new Runnable() {
            @Override
            public void run() {
                AppManager appManager = AppManager.getInstance();
                appManager.backActivity(DetailActivity.class);
                appManager.finishActivity();
            }
        });
    }

    public void fullscreen(JsonObject backData) {
        ControlManager.get().getSocketServer().getHandler().post(new Runnable() {
            @Override
            public void run() {
                boolean isFullscreen = backData.get("isFullscreen").getAsBoolean();
                AppManager appManager = AppManager.getInstance();
                BaseActivity currentActivity = ((BaseActivity)appManager.currentActivity());
                if(currentActivity instanceof DetailActivity) {
                    PlayerFragment player = DetailActivity.getManagedPlayerFragment();
                    if(player != null) {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("type", "detail");
                        jsonObject.addProperty("fullscreen", isFullscreen);
                        if (isFullscreen) {
                            player.getVodController().startFullScreen();
                        } else {
                            ControlManager.get().getSocketServer().sendToAll(jsonObject);
                            player.getVodController().stopFullScreen();
                        }
                    }
                }
            }
        });
    }

    public void gotopos(JsonObject backData) {
        ControlManager.get().getSocketServer().getHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    String way = backData.get("way").getAsString();
                    AppManager appManager = AppManager.getInstance();
                    DetailActivity detailActivity = (DetailActivity) appManager.getActivity(DetailActivity.class);
                    if (detailActivity != null) {
                        PlayerFragment player = DetailActivity.getManagedPlayerFragment();
                        if (player != null) {
                            if (way.equalsIgnoreCase("next"))
                                player.playNext();
                            else if (way.equalsIgnoreCase("previous"))
                                player.playPrevious();
                            else {
                                JsonObject target = JsonParser.parseString(way).getAsJsonObject();
                                String flag = target.get("flag").getAsString();
                                int index = target.get("index").getAsInt();
                                if(index < 0)
                                    return;
                                VodInfo info = detailActivity.getVodInfo();
                                if(!info.seriesMap.keySet().contains(flag))
                                    return;
                                List<VodInfo.VodSeries> seriesList =  info.seriesMap.get(flag);
                                if(index >= seriesList.size())
                                    return;
                                int flagIndex = 0;
                                for(VodInfo.VodSeriesFlag flagObj : info.seriesFlags) {
                                    if(flagObj.name.equals(flag)) {
                                        flagObj.selected = true;
                                        info.playFlag = flag;
                                        detailActivity.updateSeriesFlagPosition(flagIndex);
                                    } else {
                                        flagObj.selected = false;
                                    }
                                    int loopIndex = 0;
                                    for (VodInfo.VodSeries episode : info.seriesMap.get(flagObj.name)) {
                                        if(flagObj.selected && loopIndex == index) {
                                            episode.selected = true;
                                            info.playIndex = index;
                                            detailActivity.jumpToPlay(false, true, null);
                                        }
                                        else
                                            episode.selected = false;
                                        loopIndex++;
                                    }
                                    flagIndex++;
                                }
                            }
                        }
                    }
                }catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        });
    }

    public void playpause(JsonObject backData) {
        ControlManager.get().getSocketServer().getHandler().post(new Runnable() {
            @Override
            public void run() {
                boolean isPlay = backData.get("isPlay").getAsBoolean();
                AppManager appManager = AppManager.getInstance();
                DetailActivity detailActivity = (DetailActivity) appManager.getActivity(DetailActivity.class);
                if(detailActivity != null) {
                    PlayerFragment player = DetailActivity.getManagedPlayerFragment();
                    if(player != null) {
                        if(player.getVideoView().isPlaying() && !isPlay)
                            player.getVideoView().pause();
                        else if(!player.getVideoView().isPlaying() && isPlay)
                            player.getVideoView().resume();
                    }
                }
            }
        });
    }

    public void parser(JsonObject backData) {
        ControlManager.get().getSocketServer().getHandler().post(new Runnable() {
            @Override
            public void run() {
                AppManager appManager = AppManager.getInstance();
                DetailActivity detailActivity = (DetailActivity) appManager.getActivity(DetailActivity.class);
                if (detailActivity != null) {
                    PlayerFragment player = DetailActivity.getManagedPlayerFragment();
                    if (player != null) {
                        int index = backData.get("index").getAsInt();
                        player.getVodController().updateParser(index);
                    }
                }
            }
        });
    }

    public void playerconfig(JsonObject backData) {
        ControlManager.get().getSocketServer().getHandler().post(new Runnable() {
            @Override
            public void run() {
                AppManager appManager = AppManager.getInstance();
                DetailActivity detailActivity = (DetailActivity) appManager.getActivity(DetailActivity.class);
                if(detailActivity != null) {
                    PlayerFragment player = DetailActivity.getManagedPlayerFragment();
                    JSONObject mVodPlayerCfg = null;
                    try {
                        mVodPlayerCfg = player.getVodController().getPlayerConfig();
                        String changedOption = null;
                        if (backData.has("pl")) {
                            changedOption = "pl";
                            mVodPlayerCfg.put("pl", backData.get("pl").getAsInt());
                        } else if (backData.has("pr")) {
                            changedOption = "pr";
                            mVodPlayerCfg.put("pr", backData.get("pr").getAsInt());
                        } else if (backData.has("ijk")) {
                            changedOption = "ijk";
                            mVodPlayerCfg.put("ijk", backData.get("ijk").getAsString());
                        } else if (backData.has("sc")) {
                            changedOption = "sc";
                            mVodPlayerCfg.put("sc", backData.get("sc").getAsInt());
                        } else if (backData.has("sp")) {
                            changedOption = "sp";
                            mVodPlayerCfg.put("sp", backData.get("sp").getAsFloat());
                        }
                        if(changedOption != null) {
                            player.getVodController().setPlayerConfig(mVodPlayerCfg);
                            player.getVodController().notifyPlayerConfigUpdate(changedOption);
                        }
                    } catch (Throwable th) {

                    }
                }
            }
        });
    }

    public void playing(JsonObject backData, WebSocketServer.NoticeWebSocket socket) {
        ControlManager.get().getSocketServer().getHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    AppManager appManager = AppManager.getInstance();
                    DetailActivity detailActivity = (DetailActivity) appManager.getActivity(DetailActivity.class);
                    if (detailActivity != null && detailActivity.getVodInfo() != null) {
                        PlayerFragment player = DetailActivity.getManagedPlayerFragment();
                        if (player != null) {
                            JsonObject jObj = new JsonObject();
                            jObj.addProperty("type", "ctrl");
                            jObj.addProperty("pos", player.getVideoView().getCurrentPosition());
                            jObj.addProperty("dur", player.getVideoView().getDuration());
                            jObj.addProperty("state", player.getVideoView().getCurrentPlayState());
                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        socket.send(jObj.toString());
                                    }catch (Throwable th) {
                                        th.printStackTrace();
                                    }
                                }
                            }.start();
                        }
                    }
                }catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        });
    }
}
