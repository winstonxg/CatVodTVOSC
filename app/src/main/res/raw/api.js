var playerState = {
    STATE_ERROR : [-1, "播放失败❌"],
    STATE_IDLE : [0, "播放器闲置中💤"],
    STATE_PREPARING : [1, "视频加载中🌀"],
    STATE_PREPARED : [2, "视频加载完成👍"],
    STATE_PLAYING : [3, "正在播放"],
    STATE_PAUSED : [4, "暂停"],
}

var apiList = [];
var vodSpeedOptions = [.5, .75, 1, 1.25, 1.5, 1.75, 2, 2.25, 2.5, 2.75, 3];
var vodScaleOptions = ["默认", "16:9", "4:3", "填充", "原始", "裁剪"];
var vodPlayerOptions = ["系统播放器", "IJK播放器", "Exo播放器"];
var ijkOptions = [];
var cachedPlayerCfg = "";
var channelGroups = [];

$(function(){
    vodSpeedOptions.forEach(val => {
        $(".vodSpeed").append(`<option value="${val}">x${val}</option>`);
    });
    vodScaleOptions.forEach((val, index) => {
        $(".vodScale").append(`<option value="${index}">${val}</option>`);
    });
    vodPlayerOptions.forEach((val, index) => {
        $(".vodPlayer").append(`<option value="${index}">${val}</option>`);
    });
});

function lookUpPlayerState(state) {
    for(var entry of Object.entries(playerState)) {
        if(entry[1][0] == state)
            return entry[1];
    }
}

var currentVodData;
var parserFlags;
var parsers;

function initCurrentVodData() {
    currentVodData = {
        sourceKey: null,
        category: {},
        page: 1,
        isRequesting: false,
        hasMore: true,
        filter: "",
    };
}

function updateApiList(cb) {
    $('#loadingToast').fadeIn();
    $.get('/api?type=api-list', function (data) {
        $('#loadingToast').fadeOut();
        var select = $("#vodSelect");
        select.empty();
        try {
            if(data && data.sources.length > 0) {
                apiList = data.sources;
                data.sources.forEach(element => {
                    var selected = element.key == data.homeKey ? "selected" : "";
                    var option = $(`<option value="${element.key}" ${selected}>${element.name}</option>`);
                    select.append(option);
                });
            }
        }catch(err) {}
        if(cb) cb(data);
        if(data && data.sources.length > 0) {
            getHomeData(data.homeKey);
        }
    }).fail(() => {
        window.setTimeout(() => updateApiList(), 5000);
    });
    
}

function updateChannelGroup() {
    $.get('/api?type=live', function (data) {
        channelGroups = data;
        var liveGroupSelect = $("#liveGroupSelect");
        liveGroupSelect.empty();
        channelGroups.forEach((el, index) => {
            var option = $(`<option value="${index}">${el.groupName}</option>`);
            liveGroupSelect.append(option);
        });
        $.get('/api?type=live-last-channel', function (data) {
            if(data) {
                channelGroups.forEach((el, index) => {
                    var foundChannelIndex = el.liveChannelItems.findIndex(x => x.channelName == data);
                    if(foundChannelIndex >= 0) {
                        liveGroupSelect.val(index);
                        updateChannelList(index);
                        $("#channelSelect").val(foundChannelIndex);
                    }
                });
            } else {
                updateChannelList(0);
            }
        }).fail((a, b, c) => {
            updateChannelList(0);
        });;
    }).fail(() => {
        window.setTimeout(() => updateChannelGroup(), 5000);
    });
}

function updateChannelList(channelGroupIndex) {
    var channelSelect = $("#channelSelect");
    channelSelect.empty();
    channelGroups[channelGroupIndex].liveChannelItems.forEach((el, index) => {
        var option = $(`<option value="${index}">${el.channelName}</option>`);
        channelSelect.append(option);
    });
}

function initParserData() {
    $.get('/api?type=parser-flags', function(data){
        parserFlags = data;
    });
    $.get('/api?type=parsers', function(data){
        parsers = data;
        var parserBox = $(".parsers");
        parserBox.empty();
        parsers.parsers.forEach(el => {
            var parserEl = $(`<div>${el.name}</div>`);
            if(el.name == parsers.selected)
                parserEl.addClass("selected");
            parserBox.append(parserEl);
        });
    });
}

