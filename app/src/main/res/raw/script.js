function search() {
    doAction('search', { word: $('#search_key_word').val() });
}

function backgroundSearch() {
    runBackgroundSearch($('#search_key_word').val());
}

function api() {
    doAction('api', { url: $('#diy_api_url').val() });
}

function push() {
    doAction('push', { url: $('#push_url').val() });
}

function doAction(action, kv) {
    kv['do'] = action;
    // alert(JSON.stringify(kv));
    $.post('/action', kv, function (data) {
        console.log(data);
        // alert(data);
    });
    return false;
}

function tpl_top(path) {
    return `<a class="weui-cell  weui-cell_access" href="javascript:void(0)" onclick="listFile('` + path + `')">
    <div class="weui-cell__hd"><img src="`+ ic_dir + `" alt="" style="width: 32px; margin-right: 16px; display: block;"></div>
    <span class="weui-cell__bd">
        <span>..</span>
    </span>
    <span class="weui-cell__ft">
    </span>
    </a>`;
}

function tpl_dir(name, time, path) {
    return `<a class="weui-cell  weui-cell_access" href="#" onclick="listFile('` + path + `')">
    <div class="weui-cell__hd"><img src="`+ ic_dir + `" alt="" style="width: 32px; margin-right: 16px; display: block;"></div>
    <span class="weui-cell__bd">
    <span>`+ name + `</span>
        <div class="weui-cell__desc">`+ time + `</div>
    </span>
    <span class="weui-cell__ft">
    </span>
    </a>`;
}

function tpl_file(name, time, path, canDel) {
    return `<a class="weui-cell  weui-cell_access" href="javascript:void(0)" onclick="selectFile('` + path + `', ` + canDel + `)">
    <div class="weui-cell__hd"><img src="`+ ic_file + `" alt="" style="width: 32px; margin-right: 16px; display: block;"></div>
    <span class="weui-cell__bd">
        <span>`+ name + `</span>
        <div class="weui-cell__desc">`+ time + `</div>
    </span>
    </a>`;
}

function clear_list() {
    $('#file_list').html('');
}

function add_file(node) {
    $('#file_list').append(node);
}

