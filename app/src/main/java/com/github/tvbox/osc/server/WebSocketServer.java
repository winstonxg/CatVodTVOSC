package com.github.tvbox.osc.server;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.ui.activity.DetailActivity;
import com.github.tvbox.osc.ui.activity.PlayActivity;
import com.github.tvbox.osc.ui.fragment.PlayerFragment;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.VodSearch;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.orhanobut.hawk.Hawk;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;
import xyz.doikki.videoplayer.player.VideoView;

/**
 * @author Paul S. Hawke (paul.hawke@gmail.com) On: 4/23/14 at 10:31 PM
 */
public class WebSocketServer extends NanoWSD {

    /**
     * logger to log to.
     */
    private static final Logger LOG = Logger.getLogger(WebSocketServer.class.getName());
    private static List<NoticeWebSocket> connected = new ArrayList<>();
    private Context mContext;
    private Handler mHandler;

    public static int serverPort = 9878;

    public Handler getHandler() {
        return mHandler;
    }

    public WebSocketServer(int port, Context mContext) {
        super(port);
        this.mContext = mContext;
        mHandler = new Handler(this.mContext.getMainLooper());
    }

    public void sendToAll(JsonObject object) {
        if(!Hawk.get(HawkConfig.REMOTE_CONTROL, true))
            return;
        String sentData = object.toString();
        new Thread() {
            @Override
            public void run() {
                synchronized (connected) {
                    for (WebSocket socket : connected) {
                        try {
                            socket.send(sentData);
                        } catch (Exception ex) {
                            try {
                                socket.close(CloseCode.InvalidFramePayloadData, "error", false);
                            } catch (Exception ex2) {
                            }
                        }
                    }
                }
            }
        }.start();
    }

    @Override
    public void stop() {
        super.stop();
        synchronized (WebSocketServer.connected) {
            for(WebSocket socket:connected) {
                try {
                    socket.close(CloseCode.GoingAway, "close", false);
                } catch (IOException e) {

                }
            }
            connected.clear();
        }
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        NoticeWebSocket newSocket = new NoticeWebSocket(this, handshake);
        return newSocket;
    }

    public static class NoticeWebSocket extends WebSocket {

        private static final byte[] PING_PAYLOAD = "1337DEADBEEFC001".getBytes();

        private final WebSocketServer server;
        private TimerTask ping = null;

        public NoticeWebSocket(WebSocketServer server, IHTTPSession handshakeRequest) {
            super(handshakeRequest);
            this.server = server;
        }

        @Override
        protected void onOpen() {
            synchronized (WebSocketServer.connected) {
                WebSocketServer.connected.add(this);
            }
            if (ping == null) {
                ping = new TimerTask() {
                    @Override
                    public void run(){
                        try { ping(PING_PAYLOAD); }
                        catch (IOException e) { ping.cancel(); }
                    }
                };
                new Timer().schedule(ping, 1000, 2000);
            }
        }

        @Override
        protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
            synchronized (WebSocketServer.connected) {
                WebSocketServer.connected.remove(this);
            }
            try {
                if (ping != null) ping.cancel();
            }catch (Exception ex) {}
        }

        @Override
        protected void onMessage(WebSocketFrame message) {
            try {
                message.setUnmasked();
                JsonObject backData = JsonParser.parseString(message.getTextPayload()).getAsJsonObject();
                String type = backData.get("type").getAsString();
                if(type != null) {
                    String[] classAndMethod = type.split("\\-");
                    if(classAndMethod.length == 2) {
                        try {
                            Class processor = Class.forName("com.github.tvbox.osc.server.socketprocessors." + classAndMethod[0] + "Processor");
                            if(processor != null) {
                                java.lang.reflect.Method matchedMethod = null;
                                for (java.lang.reflect.Method method : processor.getMethods()) {
                                    if(method.getName().equalsIgnoreCase(classAndMethod[1])) {
                                        matchedMethod = method;
                                        break;
                                    }
                                }
                                if(matchedMethod != null) {
                                    Class<?>[] paramTypes = matchedMethod.getParameterTypes();
                                    List<Object> params = new ArrayList<>();
                                    for(Class paramType : paramTypes) {
                                        if(paramType == JsonObject.class)
                                            params.add(backData);
                                        else if(paramType == NoticeWebSocket.class)
                                            params.add(this);
                                        else
                                            params.add(null);
                                    }
                                    matchedMethod.invoke(processor.newInstance(), params.toArray());
                                }
                            }
                        }catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                sendFrame(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onPong(WebSocketFrame pong) {

        }

        @Override
        protected void onException(IOException exception) {
            WebSocketServer.LOG.log(Level.SEVERE, "exception occured", exception);
        }

        @Override
        protected void debugFrameReceived(WebSocketFrame frame) {

        }

        @Override
        protected void debugFrameSent(WebSocketFrame frame) {

        }
    }

    private class VodProcessor {


    }
}