function getHomeData(sourceKey, cb) {
    initCurrentVodData();
    currentVodData.sourceKey = sourceKey;
    $.get(`/api?type=category-list&sourceKey=${sourceKey}`, function (data) {
        currentVodData.category = data;
        var select = $("#categorySelect");
        select.empty();
        var homeOption = $(`<option value="_home" selected>首页</option>`);
        select.append(homeOption);
        try {
            data.classes.sortList.forEach(element => {
                var option = $(`<option value="${element.id}">${element.name}</option>`);
                select.append(option);
            });
        }catch(err) {}
        getVodData("_home", "", true);
        if(cb) cb();
    }).fail(() => {
        window.setTimeout(() => updateApiList(), 5000);
    });
}

function getVodData(categoryId, filters, reload, cb) {
    var vodList = $("#vodList");
    if(reload) {
        vodList.empty();
        currentVodData.page = 1;
        currentVodData.hasMore = true;
        if(filters) {
            var base64Filter = btoa(unescape(encodeURIComponent(filters)));
            currentVodData.filter = base64Filter;
        } else {
            if(currentVodData.filter == filters)
                updateFilter(categoryId);
            currentVodData.filter = "";
        }
    }
    if(currentVodData.isRequesting || !currentVodData.hasMore) {
        if(cb) cb(false);
        return;
    }
    currentVodData.isRequesting = true;
    var temp = `<a href="javascript:" class="weui-grid vodItem" role="button">
        <div class="vodImg"></div>
        <p class="weui-grid__label"></p>
        </a>`;
    $("#vodLoadMore").fadeIn();
    if(categoryId == "_home") {
        try {
            currentVodData.category.list.videoList.forEach(element => {
                var vodItem = $(temp);
                vodItem.find(".vodImg").css("background-image", `url(${element.pic})`);
                vodItem.find(".weui-grid__label").text(element.name);
                vodItem.data("voddata", element);
                vodList.append(vodItem);
            });
        }catch(err) {}
        $("#vodLoadMore").fadeOut();
        $("#vodNoMore").fadeIn();
        currentVodData.hasMore = false;
        currentVodData.isRequesting = false;
        if(cb) cb(true);
    } else {
        if(!currentVodData.hasMore)
            return;
        $.get(`/api?type=category-content&sourceKey=${currentVodData.sourceKey}&id=${categoryId}&page=${currentVodData.page}&filters=${currentVodData.filter}`, 
        function (data) {
            try {
                if(data.movie.videoList.length == 0) {
                    currentVodData.hasMore = false;
                    $("#vodLoadMore").fadeOut();
                    $("#vodNoMore").fadeIn();
                    return;
                }
                data.movie.videoList.forEach(element => {
                    var vodItem = $(temp);
                    vodItem.find(".vodImg").css("background-image", `url(${element.pic})`);
                    vodItem.find(".weui-grid__label").text(element.name);
                    vodItem.data("voddata", element);
                    vodList.append(vodItem);
                });
            }catch(err) {}
            currentVodData.isRequesting = false;
            if(cb) cb(true);
        }).fail(() => {
            window.setTimeout(() => updateApiList(), 5000);
        });
        currentVodData.page++;
    }
}

function updateFilter(categoryId) {
    var filterPane = $("#vodFilter .weui-cells.vodFilterCells");
    filterPane.empty();
    var selectedCategory = currentVodData.category.classes.sortList.find(x => x.id == categoryId);
    if(!selectedCategory) {
        $("#btnVodFilterOpen").fadeOut();
        return;
    }
    var filters = selectedCategory.filters;
    if(filters.length == 0) {
        $("#btnVodFilterOpen").fadeOut();
        return;
    }
    $("#btnVodFilterOpen").fadeIn();
    var filterSeletorTemp = `<label class="weui-cell weui-cell_active weui-cell_select weui-cell_select-after">
        <div class="weui-cell__hd">
            <span class="weui-label"></span>
        </div>
        <div class="weui-cell__bd">
            <select class="weui-select vod-filter-select">
            </select>
        </div>
    </label>`;
    filters.forEach(element => {
        var filterSelector = $(filterSeletorTemp);
        filterPane.append(filterSelector);
        filterSelector.find(".weui-label").text(element.name);
        var seletor = filterSelector.find(".weui-select");
        seletor.data("key", element.key);
        element.options.forEach(optionData => {
            var option = $(`<option value="${optionData.value}">${optionData.key}</option>`);
            seletor.append(option);
        });
    });
}