let ic_dir = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAA7AAAAOwBeShxvQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAIoSURBVFiF7Ze/T9RgGMc/T9/WO+74IXdRNKCJxETPQaOJi8LoxiouTurmv+DAYvwH3JzVhOhgSHByMTqY6GBCCBFMcJFwURC4O1rbvo8DoIg9rvHuYOGTdGjfb/J88vZpn1Z0bKw7JjsiaCcJqMiCKQ1OyuhonLTeLG5MZlLQq/UCooqd/nwfuNcOAUfgSorcbR0fN20RACRF7hgzc0PtEJB47IGmCSq8EXjbOKjG+VG54Buv531puK/WkfO2L0cY+ZI5ksVfD84vTt91U5vCEBvH7qxU0GqV18PX+Xg2+e6GFmZXhPlc3zOX5dW0Do2xMbIeEBuXmVMX68Y8BwxQzhXFlWqtdQKbzA+cIch0pMq630vgX55DTJgY+Bn18ql8hyAqphb4VjjeMLPVeG722nOKztddw0EPTARPUws0ohqB3TRw8w2KAwyaCRara/i2u+niCqxHf85TPQWe+Jw2L3kX3GhaYCdip5xU74FV28dSPNB8QZR8VEbCiCe1h+kFWopCVLH42oWz58Xh98s/y9o+CWzjQOBA4EBg3wX+mgWxdwlrTtSJRrjBK0T99glUC49R52jdcG75Fp7/on0C4BGGIeWFMtsHRL4zT2/hMCoerebfHkgYTWm+2/+XHTsQ4h3y6D/ZnxgWjRKvt0wgv3QTa+rN/I0mbDVipxxLe3c5kWjNgqIOSOO/nRajMVu99kF0hi4iM4LQtSfVrWbigHMa21k35NEvWSq4Cnb1Ay8AAAAASUVORK5CYII=';
let ic_file = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAA7AAAAOwBeShxvQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAJdSURBVFiF7ZfLaxNRFMa/c+8MbZO0xAgmGIIxUmtsQCkobsSNO12I7v0PXAjuBDeCCN0I/gvdu3HhQlHBBwouTA1UUoulKXmYSDp59TEzx4XUpmFm0tsMTRd+u5l7uN9vvnvunRmCh+r1+km2+CWAlFedm5byP79PIHRx8tKk4VYjvCawTfvafs0BoN1qThWNYn7h3cK4EgDPI23P07Ox5pM7+zXfVqPRPFbulPO517nQngBmv3JwkwIPQXQDXDs9KAAANI1WtLpVXcx9yEU8AWY/WU95nRvL1tVbfhgLKXcgGq1oZe3XajabPdJdo3VfMOg2AAL7YQ8kUyewIiQs0wQAbG5sjHbqnUUARx0BAMi/IP4oEAxganpnFcuFEggU7q7x3AUHoaED9C6Bp5gZlmUpGQgSENL9OR0B2KUJCssFFAslJQAShPMXzkHX9b0DuCmRTCCRTCgB9JPzSeirhbeUEjDqBgyjoWQgiBCLxyCEcx8494DLZLZtwzbVmpAFeUaqlEA4EkY4Eu5fqKBDeg64bsNVVEoVJQMCITOTga47WznfJefJ4onjiMWjygBSk67jSgcRCYImlNqmr4beA0M/iP4ncDgT+BfBAUThmQDbpv+OPZ+8ngBa+xWEWfbFt9PqgJmh6/r7XR5OxduIkpcw9uMsWE4MZM7QMKoFq2uBF2dS6VO1vgArWzPIjDwHSUCOA2DXf0sFit9z6XS61nu7F8AEgLfrD/CtfQUhWR3YNzZS+ngzdPc+ps03TuO7Xjuzn61HzHQPDL1pAvaAu4AZJYvo+uPL9MWt5g/5NsVsHsMO8wAAAABJRU5ErkJggg==';

let current_root = '';
let current_parent = '';
let current_remote = '';
let current_file = '';

function selectFile(path, canDel) {
    current_file = path;
    if (canDel)
        $("#delFileBtn").fadeIn();
    else
        $("#delFileBtn").fadeOut();
    // $("#fileUrl0")[0].value = current_remote.replace('clan://', 'http://') + 'file/' + current_file;
    $("#fileUrl1")[0].value = "clan://localhost/" + current_file;
    $("#fileUrl2")[0].value = current_remote + current_file;
    var fileInfoDialog = $("#fileInfoDialog");
    fileInfoDialog.fadeIn(200);
    fileInfoDialog.find(".js_dialog").addClass('weui-half-screen-dialog_show');
    setTimeout(function(){
        fileInfoDialog.attr('aria-hidden','false');
        fileInfoDialog.attr('aria-modal','true');
        fileInfoDialog.attr('tabindex','0');
        fileInfoDialog.trigger('focus');
    },200);
}

function fileToApi(type) {
    if (type === 1) {
        doAction('api', { url: "clan://localhost/" + current_file });
    } else {
        doAction('api', { url: current_remote + current_file });
    }
}

function hideFileInfo() {
    $("#fileInfoDialog").fadeOut();
}

function listFile(path) {
    $('#loadingToast').fadeIn();
    $.get('/file/' + path, function (res) {
        let info = JSON.parse(res);
        let parent = info.parent;
        let canDel = info.del === 1;
        current_root = path;
        current_parent = parent;
        current_remote = info.remote;
        let array = info.files;
        if (path === '' && array.length == 0)
            warnToast('读取本地文件失败，可能没有存储权限');
        clear_list();
        if (parent !== '.')
            add_file(tpl_top(parent));

        if (canDel) {
            $('#delCurFolder').fadeIn();
        } else {
            $('#delCurFolder').fadeOut();
        }

        array.forEach(node => {
            if (node.dir === 1) {
                add_file(tpl_dir(node.name, node.time, node.path));
            } else {
                add_file(tpl_file(node.name, node.time, node.path, canDel));
            }
        });
        $('#loadingToast').fadeOut();
    })
}

