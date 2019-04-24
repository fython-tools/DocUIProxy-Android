SAF Enhancer Lite | SAF 增强器 Lite

----

[中文 README](./README.md)

Help applications which does not support Storage Access Framework (like WeChat) to 
choose media from Android Documents UI.

## What it can do?

Handle capture request from apps and respond with media files chosen in Android Documents UI so 
that apps which does not support Storage Access Framework can also choose media from 
documents provider.

## How to use.

1. Download pre-built package from
   [GitHub Releases](https://github.com/fython-tools/DocUIProxy-Android/releases) 
   or clone and compile it by yourself.
2. Open "SAF Enhancer Lite" to make some initial setup:
   a. Grant necessary permission, or media chosen result cannot be received by apps.
   b. Choose apps which should be handled. (Default: WeChat only)
   c. If you also use "Storage Redirect" or operation system is Android Q+,
      please follow tips in UI and do some settings.
3. Click button which calls camera apps in your apps. Choose "SAF Enhancer Lite" and then you 
   can choose media files from Documents UI. (You can also make it as default. We allow users to
   pick a real camera app when SAFEnhancer is called by apps.)

When you are using WeChat:
After finishing 1 & 2 step, click "Gallery" button in the more banner of chat interface. 
It will pop-up a chooser to pick a camera app. Please choose "SAF Enhancer Lite" in that dialog.
Choose media files you want, result will be sent to WeChat.

## Demo video

[https://www.youtube.com/watch?v=R29z_ZaQN3Y](https://www.youtube.com/watch?v=R29z_ZaQN3Y)

## Contact author

Telegram @fython

## Licenses

GPL v3