function playVod(sourceKey, vodId, name) {
    var settings = {
        url: "/api?type=play",
        method: "POST",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify({
          "id": vodId,
          "sourceKey": sourceKey
        })
    };
      
    $.ajax(settings).done(function(data) {
        if(data.succeeded) {
            $("#successToastContent").text(`已请求TV猫盒播放 ${name}`);
            $("#successToast").fadeIn();
            window.setTimeout(() => $("#successToast").fadeOut(), 2000);
        } else {
            $("#warnToastContent").text(`TV猫盒无法识别该请求`);
            $("#warnToast").fadeIn();
            window.setTimeout(() => $("#warnToast").fadeOut(), 2000);
        }
    }).fail(function(jqXHR, textStatus, errorThrown) {
        $("#warnToastContent").text(`TV猫盒无法被访问`);
        $("#warnToast").fadeIn();
        window.setTimeout(() => $("#warnToast").fadeOut(), 2000);
    });
}

function getPlaying(cb) {
    $.get(`/api?type=playing-info`, 
        function (data) {
            var playing = $(".player");
            if(cachedPlayerCfg) {
                data.playerCfg = cachedPlayerCfg;
                cachedPlayerCfg = null;
            }
            playing.data("info", data);
            playing.find(".img").css("background-image", `url(${data.pic})`);
            if(data.actor)
                playing.find(".detail .actor").css("display", "block").find(".value").text(data.actor);
            else
                playing.find(".detail .actor").css("display", "none");
            if(data.area)
                playing.find(".detail .area").css("display", "block").find(".value").text(data.area);
            else
                playing.find(".detail .area").css("display", "none");
            if(data.year)
                playing.find(".detail .year").css("display", "block").find(".value").text(data.year);
            else
                playing.find(".detail .year").css("display", "none");
            if(data.type)
                playing.find(".detail .type").css("display", "block").find(".value").text(data.type);
            else
                playing.find(".detail .type").css("display", "none");
            if(data.des)
                playing.find(".detail .desc").css("display", "block").find(".value").text(data.des);
            else
                playing.find(".detail .desc").css("display", "none");
            var seriesFlagBox = $(".seriesFlags");
            seriesFlagBox.empty();
            data.seriesFlags.forEach(el => {
                var flag = $(`<div>${el.name}</div>`);
                flag.data("info", el);
                if(el.name == data.playFlag) {
                    flag.addClass("selected");
                    updateSeries(el.name);
                }
                seriesFlagBox.append(flag);
            });
            if(data.ijkCodes) {
                ijkOptions = data.ijkCodes;
                $(".vodIjkCodes").empty();
                ijkOptions.forEach(code => {
                    $(".vodIjkCodes").append(`<option value="${code.name}">${code.name}</option>`);
                });
            }
            try{
                updatePlayInfo({});
                updateVodProgress({ pos: data.playPosition, dur: data.playDuration });
                updatePlayerState(data.playState);
                updateFullscreen(data.fullscreen);
            }catch(e) {}
            updatePlaying(true);
            if(cb) cb(true);
        }).fail(() => {
            updatePlaying(false);
            if(cb) cb(false);
        });
}

function updateSeries(flagName) {
    var seriesBox = $(".series");
    seriesBox.empty();
    var playing = $(".player");
    var data = playing.data("info");
    var isCurrentPlayFlag = flagName == data.playFlag;
    data.seriesMap[flagName].forEach((el, index) => {
        var episode = $(`<div>${el.name}</div>`);
        episode.data("info", el);
        episode.data("flag", flagName);
        episode.data("index", index);
        if(isCurrentPlayFlag && index == data.playIndex)
            episode.addClass("selected");
            seriesBox.append(episode);
    });
}