function warnToast(msg) {
    $('#warnToastContent').html(msg);
    $('#warnToast').fadeIn();
    setTimeout(() => {
        $('#warnToast').fadeOut();
    }, 1000);
}

function uploadFile() {
    $('#file_uploader').click();
}

function uploadTip() {
    let files = $('#file_uploader')[0].files;
    if (files.length <= 0)
        return false;
    let tip = '';
    for (var i = 0; i < files.length; i++) {
        tip += (files[i].name) + ',';
    }
    $('#uploadTipContent').html(tip);
    $('#uploadTip').fadeIn();
}

function doUpload(yes) {
    $('#uploadTip').fadeOut();
    if (yes == 1) {
        let files = $('#file_uploader')[0].files;
        if (files.length <= 0)
            return false;
        var formData = new FormData();
        formData.append('path', current_root);
        for (i = 0; i < files.length; i++) {
            formData.append("files-" + i, files[i]);
        }
        $('#loadingToast').fadeIn();
        $.ajax({
            url: '/upload',
            type: 'post',
            data: formData,
            processData: false,
            contentType: false,
            complete: function () {
                $('#loadingToast').fadeOut();
                listFile(current_root);
            }
        });
    }
}

function newFolder() {
    $('#newFolder').fadeIn();
}

function doNewFolder(yes) {
    $('#newFolder').fadeOut();
    if (yes == 1) {
        let name = $('#newFolderContent')[0].value.trim();
        if (name.length <= 0)
            return false;
        $('#loadingToast').fadeIn();
        $.post('/newFolder', { path: current_root, name: '' + name }, function (data) {
            $('#loadingToast').fadeOut();
            listFile(current_root);
        });
    }
}


function delFolder() {
    $('#delFolderContent').html('是否删除 ' + current_root);
    $('#delFolder').fadeIn();
}

function doDelFolder(yes) {
    $('#delFolder').fadeOut();
    if (yes == 1) {
        $('#loadingToast').fadeIn();
        $.post('/delFolder', { path: current_root }, function (data) {
            $('#loadingToast').fadeOut();
            listFile(current_parent);
        });
    }
}

function delFolder() {
    $('#delFolderContent').html('是否删除 ' + current_root);
    $('#delFolder').fadeIn();
}

function doDelFolder(yes) {
    $('#delFolder').fadeOut();
    if (yes == 1) {
        $('#loadingToast').fadeIn();
        $.post('/delFolder', { path: current_root }, function (data) {
            $('#loadingToast').fadeOut();
            listFile(current_parent);
        });
    }
}

function delFile() {
    hideFileInfo();
    $('#delFileContent').html('是否删除 ' + current_file);
    $('#delFile').fadeIn();
}

function doDelFile(yes) {
    $('#delFile').fadeOut();
    if (yes == 1) {
        $('#loadingToast').fadeIn();
        $.post('/delFile', { path: current_file }, function (data) {
            $('#loadingToast').fadeOut();
            listFile(current_root);
        });
    }
}

function showPanel(id) {
    let tab = $('#tab' + id)[0];
    $(tab).attr('aria-selected', 'true').addClass('weui-bar__item_on');
    $(tab).siblings('.weui-bar__item_on').removeClass('weui-bar__item_on').attr('aria-selected', 'false');
    var panelId = '#' + $(tab).attr('aria-controls');
    if (id === 6 && current_remote.length === 0) {
        listFile('')
    }
    $(panelId).css('display', 'block');
    $(panelId).siblings('.weui-tab__panel').css('display', 'none');
}

