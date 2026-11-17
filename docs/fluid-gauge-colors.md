# 流体仪表铜色边框

按要求仅将 Create 原生工厂仪表的边框改为铜色。复制 `factory_panel.png` 和 `factory_panel_packager_mode.png` 两张 16×16 原生贴图，只在 UV 0–8、0–8 的边框图块中逐像素替换色阶，各改变 44 个有色像素。透明度、表盘、指示灯、背板和其余像素保持原值。

物品模型继承 `create:item/factory_gauge`，只覆盖贴图。放置后的 `FluidGaugeModel extends FactoryPanelModel` 先调用原生 `addPanel`，保留四槽布局、安装方向、普通／补货模式和激活／被动状态，再将两种原生面板贴图的 UV 映射到各自的铜色版本。仅应用于 CCD 流体仪表；原版工厂仪表和原生灯光、路径渲染器不变。两种贴图依赖在模型烘焙前注册，资源重载时使用本次烘焙的 sprite。

`tools/recolor-fluid-gauge.cjs` 接收解压的 Create 源码根目录，使用 sharp 执行精确像素换色并生成预览。`outputs/fluid-gauge-copper/comparison.png` 左侧为原版，右侧为铜色；上排普通模式，下排补货模式。`palette.json` 保存色阶映射、尺寸及换色像素数量。本次仅静态检查和离线渲染，未构建或启动游戏。

原模型、原始贴图归 The Create Team / The Creators of Create 所有，本项目仅修改了所述边框颜色，不将其标注为原创美术。上游美术资源的许可与代码许可分别说明，见 [Create LICENSE.md](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md)。