function updatePlayInfo(updated) {
    var playing = $(".player");
    var data = playing.data("info");
    if(data) {
        var reverseSortUpdated = (updated.reverseSort != undefined && updated.reverseSort != data.reverseSort);
        data = Object.assign({}, data, updated);
        if(reverseSortUpdated)
        {
            Object.entries(data.seriesMap).forEach(series => {
                series[1].reverse();
            });
            updateSeries(data.playFlag);
        }
        updateParserList(data.playFlag);
        playing.data("info", data);
        var info = data.name;
        if(data.playFlag) {
            var series = data.seriesMap[data.playFlag];
            info += (" " + series[data.playIndex].name);
            $(".seriesFlags > *").each(function() {
                var _this = $(this);
                var info = _this.data("info");
                if(info.name == data.playFlag) {
                    _this.addClass("play");
                } else {
                    _this.removeClass("play");
                }
            });
        }
        if(data.playIndex != null) {
            var seriesBox = $(".series");
            seriesBox.children().each(function(index) {
                if(index == data.playIndex)
                    $(this).addClass("selected");
                else
                    $(this).removeClass("selected");
            });
        }
        if(data.playerCfg) {
            var playerCfg = JSON.parse(data.playerCfg);
            $(".vodSpeed").val(playerCfg.sp);
            $(".vodScale").val(playerCfg.sc);
            $(".vodPlayer").val(playerCfg.pl);
            $(".vodIjkCodes").val(playerCfg.ijk);
            $(".vodIjkCodesBox").css("display", playerCfg.pl == 1 ? "" : "none");
        }
        playing.find(".info").text(info);
        updatePlayButton(data.playState);
    } else {
        if(updated.playerCfg)
            cachedPlayerCfg = updated.playerCfg;
    }
}

function updatePlayButton(state) {
    if(state == 3) {
        $(".player .play-pause").removeClass("fa-circle-play").addClass("fa-circle-pause");
    } else if(state == 4) {
        $(".player .play-pause").removeClass("fa-circle-pause").addClass("fa-circle-play");
    }
}

function updatePlaying(show) {
    if(show) {
        $(".player").addClass("show");
        $("#mainTab").animate({"height": $(".page__bd").height() - 100}, 300);
    } else {
        var playing = $(".player");
        playing.data("info", null);
        $(".player").removeClass("show");
        $("#mainTab").animate({"height": $(".page__bd").height()}, 300);
    }
}

function updateVodProgress(dataObj) {
    var percent = (dataObj.pos / dataObj.dur * 100);
    $sliderHandler.data("duration", dataObj.dur);
    $sliderHandler.css("left", percent + "%");
    $sliderTrack.css("width", percent + "%");
    var duration = parseInt(dataObj.dur / 60000) + ":" + parseInt(dataObj.dur % 60000 / 1000).toLocaleString('en-US', {
        minimumIntegerDigits: 2,
        useGrouping: false
        });
    var position = parseInt(dataObj.pos / 60000) + ":" + parseInt(dataObj.pos % 60000 / 1000).toLocaleString('en-US', {
        minimumIntegerDigits: 2,
        useGrouping: false
        });
    $sliderValue.text(position + "/" + duration);
}

function updatePlayerState(state) {
    updatePlayButton(state);
    if (state <= playerState.STATE_PREPARED[0]) {
        var controller = $(".player .vod-controller");
        controller.css("display", "none");
        var stateInfoBox = $(".player .vod-state");
        stateInfoBox.fadeOut({duration: 300, complete: function() {
            stateInfoBox.text(lookUpPlayerState(state)[1]);
            stateInfoBox.fadeIn({duration: 300, complete: function() {
                if(state == playerState.STATE_PREPARED[0])
                    window.setTimeout(function() {
                        controller.css("display", "block");
                        stateInfoBox.css("display", "none");
                    }, 500);
            }});
        }});
    }
}

function updateFullscreen(isFullscreen) {
    if(isFullscreen) {
        $(".player .fullscreen").removeClass("fa-expand").addClass("fa-compress");
    } else if(isFullscreen == false) {
        $(".player .fullscreen").removeClass("fa-compress").addClass("fa-expand");
    }
}