function closeDialog(o){
    const $jsDialogWrap = o.parents('.js_dialog_wrap');
    if($jsDialogWrap.attr("onclose"))
        eval($jsDialogWrap.attr("onclose"));
    $jsDialogWrap.attr('aria-hidden','true').attr('aria-modal','false').removeAttr('tabindex');
    $jsDialogWrap.fadeOut(300);
    $jsDialogWrap.find('.js_dialog').removeClass('weui-half-screen-dialog_show');
    setTimeout(function(){
      $('#' + $jsDialogWrap.attr('ref')).trigger('focus');
    }, 300);
  }

var $sliderTrack = $('#sliderTrack'),
    $sliderHandler = $('#sliderHandler'),
    $sliderValue = $('#sliderValue')
    isSliderDragging = false;;

$(function () {

    var totalLen = $('#sliderInner').width(),
        startLeft = 0,
        startX = 0;

    $sliderHandler
        .on('touchstart', function (e) {
            startLeft = parseInt($sliderHandler[0].style.left) * totalLen / 100;
            startX = e.originalEvent.changedTouches[0].clientX;
            isSliderDragging = true;
        })
        .on('touchmove', function(e){
            var dist = startLeft + e.originalEvent.changedTouches[0].clientX - startX,
                percent;
            dist = dist < 0 ? 0 : dist > totalLen ? totalLen : dist;
            percent =  dist / totalLen * 100;
            $sliderTrack.css('width', percent + '%');
            $sliderHandler.css('left', percent + '%');
            var durationNum = $sliderHandler.data("duration");
            var duration = parseInt(durationNum / 60000) + ":" + parseInt(durationNum % 60000 / 1000).toLocaleString('en-US', {
                minimumIntegerDigits: 2,
                useGrouping: false
              });
            var positionNum = durationNum * (percent / 100);
            var position = parseInt(positionNum / 60000) + ":" + parseInt(positionNum % 60000 / 1000).toLocaleString('en-US', {
                minimumIntegerDigits: 2,
                useGrouping: false
              });
            $sliderValue.text(position + "/" + duration);
            e.preventDefault();
        })
        .on('touchend', function(e) {
            isSliderDragging = false;
            var dist = startLeft + e.originalEvent.changedTouches[0].clientX - startX,
                percent;
            dist = dist < 0 ? 0 : dist > totalLen ? totalLen : dist;
            percent =  dist / totalLen * 100;
            mainSocket.send(JSON.stringify({ type: 'Vod-seek', percent: percent }));
        });
    
    $(".page").addClass("js_show");
    $('.weui-tabbar__item').on('click', function () {
        showPanel(parseInt($(this).attr('id').substr(3)));
    });
    $('.js_dialog_wrap').on('touchmove', function(e) {
        //e.preventDefault();
    });
    $('.js_close').on('click', function() {
      closeDialog($(this));
    });
    $('.weui-navbar__item').on('click', function () {
        $(this).attr('aria-selected','true').addClass('weui-bar__item_on');
        $(this).siblings('.weui-bar__item_on').removeClass('weui-bar__item_on').attr('aria-selected','false');
        var panelId = '#' + $(this).attr('aria-controls');
        $(panelId).css('display','block');
        $(panelId).siblings('.weui-tab__panel').css('display','none');
    });
    $('#loadingToast').fadeIn();
    updateApiList(() => {
        $('#loadingToast').fadeOut();
    });
    updateChannelGroup();
    $("body").on("change", "#vodSelect", function() {
        $('#loadingToast').fadeIn();
        getHomeData(this.value, () => {
            $('#loadingToast').fadeOut();
        });
    });
    $("body").on("change", "#categorySelect", function() {
        $('#loadingToast').fadeIn();
        getVodData(this.value, "", true, () => {
            $('#loadingToast').fadeOut();
        });
    });
    $("#panel2").on("scroll", function() {
        if($(this).scrollTop() + $(this).innerHeight() >= $(this)[0].scrollHeight - 134) {
            getVodData($("#categorySelect").val(), "", false);
        }
    });
    $("body").on('click', '#btnVodFilterOpen', function(){
        var vodFilterDialog = $("#vodFilterDialog");
        vodFilterDialog.fadeIn(200);
        vodFilterDialog.find(".js_dialog").addClass('weui-half-screen-dialog_show');
        setTimeout(function(){
            vodFilterDialog.attr('aria-hidden','false');
            vodFilterDialog.attr('aria-modal','true');
            vodFilterDialog.attr('tabindex','0');
            vodFilterDialog.trigger('focus');
        },200);
    });
    $("body").on('click', "#btnConfirmFilter", function() {
        var selectors = $(".vod-filter-select");
        var filters = [];
        selectors.each(index => {
            var jqSelector = $(selectors[index]);
            if(jqSelector.val()) {
                filters.push({ k: jqSelector.data("key"), v: jqSelector.val() });
            }
        });
        var filterStr = "";
        if(filters.length > 0)
            filterStr = JSON.stringify(filters);
        var base64Filter = btoa(unescape(encodeURIComponent(filterStr)));
        if(base64Filter != currentVodData.filter) {
            $("#vodFilterDialog").find(".js_close").trigger("click");
            $('#loadingToast').fadeIn();
            getVodData($("#categorySelect").val(), filterStr, true, () => {
                $('#loadingToast').fadeOut();
            });
        }
    });
    $("body").on('click', ".vodItem", function() {
        var vodData = $(this).data("voddata");
        playVod(currentVodData.sourceKey, vodData.id, vodData.name);
    });
    $("body").on('click', '.player .close-vod', function() {
        $("#playerPopDialogWrap").find(".js_close").trigger("click");
        $("#closeVodVideo").fadeIn();
    });
    $("body").on('click', '.player .fa-expand, .player .fa-compress', function() {
        var isFullscreen = $(this).hasClass("fa-expand");
        mainSocket.send(JSON.stringify({type: 'Vod-fullscreen', isFullscreen: isFullscreen}));
    });
    $("body").on('click', '.player .fa-backward-step, .player .fa-forward-step', function() {
        var isNext = $(this).hasClass("fa-forward-step");
        mainSocket.send(JSON.stringify({type: 'Vod-gotopos', way: isNext ? 'next' : 'previous'}));
    });
    $("body").on('click', '.player .fa-circle-play, .player .fa-circle-pause', function() {
        var isPlay = $(this).hasClass("fa-circle-play");
        mainSocket.send(JSON.stringify({type: 'Vod-playpause', isPlay: isPlay}));
    });
    $("body").on('click', '#playing .expand-info', function() {
        var playerPopDialog = $("#playerPopDialogWrap");
        $("#playerPopSliderBox").append($("#playingSliderBox").children());
        playerPopDialog.fadeIn(200);
        playerPopDialog.find(".js_dialog").addClass('weui-half-screen-dialog_show');
        setTimeout(function(){
            playerPopDialog.attr('aria-hidden','false');
            playerPopDialog.attr('aria-modal','true');
            playerPopDialog.attr('tabindex','0');
            playerPopDialog.trigger('focus');
        },200);
    });
    $("body").on('click', '.seriesFlags > *', function(){
        $('.seriesFlags > *').removeClass("selected");
        var _this = $(this);
        _this.addClass("selected");
        var data = _this.data("info");
        updateSeries(data.name);
    });
    $("body").on('click', '.series > *:not(selected)', function(){
        $('.seriesFlags > *').removeClass("selected");
        var _this = $(this);
        _this.addClass("selected");
        var index = _this.data("index");
        var flagName = _this.data("flag");
        mainSocket.send(JSON.stringify({type: 'Vod-gotopos', way: JSON.stringify({ flag: flagName, index: index })}));
    });
    $("body").on('click', '.parsers:not(.hide) > *:not(selected)', function(){
        var allParsers = $('.parsers > *');
        allParsers.removeClass("selected");
        var _this = $(this);
        _this.addClass("selected");
        var index = allParsers.index(this);
        mainSocket.send(JSON.stringify({type: 'Vod-parser', index: index}));
    });
    $("body").on('change', '.vodSpeed, .vodScale, .vodPlayer, .vodIjkCodes', function(){
        var _this = $(this);
        var sentData = {type: 'Vod-playerconfig'};
        sentData[_this.data("name")] = _this.val();
        mainSocket.send(JSON.stringify(sentData));
    });
    $("body").on('click', '.searchResult', function() {
        var vodData = $(this).data("voddata");
        playVod(vodData.sourceKey, vodData.id, vodData.name);
    });
    $("body").on('change', '#liveGroupSelect', function() {
        updateChannelList($(this).val());
    });
    $("body").on('click', '#btnPlayLive', function() {
        playLive(
            $("#liveGroupSelect").val(), 
            $("#channelSelect").val(), 
            $("#channelSelect option:selected").text());
    });
});

