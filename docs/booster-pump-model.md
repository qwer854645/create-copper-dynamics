# 增压泵模型

铜壳使用 pixel-art 技能绘制的原创 16×16 像素布局，参考 Create 机械泵与铜机壳的色阶。铜面采用大小不一、位置不对称的连续明暗像素簇，打散重复排列的点和条纹；不加铆钉。

两个流体口直接导入 Create 6.0.10-280 的 `fluid_pipe/rim_connector/up`、`rim/up`、`rim_connector/down`、`rim/down` 原模型几何，逐面保留原生 UV、旋转、裁切面和尺寸，贴图直接引用 `create:block/pipes`。没有重画管口或复制管道贴图。原接口资源归 The Create Team / The Creators of Create 所有，不将其标注为 CCD 原创；美术许可见 [Create LICENSE.md](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md)。

四个动力面使用独立的 16×16 `shaft_face.png`：外侧沿用新铜面，中央 8×8 模型像素复用 Create `gearbox.png` 的 [4,4,12,12] 区域，其中四个原机壳棕色角像素替换为对应位置的铜面像素，其余深色孔洞与灰色孔壁像素保持原值。原生宽 4 的半轴置于轴孔中央，交接处可见孔壁。原生轴孔像素同样归 Create 所有，不增加独立轴承座或铆钉。

方向标记直接引用原生 `mechanical_pump/block` 端圈四个侧面的 UV=[8,0,6.5,6]、rotation=270 和 `create:block/pump` 贴图，不再画替代箭头，也不改原图像素。在现有铜壳 Y=2..4、12..14 的两端各贴合四张面，宽 12、高 2，离侧壁 .01 避免共面闪烁；标记带高度适配当前机壳两端的可用空间，不覆盖中央轴孔。源面图案、颜色和 UV 与原生一致，随六向 FACING 和原生扳手反向一起旋转。世界模型共 13 个元素，物品另加四段静态半轴。原生端圈方向图案属于 Create 美术资源。

世界转轴继续由现有 `BoosterPumpRenderer` 绘制 Create `SHAFT_HALF`，世界静态模型不放置轴体。物品模型则增加四段静态原生半轴，按 0/90/180/270 度旋转几何和对应 UV，确保物品栏可见。中央泵体尺寸和接口中心不变。放置逻辑直接继承原生 PumpBlock，移除 CCD 的额外放置选择；扳手仍用原生 FACING 反向。管路连接、四面动力、碰撞、泵送能力和应力配置沿用现有逻辑。

`tools/create-booster-pump.cjs` 接收解压的 Create 源码根目录，生成资源及 `outputs/booster-pump/preview.png`。预览读取包含静态半轴的实际物品模型，展示水平正反面与竖直摆放。`textures.png` 左侧为自绘铜壳，右侧为加入原生轴孔的动力面；方向标记单独引用原版图集。

本次只进行静态资源检查和离线模型渲染，没有构建或启动游戏。
# 铜壳对称调整

铜壳底纹通过 `tools/symmetrize-fluid-housings.py` 使用原有铜色色阶重新组织连续像素簇，验证左右／上下镜像及 90° 旋转对称。原生 8×8 轴孔像素、流向标记和几何不变；这些原生细节仍有自身明暗方向。最新结果见 `outputs/booster-pump/textures.png`，既有三维预览未重新生成。未构建或运行客户端。
