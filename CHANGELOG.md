# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)

## [0.8.20220909] - 2022-09-09
### Added
- 安卓8.0及更高版本的设备可使用画中画
- 手机模式：全屏界面左上角添加返回按钮

### Changed
- 控制器问题修复
- 优化全屏切换

## [0.8.20220906] - 2022-09-06
### Changed
- 修复安卓4.x不能进入播放
- 修复网页遥控不能点击搜索结果
- 修复剧集按钮倒叙后不能全屏
- 修复小米不能触发更新安装包
- 部分界面更新

## [0.8.20220901] - 2022-09-01
### Added
- 长按播放控制器的跳过片头，片尾和步数按钮可重置值
- 支持源配置内直接使用jar/spider定义jar包
- 网盘支持添加Alist网页
- 网盘搜索 （暂时实现了Alist网页搜索结果）
- 全屏播放添加分辨率 （Credit: takagen99)，完结时间
- 增加设备类型选择：手机/电视，以支持手机锁屏
- 页面遥控增强 （如果设备出现播放卡顿，请尝试关闭此功能）：
 1. 搜索结果同步到遥控页面
 2. 后台搜索
 3. 同步点播和直播播放器

### Changed
- 重构老猫界面以解决某些接口出现卡顿
- 修复两次加载接口的问题
- 播放器使用Smart YouTube风格
- 强制WebView解析使用缓存
- WebView解析使用爬虫User Agent，或使用默认随机User Agent

### Removed

## [0.8.20220802.2] - 2022-08-02
### Added
- 网盘（暂时支持webDAV和本地）

### Changed
- 修复4.x安卓TLS1.1 1.2握手错误
- 修复推荐页电视出错

### Removed