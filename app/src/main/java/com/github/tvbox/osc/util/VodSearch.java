package com.github.tvbox.osc.util;

import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.viewmodel.SourceViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class VodSearch {
    private ExecutorService searchExecutorService = null;
    private AtomicInteger allRunCount = new AtomicInteger(0);
    private SourceViewModel sourceViewModel;

    public VodSearch(SourceViewModel sourceViewModel) {
        this.sourceViewModel = sourceViewModel;
    }

    public ExecutorService getSearchExecutorService() {
        return searchExecutorService;
    }

    public AtomicInteger getAllRunCount() {
        return allRunCount;
    }

    public void StartNewSearchService() {
        if (searchExecutorService != null) {
            try {
                searchExecutorService.shutdownNow();
            }catch (Exception ex) {

            }
            searchExecutorService = null;
        }
        searchExecutorService = Executors.newFixedThreadPool(5);
    }

    public void searchResult(String searchTitle, boolean shouldBackend) {
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        } finally {
            allRunCount.set(0);
        }

        searchExecutorService = Executors.newFixedThreadPool(5);
        List<SourceBean> searchRequestList = new ArrayList<>();
        searchRequestList.addAll(ApiConfig.get().getSourceBeanList());
        SourceBean home = ApiConfig.get().getHomeSourceBean();
        searchRequestList.remove(home);
        searchRequestList.add(0, home);

        ArrayList<String> siteKey = new ArrayList<>();
        for (SourceBean bean : searchRequestList) {
            if (!bean.isSearchable()) {
                continue;
            }
            siteKey.add(bean.getKey());
            allRunCount.incrementAndGet();
        }
        for (String key : siteKey) {
            searchExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    if(shouldBackend)
                        sourceViewModel.getBackQuickSearch(key, searchTitle);
                    else
                        sourceViewModel.getSearch(key, searchTitle);
                }
            });
        }
    }

    public void destroy() {
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
