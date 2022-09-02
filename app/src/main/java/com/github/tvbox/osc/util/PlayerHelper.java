package com.github.tvbox.osc.util;

import android.app.Activity;
import android.content.Context;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.IJKCode;
import com.github.tvbox.osc.player.IjkMediaPlayer;
import com.github.tvbox.osc.player.render.SurfaceRenderViewFactory;
import com.github.tvbox.osc.player.thirdparty.DangbeiPlayer;
import com.github.tvbox.osc.player.thirdparty.KodiPlayer;
import com.github.tvbox.osc.player.thirdparty.MXPlayer;
import com.github.tvbox.osc.player.thirdparty.ReexPlayer;
import com.github.tvbox.osc.player.thirdparty.UCPlayer;
import com.orhanobut.hawk.Hawk;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import tv.danmaku.ijk.media.player.IjkLibLoader;
import xyz.doikki.videoplayer.exo.ExoMediaPlayerFactory;
import xyz.doikki.videoplayer.player.AndroidMediaPlayerFactory;
import xyz.doikki.videoplayer.player.PlayerFactory;
import xyz.doikki.videoplayer.player.VideoView;
import xyz.doikki.videoplayer.render.RenderViewFactory;
import xyz.doikki.videoplayer.render.TextureRenderViewFactory;

public class PlayerHelper {

    private static Map<Integer, String> AVAILABLE_DEFAULT_PLAYERS = new TreeMap<Integer, String>() {{
        put(0, "系统播放器");
        put(1, "IJK播放器");
        put(2, "Exo播放器");
    }};
    private static Map<Integer, String> AVAILABLE_3RD_PLAYERS = new TreeMap<Integer, String>();


