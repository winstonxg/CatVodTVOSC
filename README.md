# TV猫盒
## _在TV盒子里抓住了一直猫咪~o( =∩ω∩= )m_

[![N|Solid](https://raw.githubusercontent.com/kensonmiao/CatVodTVOSC/main/app/src/main/res/drawable/app_banner.png)](https://nodesource.com/products/nsolid)

[![Build Status](https://travis-ci.org/joemccann/dillinger.svg?branch=master)](https://travis-ci.org/joemccann/dillinger)

## 功能

- 基本老猫功能
- 支持定义多JARs接口
- 可更换主界面样式 （集合式【完成】，老猫【完成】，安卓电视横屏滚动【开发中】）
- 剧集详细页长按集数或第三方播放器按钮可选择第三方播放器
- 推荐按钮

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
        { "n": "default", "v": "jar地址" }, //默认jar
        { "n": "jar1", "v": "jar1 地址" },
        { "n": "jar2", "v": "jar2 地址" }
        ...
    ],
    "sites": [
        ...
        { "key": "csp_csp1", "name": "CSP1", ..., "spider": "jar1" }, //对应spider里的n值
        { "key": "csp_csp2", "name": "CSP2", ..., "spider": "jar2" },
        { "key": "csp_csp3", "name": "CSP3", ... },  //没有spider参数的话，使用默认jar
        { "key": "csp_csp4", "name": "CSP4", ..., "spider": "jar2" },
        ...
    ],
    ...
}
```