function closePlayingVod(shouldClose) {
    $("#closeVodVideo").fadeOut();
    if(shouldClose)
        mainSocket.send(JSON.stringify({type: 'Vod-close'}));
}

function onHidingPlayerDialog() {
    $("#playingSliderBox").append($("#playerPopSliderBox").children());
}

var url = window.location.href;
if (url.indexOf('push') > 0)
    showPanel(4);
else if (url.indexOf('api') > 0)
    showPanel(6);
else
    showPanel(1);

var mainSocket;

function openSocket() {
    if(mainSocket && (mainSocket.readyState == WebSocket.OPEN || mainSocket.readyState == WebSocket.CONNECTING))
        mainSocket.close();
    $.get('/websocket-address', (data) => {
        if(!data) {
            $("#btnSearchBackend, #searchResultTabs, #tab2, #tab3, #tab5").css("display", "none");
            return;
        }
        mainSocket = new WebSocket(data);
        mainSocket.onmessage = onSocketMessageReceive;
        mainSocket.onclose = onSocketClose;
        var socketWait = function() {
            if(mainSocket.readyState == 1) {
                var data = $(".player").data("info");
                if(data && data.playState > playerState.STATE_PREPARED[0]) {
                    mainSocket.send(JSON.stringify({type: 'Vod-playing'}));
                }
            }
            if(mainSocket.readyState <= 0)
                window.setTimeout(socketWait, 5);
        }
        window.setTimeout(socketWait, 5)
        
    }).fail(() => {
        $("#playerPopDialogWrap").find(".js_close").trigger("click");
        updatePlaying(false);
        onSocketClose(null);
    });
}

