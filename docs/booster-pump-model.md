# 增压泵模型

铜壳使用 pixel-art 技能绘制的原创 16×16 像素布局，参考 Create 机械泵与铜机壳的色阶。铜面采用大小不一、位置不对称的连续明暗像素簇，打散重复排列的点和条纹；不加铆钉。

两个流体口直接导入 Create 6.0.10-280 的 `fluid_pipe/rim_connector/up`、`rim/up`、`rim_connector/down`、`rim/down` 原模型几何，逐面保留原生 UV、旋转、裁切面和尺寸，贴图直接引用 `create:block/pipes`。没有重画管口或复制管道贴图。原接口资源归 The Create Team / The Creators of Create 所有，不将其标注为 CCD 原创；美术许可见 [Create LICENSE.md](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md)。

四个动力面使用独立的 `shaft_face.png`：外侧沿用新铜面，中央 8×8 模型像素复用 Create `gearbox.png` 的 [4,4,12,12] 区域，其中四个原机壳棕色角像素替换为对应位置的铜面像素，其余深色孔洞与灰色孔壁像素保持原值。整图最近邻扩大为 32×32，在轴孔两端各画一个 3×4 texel 的深铜色上箭头，对应四侧、两头共八个箭头；几何 UV 不改，箭头随 FACING 指向流体出口。参考原生齿轮箱与机壳传动轴模型，这是不透明的深色圆形内凹轴孔，不是透明洞；原生宽 4 的半轴置于轴孔中央，交接处可见孔壁。原生轴孔像素同样归 Create 所有。没有增加独立轴承座或铆钉。

世界转轴继续由现有 `BoosterPumpRenderer` 绘制 Create `SHAFT_HALF`，世界静态模型不放置轴体。物品模型则增加四段静态原生半轴，按 0/90/180/270 度旋转几何和对应 UV，确保物品栏可见。中央泵体尺寸和接口中心不变。放置逻辑直接继承原生 PumpBlock，移除 CCD 的额外放置选择；扳手仍用原生 FACING 反向。管路连接、四面动力、碰撞、泵送能力和应力配置沿用现有逻辑。

`tools/create-booster-pump.cjs` 接收解压的 Create 源码根目录，生成资源及 `outputs/booster-pump/preview.png`。预览读取包含静态半轴的实际物品模型，展示水平正反面与竖直摆放。`textures.png` 左侧为自绘铜壳，右侧为加入原生轴孔与箭头的动力面。

本次只进行静态资源检查和离线模型渲染，没有构建或启动游戏。
