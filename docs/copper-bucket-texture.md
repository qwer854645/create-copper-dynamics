# 铜桶贴图

使用 `outputs/copper-bucket/vanilla-copper/empty.png` 的 16×16 铜色稿。
保留 Minecraft 铁桶轮廓、透明度和明暗像素布局，通过 pixel-art 工具逐色映射为 CCD 铜色色阶。
该资源为 Mojang 原版贴图的改色衍生，不标为 CCD 原创；上游美术权利归原权利方。

运行时贴图位于 `assets/ccd/textures/item/copper_bucket.png`。
模型仅覆盖 NeoForge 动态流体容器的 `base`，流体遮罩继续继承原生桶遮罩。
移除旧的桶壳铜色乘算，改用原生 `DynamicFluidContainerModel.Colors`：桶壳保持贴图原色，流体继续使用自身纹理、颜色和动画。

生成脚本为 `tools/preview-copper-bucket.py`。预览中的水和熔岩使用原版装液桶图片展示改色，游戏实际显示由动态流体模型负责。
仅完成资源与源码静态检查，未构建或启动游戏。
