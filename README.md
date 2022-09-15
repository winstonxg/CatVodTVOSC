# TV猫盒
## _在TV盒子里抓住了一直猫咪~o( =∩ω∩= )m_

[![N|Solid](https://raw.githubusercontent.com/kensonmiao/CatVodTVOSC/main/app/src/main/res/drawable/app_banner.png)](https://nodesource.com/products/nsolid)

[![Build Status](https://travis-ci.org/joemccann/dillinger.svg?branch=master)](https://travis-ci.org/joemccann/dillinger)

## 功能

- 基本老猫功能
- 支持定义多JARs接口
- 可更换主界面样式 （集合式【完成】，老猫【完成】）
- 剧集详细页长按集数或第三方播放器按钮可选择第三方播放器
- 推荐按钮
- 网盘
- 页面遥控增强
 1. 搜索结果同步到遥控页面
 2. 后台搜索
 3. 同步点播和直播播放器
 4. 网盘浏览/点播，控制和导出

## Ver 0.7.20220722
## 额外新的参数如下

壁纸:
```javascript
{
    "wallpaper": "壁纸路径" //例子 "https://picsum.photos/1920/1080/?blur=10"
}
```

单Jar的定义方式不变，直接放路径即可:
```javascript
{
    ...
    "spider": "jar地址"
    ...
```

多Jars:
```javascript
{
    ...
    "spider": [
        { "n": "default", "v": "http://example.org/default.jar" }, //默认jar
        { "n": "jar1", "v": "http://example.org/sp.jar" },
        { "n": "p2", "v": "http://example.org/sp2.jar" }
        ...
    ],
    "sites": [
        ...
        { "key": "csp_csp1", "name": "CSP1", ..., "spider": "jar1" }, //对应spider里的n值
        { "key": "csp_csp2", "name": "CSP2", ..., "spider": "p2" },
        { "key": "csp_csp3", "name": "CSP3", ... },  //没有spider参数的话，使用默认jar
        { "key": "csp_csp4", "name": "CSP4", ..., "spider": "p2" },
        ...
    ],
    ...
}
```

## Ver 0.8.20220828
## 额外新的参数如下

直接指定Jar到源设置(JSON property name可spider亦可jar):
```javascript
{
    ...
    "sites": [
        ...
        { "key": "csp_csp1", "name": "CSP1", ..., "spider": "http://example.org/sp.jar" }, //jar包1号，当csp_csp1被调用时，sp.jar的类会被调用
        { "key": "csp_csp2", "name": "CSP2", ..., "jar": "http://example.org/sp2.jar" }, //jar包2号，当csp_csp2被调用时，sp2.jar的类会被调用
        { "key": "csp_csp3", "name": "CSP3", ... },  //没有spider参数的话，使用默认jar
        { "key": "csp_csp4", "name": "CSP4", ..., "spider": "http://example.org/sp2.jar" }, //jar包2号会被重用
        ...
    ],
    ...
}
```

## Ver 0.8.20220914
## 额外新的参数如下

针对接口源指定默认播放器:
```javascript
{
    ...
    "sites": [
        ...
        { "key": "csp_csp1", "name": "CSP1", ... }, //没有playerType参数，程序使用设置里指定的播放器
        { "key": "csp_csp2", "name": "CSP2", ..., "playerType": 0 }, //playerType 指定默认播放器为系统播放器，覆盖设置的指定播放器，但是用户变更播放器后，该影视纪录（只是该纪录）保留用户的选择
        { "key": "csp_csp3", "name": "CSP3", ..., "playerType": 1 }, //playerType 指定默认播放器为IJK，其余同上
        { "key": "csp_csp4", "name": "CSP4", ..., "playerType": 2 }, //playerType 指定默认播放器为EXO，其余同上
        ...
    ],
    ...
}
```