function updateParserList(flag) {
    var parserBox = $(".parsers");
    if(parserFlags.indexOf(flag) >= 0) {
        parserBox.removeClass("hide");
    } else {
        parserBox.addClass("hide");
    }
    
}

function runBackgroundSearch(keyword) {
    var settings = {
        url: `/api?type=search&title=${keyword}`,
        method: "POST",
        contentType: "application/json; charset=utf-8"
    };
      
    $.ajax(settings).done(function(data) {
        if(data.succeeded) {
            $("#successToastContent").text(`已请求TV猫盒后台搜索 ${keyword}`);
            $("#successToast").fadeIn();
            window.setTimeout(() => $("#successToast").fadeOut(), 2000);
        } else {
            $("#warnToastContent").text(`TV猫盒无法识别该请求`);
            $("#warnToast").fadeIn();
            window.setTimeout(() => $("#warnToast").fadeOut(), 2000);
        }
    }).fail(function(jqXHR, textStatus, errorThrown) {
        $("#warnToastContent").text(`TV猫盒无法被访问`);
        $("#warnToast").fadeIn();
        window.setTimeout(() => $("#warnToast").fadeOut(), 2000);
    });
    window.setTimeout(() => $("#search-tab-background").click(), 500);
}

function updateBackgroundResult(data) {
    if(data.action == "start") {
        $("#backendResults").empty();
        $("#backSearchLoadMore").show();
        $("#backSearchNoMore").hide();
    } else if(data.action == "end") {
        $("#backSearchLoadMore").hide();
        $("#backSearchNoMore").show();
    } else if(data.results) {
        convertSearchResult(data.results, $("#backendResults"));
    }
}

function updateForegroundResult(data) {
    if(data.action == "start") {
        $("#foreResults").empty();
        $("#foreSearchLoadMore").show();
        $("#foreSearchNoMore").hide();
    } else if(data.action == "end") {
        $("#foreSearchLoadMore").hide();
        $("#foreSearchNoMore").show();
    } else if(data.results) {
        convertSearchResult(data.results, $("#foreResults"));
    }
}

function convertSearchResult(results, box) {
    results.forEach(el => {
        var resultBox = $(`<div class="searchResult" style="display: none;">
            <div class="infoPanel">
                <div class="img">
                </div>
                <div class="detail">
                    <div class="name"><span class="value"></span></div>
                    <div class="source">来源: <span class="value"></span></div>
                    <div class="note"><span class="value"></span></div>
                </div>
            </div>
        </div>`);
        resultBox.data("voddata", { sourceKey: el.sourceKey, id: el.id, name: el.name });
        resultBox.find(".img").css("background-image", `url(${el.pic})`);
        var foundSource = apiList.find(x => x.key == el.sourceKey);
        resultBox.find(".name .value").text(el.name);
        resultBox.find(".source .value").text(foundSource ? foundSource.name : el.sourceKey);
        resultBox.find(".note .value").text(el.note);
        box.append(resultBox);
        resultBox.fadeIn();
    });
}

function playLive(groupIndex, channelIndex, name) {
    var settings = {
        url: "/api?type=play-live",
        method: "POST",
        contentType: "application/json; charset=utf-8",
        data: JSON.stringify({
          "groupIndex": groupIndex,
          "channelIndex": channelIndex
        })
    };
      
    $.ajax(settings).done(function(data) {
        if(data.succeeded) {
            $("#successToastContent").text(`已请求在TV猫盒播放直播 ${name}`);
            $("#successToast").fadeIn();
            window.setTimeout(() => $("#successToast").fadeOut(), 2000);
        } else {
            $("#warnToastContent").text(`TV猫盒无法识别该请求`);
            $("#warnToast").fadeIn();
            window.setTimeout(() => $("#warnToast").fadeOut(), 2000);
        }
    }).fail(function(jqXHR, textStatus, errorThrown) {
        $("#warnToastContent").text(`TV猫盒无法被访问`);
        $("#warnToast").fadeIn();
        window.setTimeout(() => $("#warnToast").fadeOut(), 2000);
    });
}