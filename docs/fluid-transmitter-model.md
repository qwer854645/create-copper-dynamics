# 流体传输器模型

沿用用户认可的增压泵美术：简洁铜壳、原生接口、不规则像素材质，不增加铆钉、动力轴或外置轴承座。

三张原创 16×16 贴图分别为铜壳、左右过滤面和上下过滤面。手工安排连续、非对称的铜色像素簇，避免重复排列的小点。四个侧面各有内嵌过滤面板，中心保留给原生过滤图标；旁边的小箭头指向本地 +Z 输出方向。原有方块朝向变换同步旋转箭头。

中央壳体为 [0,0,3]–[16,16,13]，保持前后直通。过滤面直接位于方块四侧表面，中心为 [16,8,8]、[0,8,8]、[8,16,8]、[8,0,8]，对齐现有 `FilteringBehaviour` 的交互中心；不再使用浮在机壳外的薄铁块模型。方块碰撞沿用现有整块形状。

前后管口直接导入 Create 6.0.10-280 的 `fluid_pipe/rim_connector/north`、`rim/north`、`rim_connector/south`、`rim/south` 原生几何，完整保留尺寸、UV、旋转和裁切设置，直接引用 `create:block/pipes` 贴图。原模型与管道贴图归 The Create Team / The Creators of Create 所有，不标注为 CCD 原创；许可见 [Create LICENSE.md](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md)。

本次只修改模型与贴图；前后定向转移、四面过滤、红石暂停、同向链式转移和无内部储液的实现不变，不添加 UI 或逻辑代码。继续使用现有原生 `SmartBlockEntityRenderer` 显示过滤图标。

`tools/create-fluid-transmitter.cjs` 接收解压的 Create 源码根目录，生成资源及 `outputs/fluid-transmitter/preview.png`，展示输入侧、输出侧和竖直摆放。`textures.png` 顺序为铜壳、左右侧、上下侧。仅静态检查与离线渲染，未构建或启动游戏。
