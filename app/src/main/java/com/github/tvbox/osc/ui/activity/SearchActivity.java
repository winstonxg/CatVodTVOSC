package com.github.tvbox.osc.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.event.ServerEvent;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.adapter.PinyinAdapter;
import com.github.tvbox.osc.ui.adapter.SearchAdapter;
import com.github.tvbox.osc.ui.dialog.RemoteDialog;
import com.github.tvbox.osc.ui.tv.QRCodeGen;
import com.github.tvbox.osc.ui.tv.widget.SearchKeyboard;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.VodSearch;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class SearchActivity extends BaseActivity {
    private LinearLayout llLayout;
    private TvRecyclerView mGridView;
    private TvRecyclerView mGridViewWord;
    SourceViewModel sourceViewModel;
    private EditText etSearch;
    private TextView tvSearch;
    private TextView tvClear;
    private SearchKeyboard keyboard;
    private TextView tvAddress;
    private ImageView ivQRCode;
    private SearchAdapter searchAdapter;
    private PinyinAdapter wordAdapter;
    private String searchTitle = "";
    private View footLoading = null;
    private VodSearch vodSearch;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_search;
    }

    @Override
    protected void init() {
        initView();
        initViewModel();
        initData();
    }

    private List<Runnable> pauseRunnable = null;

    @Override
    protected void onResume() {
        super.onResume();
        if (vodSearch != null && pauseRunnable != null && pauseRunnable.size() > 0) {
            vodSearch.StartNewSearchService();
            vodSearch.getAllRunCount().set(pauseRunnable.size());
            for (Runnable runnable : pauseRunnable) {
                vodSearch.getSearchExecutorService().execute(runnable);
            }
            pauseRunnable.clear();
            pauseRunnable = null;
        }
    }

    private void initView() {
        EventBus.getDefault().register(this);
        llLayout = findViewById(R.id.llLayout);
        etSearch = findViewById(R.id.etSearch);
        tvSearch = findViewById(R.id.tvSearch);
        tvClear = findViewById(R.id.tvClear);
        tvAddress = findViewById(R.id.tvAddress);
        ivQRCode = findViewById(R.id.ivQRCode);
        mGridView = findViewById(R.id.mGridView);
        keyboard = findViewById(R.id.keyBoardRoot);
        mGridViewWord = findViewById(R.id.mGridViewWord);
        mGridViewWord.setHasFixedSize(true);
        mGridViewWord.setLayoutManager(new V7LinearLayoutManager(this.mContext, 1, false));
        wordAdapter = new PinyinAdapter();
        mGridViewWord.setAdapter(wordAdapter);
        footLoading = getLayoutInflater().inflate(R.layout.item_search_lite, null);
        footLoading.findViewById(R.id.tvName).setVisibility(View.GONE);
        footLoading.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        wordAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                search(wordAdapter.getItem(position));
            }
        });
        mGridView.setHasFixedSize(true);
        // lite
        if (Hawk.get(HawkConfig.SEARCH_VIEW, 0) == 0)
            mGridView.setLayoutManager(new V7LinearLayoutManager(this.mContext, 1, false));
            // with preview
        else
            mGridView.setLayoutManager(new V7GridLayoutManager(this.mContext, 3));
        searchAdapter = new SearchAdapter();
        mGridView.setAdapter(searchAdapter);
        searchAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                Movie.Video video = searchAdapter.getData().get(position);
                if (video != null) {
                    try {
                        if (vodSearch.getSearchExecutorService() != null) {
                            pauseRunnable = vodSearch.getSearchExecutorService().shutdownNow();
                        }
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString("id", video.id);
                    bundle.putString("sourceKey", video.sourceKey);
                    jumpActivity(DetailActivity.class, bundle);
                }
            }
        });
        tvSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                String wd = etSearch.getText().toString().trim();
                if (!TextUtils.isEmpty(wd)) {
                    search(wd);
                } else {
                    Toast.makeText(mContext, "输入内容不能为空", Toast.LENGTH_SHORT).show();
                }
            }
        });
        tvClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                etSearch.setText("");
            }
        });
        keyboard.setOnSearchKeyListener(new SearchKeyboard.OnSearchKeyListener() {
            @Override
            public void onSearchKey(int pos, String key) {
                if (pos > 0) {
                    String text = etSearch.getText().toString().trim();
                    text += key;
                    etSearch.setText(text);
                } else if (pos == 0) {
                    String text = etSearch.getText().toString().trim();
                    if (text.length() > 0) {
                        text = text.substring(0, text.length() - 1);
                        etSearch.setText(text);
                    }
                }
                if(etSearch.getText().length() <= 0) {
                    loadHotSearch();
                } else {
                    loadRec(etSearch.getText().toString());
                }
            }
        });
        setLoadSir(llLayout);
    }

    private void initViewModel() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        vodSearch = new VodSearch(sourceViewModel);
    }

    /**
     * 拼音联想
     */
    private void loadRec(String key) {
        wordAdapter.setNewData(new ArrayList<>());
        OkGo.<String>get("https://suggest.video.iqiyi.com/")
                .params("if", "mobile")
                .params("key", key)
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            ArrayList<String> hots = new ArrayList<>();
                            String result = response.body();
                            JsonObject json = JsonParser.parseString(result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1)).getAsJsonObject();
                            JsonArray itemList = json.get("data").getAsJsonArray();
                            for (JsonElement ele : itemList) {
                                JsonObject obj = (JsonObject) ele;
                                hots.add(obj.get("name").getAsString().trim());
                            }
                            wordAdapter.setNewData(hots);
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return response.body().string();
                    }
                });
    }

    private void initData() {
        if(Hawk.get(HawkConfig.REMOTE_CONTROL, true))
            refreshQRCode();
        else
            findViewById(R.id.remoteRoot).setVisibility(View.GONE);
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("title")) {
            String title = intent.getStringExtra("title");
            showLoading();
            search(title);
        }
        // 加载热词
        loadHotSearch();
    }

    //load hot search
    private void loadHotSearch() {
        OkGo.<String>get("https://node.video.qq.com/x/api/hot_search")
                .params("channdlId", "0")
                .params("_", System.currentTimeMillis())
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            ArrayList<String> hots = new ArrayList<>();
                            JsonObject mapResult = JsonParser.parseString(response.body())
                                    .getAsJsonObject()
                                    .get("data").getAsJsonObject()
                                    .get("mapResult").getAsJsonObject();
                            List<String> groupIndex = Arrays.asList("0", "1", "2", "3", "5");
                            for(String index : groupIndex) {
                                JsonArray itemList = mapResult.get(index).getAsJsonObject()
                                        .get("listInfo").getAsJsonArray();
                                for (JsonElement ele : itemList) {
                                    JsonObject obj = (JsonObject) ele;
                                    String hotKey = obj.get("title").getAsString().trim().replaceAll("<|>|《|》|-", "").split(" ")[0];
                                    if(!hots.contains(hotKey))
                                        hots.add(hotKey);
                                }
                            }

                            wordAdapter.setNewData(hots);
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return response.body().string();
                    }
                });
    }

    private void refreshQRCode() {
        String address = ControlManager.get().getAddress(false);
        tvAddress.setText(String.format("远程搜索使用手机/电脑扫描下面二维码或者直接浏览器访问地址\n%s", address));
        ivQRCode.setImageBitmap(QRCodeGen.generateBitmap(address, 300, 300));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void server(ServerEvent event) {
        if (event.type == ServerEvent.SERVER_SEARCH) {
            String title = (String) event.obj;
            showLoading();
            search(title);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_SEARCH_RESULT) {
            try {
                searchData(event.obj == null ? null : (AbsXml) event.obj);
            } catch (Exception e) {
                searchData(null);
            }
        }
    }

    private void search(String title) {
        cancel();
        showLoading();
        JsonObject startJson = new JsonObject();
        startJson.addProperty("type", "search");
        startJson.addProperty("action", "start");
        ControlManager.get().getSocketServer().sendToAll(startJson);
        this.searchTitle = title;
        mGridView.setVisibility(View.INVISIBLE);
        searchAdapter.setNewData(new ArrayList<>());
        vodSearch.searchResult(searchTitle, false);
    }

    private void searchData(AbsXml absXml) {
        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
            List<Movie.Video> data = new ArrayList<>();
            for (Movie.Video video : absXml.movie.videoList) {
                if (video.name.contains(searchTitle))
                    data.add(video);
            }
            JsonObject endJson = new JsonObject();
            endJson.addProperty("type", "search");
            endJson.add("results", JsonParser.parseString((new Gson()).toJson(data)));
            ControlManager.get().getSocketServer().sendToAll(endJson);
            if (searchAdapter.getData().size() > 0) {
                searchAdapter.addData(data);
            } else {
                showSuccess();
                mGridView.setVisibility(View.VISIBLE);
                searchAdapter.setNewData(data);
                searchAdapter.addFooterView(footLoading);
            }
        }

        int count = vodSearch.getAllRunCount().decrementAndGet();
        if (count <= 0) {
            if (searchAdapter.getData().size() <= 0) {
                showEmpty();
            }
            searchAdapter.removeFooterView(footLoading);
            cancel();
        }
    }

    private void cancel() {
        JsonObject endJson = new JsonObject();
        endJson.addProperty("type", "search");
        endJson.addProperty("action", "end");
        ControlManager.get().getSocketServer().sendToAll(endJson);
        OkGo.getInstance().cancelTag("search");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancel();
        vodSearch.destroy();
        EventBus.getDefault().unregister(this);
    }
}