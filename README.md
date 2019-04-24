SAF Enhancer Lite | SAF 增强器 Lite

----

[English README](./README-en.md)

帮助不支持 Storage Access Framework 的应用程序（例如微信）通过 Android 文档界面选择媒体

## 它可以做什么？

处理应用的拍摄请求并将从 Android 文档界面所选择的媒体文件作为结果回应，从而使得本身不支持 
Storage Access Framework 的应用程序也可以支持在文档界面中选择媒体。

## 如何使用？

1. 从 [GitHub Releases](https://github.com/fython-tools/DocUIProxy-Android/releases) 下载预编译
   包，或者自行 Clone 项目下来进行编译。
2. 打开 “SAF 增强器 Lite” 进行必要的设置。
   a. 赋予必要的存储权限，否则选择媒体文件后无法将结果传递给应用。
   b. 选择要被处理的应用，默认只有微信会被处理。
   c. 如果你还使用了 “存储重定向” 或操作系统是 Android Q 或更新的版本，请根据界面内提示进行设定。
3. 在你的应用内按下可以打开系统相机的按钮，选择 “SAF 增强器 Lite”，即可从 Android 文档界面中选择
   媒体文件。（你可以直接设定为默认值，我们还允许其它应用调用 SAF 增强器时继续使用真实的相机应用。）

以微信为例：
按照 1、2 步骤进行后，
在聊天界面的更多栏中长按 “相册/相簿” 按钮，会弹出默认相机选择，请选择 “SAF 增强器 Lite”，
根据你的需求选择媒体图片，将会成功返回到微信的准备发送界面。

## 演示视频

[https://www.youtube.com/watch?v=R29z_ZaQN3Y](https://www.youtube.com/watch?v=R29z_ZaQN3Y)

## 联系作者

Telegram @fython

## 许可证

GPL v3