function onSocketMessageReceive(event) {
    if(event.data) {
        var dataObj = JSON.parse(event.data);
        if(dataObj.type == "updateApiUrl")
            window.setTimeout(() => updateApiList(), 5000);
        else if(dataObj.type == "ctrl") {
            if(!isSliderDragging && dataObj.state >= playerState.STATE_PLAYING[0]) {
                updateVodProgress(dataObj);
            }
            updatePlayerState(dataObj.state);
        } else if(dataObj.type == "detail") {
            var tryCount = 0;
            if(dataObj.state == "activated") {
                var retryFunc = function(succeeded) {
                    if(!succeeded && tryCount < 5) {
                        getPlaying(retryFunc);
                        tryCount++;
                    }
                };
                getPlaying(retryFunc);
            } else if(dataObj.state == "deactivated") {
                $("#playerPopDialogWrap").find(".js_close").trigger("click");
                updatePlaying(false);
            }
            updateFullscreen(dataObj.fullscreen);
        } else if(dataObj.type == "vod-update-info") {
            dataObj.type = undefined;
            updatePlayInfo(dataObj);
        } else if(dataObj.type == 'parser-change') {
            $(".parsers > div").removeClass("selected");
            $(".parsers > div").each(function(){
                var _this = $(this);
                if(_this.text() == dataObj.parser) {
                    _this.addClass('selected');
                    return false;
                }
            });
        } else if(dataObj.type == "back-search") {
            updateBackgroundResult(dataObj);
        } else if(dataObj.type == "search") {
            updateForegroundResult(dataObj);
        }
    }
}

function onSocketClose(event) {
    window.setTimeout(() => openSocket(), 1000);
}

openSocket();
initParserData();
getPlaying();