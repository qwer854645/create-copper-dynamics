# 增压泵模型

铜壳使用 pixel-art 技能绘制的原创 16×16 贴图，参考 Create 机械泵与铜机壳的色阶，保留像素接缝和磨损，去掉壳体铆钉。

两个流体口直接导入 Create 6.0.10-280 的 `fluid_pipe/rim_connector/up`、`rim/up`、`rim_connector/down`、`rim/down` 原模型几何，逐面保留原生 UV、旋转、裁切面和尺寸，贴图直接引用 `create:block/pipes`。没有重画管口或复制管道贴图。原接口资源归 The Create Team / The Creators of Create 所有，不将其标注为 CCD 原创；美术许可见 [Create LICENSE.md](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md)。

四个动力口取消自制轴承座，仅保留现有 `BoosterPumpRenderer` 绘制的 Create `SHAFT_HALF`。静态模型不额外放置轴体，避免重叠；物品外观沿用静态父模型，不额外实现动态半轴渲染。中央泵体尺寸和接口中心不变。方块朝向、管路连接、四面动力、碰撞、泵送能力和应力配置沿用现有逻辑。

`tools/create-booster-pump.cjs` 接收解压的 Create 源码根目录，生成资源及 `outputs/booster-pump/preview.png`。预览额外组合原生半轴，展示水平正反面与竖直摆放，轴按静止角度示意。`textures.png` 仅展示 CCD 自绘铜壳。

本次只进行静态资源检查和离线模型渲染，没有构建或启动游戏。