    public static void updateCfg(VideoView videoView, JSONObject playerCfg) {
        int playerType = Hawk.get(HawkConfig.PLAY_TYPE, 0);
        int renderType = Hawk.get(HawkConfig.PLAY_RENDER, 0);
        String ijkCode = Hawk.get(HawkConfig.IJK_CODEC, "软解码");
        int scale = Hawk.get(HawkConfig.PLAY_SCALE, 0);
        try {
            playerType = playerCfg.getInt("pl");
            renderType = playerCfg.getInt("pr");
            ijkCode = playerCfg.getString("ijk");
            scale = playerCfg.getInt("sc");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        IJKCode codec = ApiConfig.get().getIJKCodec(ijkCode);
        PlayerFactory playerFactory;
        if (playerType == 1) {
            playerFactory = new PlayerFactory<IjkMediaPlayer>() {
                @Override
                public IjkMediaPlayer createPlayer(Context context) {
                    return new IjkMediaPlayer(context, codec);
                }
            };
            try {
                tv.danmaku.ijk.media.player.IjkMediaPlayer.loadLibrariesOnce(new IjkLibLoader() {
                    @Override
                    public void loadLibrary(String s) throws UnsatisfiedLinkError, SecurityException {
                        try {
                            System.loadLibrary(s);
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }
                });
            } catch (Throwable th) {
                th.printStackTrace();
            }
        } else if (playerType == 2) {
            playerFactory = ExoMediaPlayerFactory.create();
        } else {
            playerFactory = AndroidMediaPlayerFactory.create();
        }
        RenderViewFactory renderViewFactory = null;
        switch (renderType) {
            case 0:
            default:
                renderViewFactory = TextureRenderViewFactory.create();
                break;
            case 1:
                renderViewFactory = SurfaceRenderViewFactory.create();
                break;
        }
        videoView.setPlayerFactory(playerFactory);
        videoView.setRenderViewFactory(renderViewFactory);
        videoView.setScreenScaleType(scale);
    }

    public static void updateCfg(VideoView videoView) {
        int playType = Hawk.get(HawkConfig.PLAY_TYPE, 0);
        PlayerFactory playerFactory;
        if (playType == 1) {
            playerFactory = new PlayerFactory<IjkMediaPlayer>() {
                @Override
                public IjkMediaPlayer createPlayer(Context context) {
                    return new IjkMediaPlayer(context, null);
                }
            };
            try {
                tv.danmaku.ijk.media.player.IjkMediaPlayer.loadLibrariesOnce(new IjkLibLoader() {
                    @Override
                    public void loadLibrary(String s) throws UnsatisfiedLinkError, SecurityException {
                        try {
                            System.loadLibrary(s);
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }
                });
            } catch (Throwable th) {
                th.printStackTrace();
            }
        } else if (playType == 2) {
            playerFactory = ExoMediaPlayerFactory.create();
        } else {
            playerFactory = AndroidMediaPlayerFactory.create();
        }
        int renderType = Hawk.get(HawkConfig.PLAY_RENDER, 0);
        RenderViewFactory renderViewFactory = null;
        switch (renderType) {
            case 0:
            default:
                renderViewFactory = TextureRenderViewFactory.create();
                break;
            case 1:
                renderViewFactory = SurfaceRenderViewFactory.create();
                break;
        }
        videoView.setPlayerFactory(playerFactory);
        videoView.setRenderViewFactory(renderViewFactory);
    }


    public static void init() {
        try {
            tv.danmaku.ijk.media.player.IjkMediaPlayer.loadLibrariesOnce(new IjkLibLoader() {
                @Override
                public void loadLibrary(String s) throws UnsatisfiedLinkError, SecurityException {
                    try {
                        System.loadLibrary(s);
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                }
            });
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public static String getPlayerName(int playType) {
        if(AVAILABLE_DEFAULT_PLAYERS.containsKey(playType))
            return AVAILABLE_DEFAULT_PLAYERS.get(playType);
        else
            return AVAILABLE_DEFAULT_PLAYERS.get(0);
    }

    public static String get3rdPlayerName(int playType) {
        if(AVAILABLE_3RD_PLAYERS.containsKey(playType))
            return AVAILABLE_3RD_PLAYERS.get(playType);
        else {
            Integer[] types = getAvailable3rdPlayerTypes();
            if(types.length > 0)
                return AVAILABLE_3RD_PLAYERS.get(types[0]);
            else
                return null;
        }
    }

    public static String getRenderName(int renderType) {
        if (renderType == 1) {
            return "SurfaceView";
        } else {
            return "TextureView";
        }
    }

    public static String getScaleName(int screenScaleType) {
        String scaleText = "默认";
        switch (screenScaleType) {
            case VideoView.SCREEN_SCALE_DEFAULT:
                scaleText = "默认";
                break;
            case VideoView.SCREEN_SCALE_16_9:
                scaleText = "16:9";
                break;
            case VideoView.SCREEN_SCALE_4_3:
                scaleText = "4:3";
                break;
            case VideoView.SCREEN_SCALE_MATCH_PARENT:
                scaleText = "填充";
                break;
            case VideoView.SCREEN_SCALE_ORIGINAL:
                scaleText = "原始";
                break;
            case VideoView.SCREEN_SCALE_CENTER_CROP:
                scaleText = "裁剪";
                break;
        }
        return scaleText;
    }

    public static void reload3rdPlayers() {
        AVAILABLE_3RD_PLAYERS.clear();
        if(MXPlayer.getPackageInfo() != null) {
            AVAILABLE_3RD_PLAYERS.put(10, "MX Player");
        }
        if(ReexPlayer.getPackageInfo() != null) {
            AVAILABLE_3RD_PLAYERS.put(11, "Reex Player");
        }
        if(UCPlayer.getPackageInfo() != null) {
            AVAILABLE_3RD_PLAYERS.put(12, "UC浏览器");
        }
        if(DangbeiPlayer.getPackageInfo() != null) {
            AVAILABLE_3RD_PLAYERS.put(13, "当贝播放器");
        }
        if(KodiPlayer.getPackageInfo() != null) {
            AVAILABLE_3RD_PLAYERS.put(14, "Kodi");
        }
    }

    public static boolean playOn3rdPlayer(int playerType, Activity mActivity, String playingUrl, String playTitle, String playSubtitle, HashMap<String, String> playingHeader) {
        boolean callResult = false;
        switch (playerType) {
            case 10: {
                callResult = MXPlayer.run(mActivity, playingUrl, playTitle, playSubtitle, playingHeader);
                break;
            }
            case 11: {
                callResult = ReexPlayer.run(mActivity, playingUrl, playTitle, playSubtitle, playingHeader);
                break;
            }
            case 12: {
                callResult = UCPlayer.run(mActivity, playingUrl, playTitle, playSubtitle, playingHeader);
                break;
            }
            case 13: {
                callResult = DangbeiPlayer.run(mActivity, playingUrl, playTitle, playSubtitle, playingHeader);
                break;
            }
            case 14: {
                callResult = KodiPlayer.run(mActivity, playingUrl, playTitle, playSubtitle, playingHeader);
            }
        }
        return callResult;
    }

    public static Integer[] getAvailable3rdPlayerTypes() {
        Integer[] types = new Integer[AVAILABLE_3RD_PLAYERS.keySet().size()];
        AVAILABLE_3RD_PLAYERS.keySet().toArray(types);
        return types;
    }

    public static Integer[] getAvailableDefaultPlayerTypes() {
        Integer[] types = new Integer[AVAILABLE_DEFAULT_PLAYERS.keySet().size()];
        AVAILABLE_DEFAULT_PLAYERS.keySet().toArray(types);
        return types;
    }

    public static String getDisplaySpeed(long speed) {
        if(speed > 1048576)
            return (speed / 1048576) + "MB/s";
        else if(speed > 1024)
            return (speed / 1024) + "KB/s";
        else
            return speed + "B/s";
    }

    public static String getDeviceTypeName(int type) {
        switch (type) {
            case 0:
                return "电视";
            case 1:
                return "手机";
        }
        return "未定义";
    }
}
