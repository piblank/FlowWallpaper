# Flow Field — 动态壁纸 APK（Perlin 噪声粒子流场）

一个**原生（Canvas，无 WebView、无第三方依赖）**的 Android 动态壁纸：数千粒子在种子化 3D Perlin 噪声向量场中流动，留下半透明彩色轨迹，深色背景 + 暗角。颜色、粒子数、流速、种子均可自定义。

## 特性
- 真·系统动态壁纸（WallpaperService），不依赖浏览器。
- 日落 / 海洋 / 霓虹 / 单色 预设，或输入任意 RGB 配色（逗号分隔，3 的倍数）。
- 可调粒子数（1500–6000）与流速；「重新生成」一键换流场形态。
- 手指滑动时粒子会被推开（涟漪互动，桌面壁纸下不触发）。

## 方式一：云端一键构建 APK（无需本机安装任何东西）
1. 把这个 `FlowWallpaper` 文件夹推到 GitHub 仓库（或用本仓库）。
2. 打开仓库的 **Actions → Build Live Wallpaper APK → Run workflow**。
3. 跑完后在 **Artifacts** 下载 `flow-wallpaper-apk`（即 `app-debug.apk`）。
4. 把 APK 传到手机安装即可（允许「未知来源」安装）。

> debug 包已用 Android 默认调试签名，可直接安装到任意设备用于自用。
> 若要发布/上架，请在本机用 Android Studio 走「Generate Signed Bundle / APK」。

## 方式二：本机用 Android Studio 构建
1. 安装 [Android Studio](https://developer.android.com/studio)（免费）。
2. `File → Open` 选择本 `FlowWallpaper` 文件夹，等待 Gradle 同步。
   （首次打开 Android Studio 会自动生成 Gradle Wrapper。）
3. `Build → Generate Signed Bundle / APK → APK`，按提示创建/选择密钥库。
4. 产物在 `app/release/app-release.apk`。

## 安装与设置
- 安装后用「文件管理」点开 APK 安装；或从启动器打开 **Flow Wallpaper** 进入设置页。
- 设置颜色/粒子数/流速后，回到桌面：**长按空白处 → 壁纸 → 选择「Flow Field 流场壁纸」→ 应用**。
- 之后想改颜色：打开启动器里的 **Flow Wallpaper** App 修改即可，壁纸实时生效。

## 自定义颜色规则
在设置页的「颜色」框输入 RGB 值，逗号分隔，数量必须是 **3 的倍数**（即 颜色数×3）。
例如日落默认：`255,111,97, 255,179,71, 232,74,138, 91,64,153`（珊瑚/琥珀/品红/靛蓝）。

## 性能说明
- 默认约 3200 粒子、~60fps，兼顾流畅与续航。可在设置里调高（更密）或调低（更省电）。
- 真 120fps 可把 `FlowWallpaperService.FRAME_DELAY` 改为 8，但会更耗电。
