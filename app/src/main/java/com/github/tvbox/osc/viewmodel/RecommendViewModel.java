package com.github.tvbox.osc.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.github.tvbox.osc.bean.AbsJson;
import com.github.tvbox.osc.bean.AbsSortJson;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.util.UA;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.callback.Callback;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.GetRequest;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;

/**
 * @author pj567
 * @date :2020/12/18
 * @description:
 */
public class RecommendViewModel extends SourceViewModel {

    public static void getVodDetail(String doubanId, Callback<String> resultCallback) {
        GetRequest<String> request = OkGo.<String>get("https://movie.douban.com/subject/" + doubanId);
        request.headers("User-Agent", UA.random());
        request.execute(new AbsCallback<String>() {
            @Override
            public void onSuccess(Response<String> response) {
                JSONObject returnedData = new JSONObject();
                try {
                    String pageStr = response.body();
                    //Thread.sleep(200);
                    JXDocument doc = JXDocument.create(pageStr);
                    JXNode description = doc.selNOne("//meta[@property='og:description']");
                    if (description != null)
                        returnedData.put("description", description.asElement().attr("content"));
                    JXNode year = doc.selNOne("//span[@class='year'][1]/text()");
                    if (year != null)
                        returnedData.put("year", year.asString().replace("(", "").replace(")", ""));
                    JXNode country = doc.selNOne("//div[@id='info'][1]/span[@class='pl' and contains(text(), '国家')]");
                    if (country != null)
                        returnedData.put("country", lookupText(pageStr, country.asElement().outerHtml(), "<br").trim());
                    JXNode lang = doc.selNOne("//div[@id='info'][1]/span[@class='pl' and contains(text(), '语言')]");
                    if (lang != null)
                        returnedData.put("language", lookupText(pageStr, lang.asElement().outerHtml(), "<br").trim());
                    JXNode releaseDate = doc.selNOne("//div[@id='info'][1]/span[@property='v:initialReleaseDate']/text()");
                    if (releaseDate != null)
                        returnedData.put("releaseDate", releaseDate.asString());
                    JXNode alias = doc.selNOne("//div[@id='info'][1]/span[@class='pl' and contains(text(), '又名')]");
                    if (alias != null)
                        returnedData.put("alias", lookupText(pageStr, alias.asElement().outerHtml(), "<br").trim());
                    JXNode imdb = doc.selNOne("//div[@id='info'][1]/span[@class='pl' and contains(text(), 'IMDb')]");
                    if (imdb != null)
                        returnedData.put("imdb", lookupText(pageStr, imdb.asElement().outerHtml(), "<br").trim());
                    JXNode totalEpisodes = doc.selNOne("//div[@id='info'][1]/span[@class='pl' and contains(text(), '集数')]");
                    if (totalEpisodes != null)
                        returnedData.put("totalEpisodes", lookupText(pageStr, totalEpisodes.asElement().outerHtml(), "<br").trim());
                    JXNode seoInfo = doc.selNOne("//script[@type='application/ld+json']/text()");
                    try {
                        JsonObject seoInfoJObj = JsonParser.parseString(seoInfo.asString()).getAsJsonObject();
                        if(seoInfoJObj.has("aggregateRating")) {
                            returnedData.put("score", seoInfoJObj.get("aggregateRating").getAsJsonObject().get("ratingValue").getAsString());
                        }
                    }catch(Exception ex) {}
                    List<JXNode> genreList = doc.selN("//div[@id='info'][1]/span[@property='v:genre']");
                    StringBuilder genreStrBuilder = new StringBuilder();
                    for (JXNode genreNode : genreList) {
                        if (genreStrBuilder.length() > 0)
                            genreStrBuilder.append(", ");
                        genreStrBuilder.append(genreNode.selOne("/text()").asString());
                    }
                    returnedData.put("genre", genreStrBuilder.toString());
                    Response<String> resp = new Response<>();
                    resp.setBody(returnedData.toString());
                    if (resultCallback != null)
                        resultCallback.onSuccess(resp);
                } catch (Exception ex) { }
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

    public static void getIMDBScore(String imdbId, Callback<String> resultCallback) {
        GetRequest<String> request = OkGo.<String>get("https://www.imdb.com/title/" + imdbId);
        request.removeHeader("User-Agent");
        request.headers("User-Agent", UA.random());
        request.execute(new AbsCallback<String>() {
            @Override
            public void onSuccess(Response<String> response) {
                JSONObject returnedData = new JSONObject();
                try {
                    String pageStr = response.body();
                    JXDocument doc = JXDocument.create(pageStr);
                    JXNode seoInfo = doc.selNOne("//script[@type='application/ld+json']/text()");
                    try {
                        JsonObject seoInfoJObj = JsonParser.parseString(seoInfo.asString()).getAsJsonObject();
                        if(seoInfoJObj.has("aggregateRating")) {
                            returnedData.put("score", seoInfoJObj.get("aggregateRating").getAsJsonObject().get("ratingValue").getAsString());
                        }
                    }catch(Exception ex) {}
                    Response<String> resp = new Response<>();
                    resp.setBody(returnedData.toString());
                    if (resultCallback != null)
                        resultCallback.onSuccess(resp);
                } catch (Exception ex) { }
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

    private static String lookupText(String text, String startText, String endText) {
        int startIndex = text.indexOf(startText) + startText.length();
        int endIndex = text.indexOf(endText, startIndex);
        return text.substring(startIndex, endIndex);
    }

    public RecommendViewModel() {
        super();
    }

    public void getSort() {
        Runnable waitResponse = new Runnable() {
            @Override
            public void run() {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                String sortJson = "{\"class\":[" +
                        "{\"type_name\":\"全部\",\"type_id\":\"全部\"}," +
                        "{\"type_name\":\"电影\",\"type_id\":\"电影\"}," +
                        "{\"type_name\":\"电视剧\",\"type_id\":\"电视剧\"}," +
                        "{\"type_name\":\"综艺\",\"type_id\":\"综艺\"}," +
                        "{\"type_name\":\"动漫\",\"type_id\":\"动漫\"}," +
                        "{\"type_name\":\"纪录片\",\"type_id\":\"纪录片\"}," +
                        "{\"type_name\":\"短片\",\"type_id\":\"短片\"}]}";
                String optionsStr = "[" +
                        "{\"key\":\"type\",\"name\":\"类型\",\"value\":[{\"n\":\"全部类型\",\"v\":\"\"},{\"n\":\"剧情\",\"v\":\"剧情\"},{\"n\":\"喜剧\",\"v\":\"喜剧\"},{\"n\":\"动作\",\"v\":\"动作\"},{\"n\":\"爱情\",\"v\":\"爱情\"},{\"n\":\"科幻\",\"v\":\"科幻\"},{\"n\":\"动画\",\"v\":\"动画\"},{\"n\":\"灾难\",\"v\":\"灾难\"},{\"n\":\"悬疑\",\"v\":\"悬疑\"},{\"n\":\"惊悚\",\"v\":\"惊悚\"},{\"n\":\"恐怖\",\"v\":\"恐怖\"},{\"n\":\"丧尸\",\"v\":\"丧尸\"},{\"n\":\"犯罪\",\"v\":\"犯罪\"},{\"n\":\"同性\",\"v\":\"同性\"},{\"n\":\"音乐\",\"v\":\"音乐\"},{\"n\":\"歌舞\",\"v\":\"歌舞\"},{\"n\":\"传记\",\"v\":\"传记\"},{\"n\":\"历史\",\"v\":\"历史\"},{\"n\":\"战争\",\"v\":\"战争\"},{\"n\":\"西部\",\"v\":\"西部\"},{\"n\":\"奇幻\",\"v\":\"奇幻\"},{\"n\":\"冒险\",\"v\":\"冒险\"},{\"n\":\"古装\",\"v\":\"古装\"},{\"n\":\"武侠\",\"v\":\"武侠\"},{\"n\":\"情色\",\"v\":\"情色\"}]}," +
                        "{\"key\":\"area\",\"name\":\"地区\",\"value\":[{\"n\":\"全部\",\"v\":\"\"},{\"n\":\"大陆\",\"v\":\"中国大陆\"},{\"n\":\"香港\",\"v\":\"中国香港\"},{\"n\":\"台湾\",\"v\":\"中国台湾\"},{\"n\":\"欧美\",\"v\":\"欧美\"},{\"n\":\"日本\",\"v\":\"日本\"},{\"n\":\"韩国\",\"v\":\"韩国\"},{\"n\":\"英国\",\"v\":\"英国\"},{\"n\":\"法国\",\"v\":\"法国\"},{\"n\":\"德国\",\"v\":\"德国\"},{\"n\":\"印度\",\"v\":\"印度\"},{\"n\":\"泰国\",\"v\":\"泰国\"},{\"n\":\"丹麦\",\"v\":\"丹麦\"},{\"n\":\"瑞典\",\"v\":\"瑞典\"},{\"n\":\"巴西\",\"v\":\"巴西\"},{\"n\":\"加拿大\",\"v\":\"加拿大\"},{\"n\":\"俄罗斯\",\"v\":\"俄罗斯\"},{\"n\":\"意大利\",\"v\":\"意大利\"},{\"n\":\"比利时\",\"v\":\"比利时\"},{\"n\":\"爱尔兰\",\"v\":\"爱尔兰\"},{\"n\":\"西班牙\",\"v\":\"西班牙\"},{\"n\":\"澳大利亚\",\"v\":\"澳大利亚\"},{\"n\":\"伊朗\",\"v\":\"伊朗\"}]}," +
                        "{\"key\":\"feature\",\"name\":\"特色\",\"value\":[{\"n\":\"全部\",\"v\":\"\"},{\"n\":\"经典\",\"v\":\"经典\"},{\"n\":\"青春\",\"v\":\"青春\"},{\"n\":\"文艺\",\"v\":\"文艺\"},{\"n\":\"搞笑\",\"v\":\"搞笑\"},{\"n\":\"励志\",\"v\":\"励志\"},{\"n\":\"魔幻\",\"v\":\"魔幻\"},{\"n\":\"感人\",\"v\":\"感人\"}]}" +
                        "]";
                JsonObject yearFilterOptions = new JsonObject();
                Calendar cal = Calendar.getInstance();
                JsonArray yearOptionArr = new JsonArray();
                int counter = 0;
                while(counter < 10) {
                    Integer year = cal.get(Calendar.YEAR) - counter++;
                    JsonObject optionValueObj = new JsonObject();
                    optionValueObj.addProperty("n", year.toString());
                    optionValueObj.addProperty("v", year.toString() + "," + year.toString());
                    yearOptionArr.add(optionValueObj);
                }
                yearOptionArr.add(JsonParser.parseString("{\"n\":\"2010年代\",\"v\":\"2010,2019\"}"));
                yearOptionArr.add(JsonParser.parseString("{\"n\":\"2000年代\",\"v\":\"2000,2009\"}"));
                yearOptionArr.add(JsonParser.parseString("{\"n\":\"90年代\",\"v\":\"1990,1999\"}"));
                yearOptionArr.add(JsonParser.parseString("{\"n\":\"90年代\",\"v\":\"1990,1999\"}"));
                yearOptionArr.add(JsonParser.parseString("{\"n\":\"80年代\",\"v\":\"1980,1989\"}"));
                yearOptionArr.add(JsonParser.parseString("{\"n\":\"70年代\",\"v\":\"1970,1979\"}"));
                yearOptionArr.add(JsonParser.parseString("{\"n\":\"60年代\",\"v\":\"1960,1969\"}"));
                yearOptionArr.add(JsonParser.parseString("{\"n\":\"更早\",\"v\":\"1,1959\"}"));

                yearFilterOptions.addProperty("key", "year");
                yearFilterOptions.addProperty("name", "年份");
                yearFilterOptions.add("value", yearOptionArr);
                String yearFilterOptionsStr = yearFilterOptions.toString();

                JsonObject configObj = JsonParser.parseString(sortJson).getAsJsonObject();
                JsonArray categoryArr = configObj.get("class").getAsJsonArray();
                JsonObject filterObj = new JsonObject();
                JsonArray filterArr = new JsonArray();
                for (JsonElement categoryObj : categoryArr) {
                    JsonArray filterArray = JsonParser.parseString(optionsStr).getAsJsonArray();
                    filterArray.add(JsonParser.parseString(yearFilterOptionsStr));
                    filterObj.add(categoryObj.getAsJsonObject().get("type_id").getAsString(), filterArray);
                }
                configObj.add("filters", filterObj);
                AbsSortXml sortXml = sortJson(sortResult, configObj.toString());
                sortResult.postValue(sortXml);
            }
        };
        spThreadPool.execute(waitResponse);
    }

    @Override
    public void getList(MovieSort.SortData sortData, int page) {
        spThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    StringBuilder tagsStr = new StringBuilder();
                    if(!sortData.id.equals("全部"))
                        tagsStr.append(sortData.id);
                    String yearRange = null;
                    if(sortData.filterSelect != null && sortData.filterSelect.keySet().size() > 0) {
                        for (String key: sortData.filterSelect.keySet()) {
                            if(tagsStr.length() > 0)
                                tagsStr.append(",");
                            if(key.equals("year"))
                                yearRange = sortData.filterSelect.get(key);
                            else
                                tagsStr.append(sortData.filterSelect.get(key));
                        }
                    }
                    int start = (page - 1) * 20;
                    OkGo.<String>get("https://movie.douban.com/j/new_search_subjects?sort=U&range=0,10&tags="+
                            tagsStr+"&start="+start+ (yearRange != null ? "&year_range=" + yearRange : "")).execute(new AbsCallback<String>() {
                        @Override
                        public void onSuccess(Response<String> response) {
                            String jsonStr = response.body();
                            JsonObject data = JsonParser.parseString(jsonStr).getAsJsonObject();
                            JSONObject json = new JSONObject();
                            try {
                                json.put("page", page);
                                json.put("pagecount", Integer.MAX_VALUE);
                                json.put("limit", 20);
                                json.put("total", Integer.MAX_VALUE);
                                JSONArray vodList = new JSONArray();
                                for (JsonElement vodDataEl : data.getAsJsonArray("data")) {
                                    JsonObject vodData = vodDataEl.getAsJsonObject();
                                    JSONObject convertedData = new JSONObject();
                                    convertedData.put("vod_id", vodData.get("id").getAsString());
                                    convertedData.put("vod_name", vodData.get("title").getAsString());
                                    convertedData.put("vod_pic", vodData.get("cover").getAsString());
                                    convertedData.put("vod_score", vodData.get("rate").getAsString());
                                    if(vodData.get("directors") != null) {
                                        StringBuilder directorSB = new StringBuilder();
                                        for (JsonElement directorEl : vodData.get("directors").getAsJsonArray()) {
                                            if(directorSB.length() > 0)
                                                directorSB.append(", ");
                                            directorSB.append(directorEl.getAsString());
                                        }
                                        convertedData.put("vod_director", directorSB.toString());
                                    }
                                    if(vodData.get("casts") != null) {
                                        StringBuilder castSB = new StringBuilder();
                                        for (JsonElement castEl : vodData.get("casts").getAsJsonArray()) {
                                            if(castSB.length() > 0)
                                                castSB.append(", ");
                                            castSB.append(castEl.getAsString());
                                        }
                                        convertedData.put("vod_actor", castSB.toString());
                                    }
                                    vodList.put(convertedData);
                                }
                                json.put("list", vodList);
                            }catch (Exception ex) { }
                            json(listResult, json.toString());
                        }

                        @Override
                        public String convertResponse(okhttp3.Response response) throws Throwable {
                            return response.body().string();
                        }
                    });
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        });
    }

    private MovieSort.SortFilter getSortFilter(JsonObject obj) {
        String key = obj.get("key").getAsString();
        String name = obj.get("name").getAsString();
        JsonArray kv = obj.getAsJsonArray("value");
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        for (JsonElement ele : kv) {
            values.put(ele.getAsJsonObject().get("n").getAsString(), ele.getAsJsonObject().get("v").getAsString());
        }
        MovieSort.SortFilter filter = new MovieSort.SortFilter();
        filter.key = key;
        filter.name = name;
        filter.setValues(values);
        return filter;
    }

    private AbsSortXml sortJson(MutableLiveData<AbsSortXml> result, String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            AbsSortJson sortJson = new Gson().fromJson(obj, new TypeToken<AbsSortJson>() {
            }.getType());
            AbsSortXml data = sortJson.toAbsSortXml(null);
            try {
                if (obj.has("filters")) {
                    LinkedHashMap<String, ArrayList<MovieSort.SortFilter>> sortFilters = new LinkedHashMap<>();
                    JsonObject filters = obj.getAsJsonObject("filters");
                    for (String key : filters.keySet()) {
                        ArrayList<MovieSort.SortFilter> sortFilter = new ArrayList<>();
                        JsonElement one = filters.get(key);
                        if (one.isJsonObject()) {
                            sortFilter.add(getSortFilter(one.getAsJsonObject()));
                        } else {
                            for (JsonElement ele : one.getAsJsonArray()) {
                                sortFilter.add(getSortFilter(ele.getAsJsonObject()));
                            }
                        }
                        sortFilters.put(key, sortFilter);
                    }
                    for (MovieSort.SortData sort : data.classes.sortList) {
                        if (sortFilters.containsKey(sort.id) && sortFilters.get(sort.id) != null) {
                            sort.filters = sortFilters.get(sort.id);
                        }
                    }
                }
            } catch (Exception e) { }
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    private void absXml(AbsXml data) {
        if (data.movie != null && data.movie.videoList != null) {
            for (Movie.Video video : data.movie.videoList) {
                if (video.urlBean != null && video.urlBean.infoList != null) {
                    for (Movie.Video.UrlBean.UrlInfo urlInfo : video.urlBean.infoList) {
                        String[] str = null;
                        if (urlInfo.urls.contains("#")) {
                            str = urlInfo.urls.split("#");
                        } else {
                            str = new String[]{urlInfo.urls};
                        }
                        List<Movie.Video.UrlBean.UrlInfo.InfoBean> infoBeanList = new ArrayList<>();
                        for (String s : str) {
                            if (s.contains("$")) {
                                String[] ss = s.split("\\$");
                                if (ss.length >= 2) {
                                    infoBeanList.add(new Movie.Video.UrlBean.UrlInfo.InfoBean(ss[0], ss[1]));
                                }
                                //infoBeanList.add(new Movie.Video.UrlBean.UrlInfo.InfoBean(s.substring(0, s.indexOf("$")), s.substring(s.indexOf("$") + 1)));
                            }
                        }
                        urlInfo.beanList = infoBeanList;
                    }
                }
            }
        }
    }

    private AbsXml json(MutableLiveData<AbsXml> result, String json) {
        try {
            AbsJson absJson = new Gson().fromJson(json, new TypeToken<AbsJson>() {
            }.getType());
            AbsXml data = absJson.toAbsXml();
            absXml(data);
            if (searchResult == result) {
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_SEARCH_RESULT, data));
            } else if (quickSearchResult == result) {
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_RESULT, data));
            } else if (result != null) {
                result.postValue(data);
            }
            return data;
        } catch (Exception e) {
            if (searchResult == result) {
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_SEARCH_RESULT, null));
            } else if (quickSearchResult == result) {
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_RESULT, null));
            } else if (result != null) {
                result.postValue(null);
            }
            return null;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}