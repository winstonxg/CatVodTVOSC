package com.github.tvbox.osc.ui.adapter;

import android.text.Html;
import android.text.TextUtils;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.picasso.RoundTransformation;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.viewmodel.RecommendViewModel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class RecommendGridAdapter extends BaseQuickAdapter<Movie.Video, BaseViewHolder> {

    public RecommendGridAdapter() {
        super(R.layout.item_grid_recommend, new ArrayList<>());
    }

    private void setDisplayDetail(BaseViewHolder helper, Movie.Video item) {
        String tvName = item.name + "(" + item.year + ")";
        if(item.totalEpisodes != null)
            tvName += ", 共" + item.totalEpisodes + "集";
        if(item.score != null && item.score.length() > 0)
            helper.setText(R.id.doubanScore, item.score);
        if(item.scoreImdb != null && item.scoreImdb.length() > 0)
            helper.setText(R.id.imdbScore, item.scoreImdb);
        helper.setText(R.id.tvName, tvName);
        setTextShow(helper, R.id.tvAlias, "别名：", item.alias);
        setTextShow(helper, R.id.tvArea, "国家：", item.area);
        setTextShow(helper, R.id.tvLang, "语言：", item.lang);
        setTextShow(helper, R.id.tvGenre, "标签：", item.type);
        setTextShow(helper, R.id.tvReleaseDate, "首播：", item.releaseDate);
        setTextShow(helper, R.id.tvDes, "简介：", item.des);
    }

    private void setTextShow(BaseViewHolder helper, int viewId, String tag, String info) {
        if (info == null || info.trim().isEmpty()) {
            helper.setGone(viewId, false);
            return;
        }
        helper.setGone(viewId, true);
        helper.setText(viewId, Html.fromHtml(getHtml(tag, info)));
    }

    private String getHtml(String label, String content) {
        if (content == null) {
            content = "";
        }
        return label + "<font color=\"#FFFFFF\">" + content + "</font>";
    }

    @Override
    protected void convert(BaseViewHolder helper, Movie.Video item) {
        helper.setText(R.id.tvName, item.name);
        setTextShow(helper, R.id.tvActor, "演员：", item.actor);
        setTextShow(helper, R.id.tvDirector, "导演：", item.director);
        ImageView ivThumb = helper.getView(R.id.ivThumb);
        if(item.isDetailLoaded) {
            setDisplayDetail(helper, item);
        } else {
            RecommendViewModel.getVodDetail(item.id, new AbsCallback<String>() {
                  @Override
                  public void onSuccess(Response<String> response) {
                      String jsonStr = response.body();
                      JsonObject data = JsonParser.parseString(jsonStr).getAsJsonObject();
                      try {
                          try {
                              item.year = Integer.parseInt(data.get("year").getAsString());
                          } catch (Exception ex) {
                          }
                          if (item.year == 0 && item.retrieveDetailTried < 3) {
                              Thread.sleep(200);
                              item.retrieveDetailTried += 1;
                              convert(helper, item);
                          }
                          if (data.has("totalEpisodes"))
                              item.totalEpisodes = data.get("totalEpisodes").getAsString();
                          if (data.has("alias"))
                              item.alias = data.get("alias").getAsString();
                          if (data.has("country"))
                              item.area = data.get("country").getAsString();
                          if (data.has("language"))
                              item.lang = data.get("language").getAsString();
                          if (data.has("genre"))
                              item.type = data.get("genre").getAsString();
                          if (data.has("releaseDate"))
                              item.releaseDate = data.get("releaseDate").getAsString();
                          if (data.has("description"))
                              item.des = data.get("description").getAsString();
                          item.isDetailLoaded = true;
                          if (data.has("imdb") && item.scoreImdb == null) {
                              RecommendViewModel.getIMDBScore(data.get("imdb").getAsString(), new AbsCallback<String>() {
                                  @Override
                                  public void onSuccess(Response<String> response) {
                                      JsonObject imdbData = JsonParser.parseString(response.body()).getAsJsonObject();
                                      if (imdbData.has("score")) {
                                          item.scoreImdb = imdbData.get("score").getAsString();
                                          setDisplayDetail(helper, item);
                                      }
                                  }

                                  @Override
                                  public String convertResponse(okhttp3.Response response) throws Throwable {
                                      return response.body().string();
                                  }
                              });
                          }
                          setDisplayDetail(helper, item);
                      } catch (Exception ex) {
                      }
                  }

                  @Override
                  public String convertResponse(okhttp3.Response response) throws Throwable {
                      return response.body().string();
                  }
            });
        }
        //由于部分电视机使用glide报错
        if (!TextUtils.isEmpty(item.pic)) {
            ivThumb.post(new Runnable() {
                @Override
                public void run() {
                    ivThumb.getLayoutParams().width = (int)(ivThumb.getHeight() * 0.693617);
                    ivThumb.requestLayout();
                    Picasso.get()
                            .load(DefaultConfig.checkReplaceProxy(item.pic))
                            .transform(new RoundTransformation(MD5.string2MD5(item.pic + "position=" + helper.getLayoutPosition()))
                                    .centerCorp(true)
                                    .override(ivThumb.getWidth(), ivThumb.getHeight())
                                    .roundRadius(AutoSizeUtils.mm2px(mContext, 10), RoundTransformation.RoundType.LEFT))
                            .placeholder(R.drawable.img_loading_placeholder)
                            .error(R.drawable.img_loading_placeholder)
                            .into(ivThumb);
                }
            });
        } else {
            ivThumb.setImageResource(R.drawable.img_loading_placeholder);
        }
    }
}