# 智能集液栅：铜槽体、黄铜顶框与前侧过滤面板

共享已认可的半格集液斗轮廓：逐级收窄的槽体、下沉一像素的灰色栅条、深灰内衬和 Create 原生底部排液口。槽体和底部保留普通版铜色，铜面沿用已认可的 CCD 增压泵布局与色阶；只有四个顶部边框元素的所有面改为黄铜，并保留原生黄铜过滤面板。灰色构件继续使用 Create 原生工业铁材质。生成器从普通版元素克隆出同一几何，仅调整顶框的材质引用，不复制维护另一套轮廓。

前侧安装 5×5 的方形过滤面板，范围 [5.5,2,14]..[10.5,7,15.5]，不超过半格高度或方块边界。面板使用本地 Create 6.0.10-280 `smart_fluid_pipe/block` 第三个元素的原生过滤边框 UV，纹理直接引用 `create:block/smart_pipe_2`，重新配置为朝南的直立安装，不自绘替代图案。板顶低于顶框顶面一像素，不遮挡顶部栅口。

`SmartCollectionGrateBlock` 增加原生水平 FACING 属性，放置时朝向玩家；结构旋转/镜像使用原版 Rotation/Mirror，扳手旋转仍调用继承的 Create `IWrenchable` 流程，只提供水平转向结果。方块状态为四个朝向各配置两个独立 multipart 部件：槽体和面板一起旋转，POWERED 不改变外观。物品模型合并相同的槽体和面板。

过滤继续使用 `FilteringBehaviour.forFluids()`，显示与命中改用原生 `ValueBoxTransform.Sided`，只有 FACING 所指一面有效。默认朝南时图标和命中中心为 [8,4.5,15.5625]，紧贴面板表面中心 [8,4.5,15.5]，缩放 .25；四向偏移和旋转由原生 Sided 变换处理，不额外实现过滤点击事件。选取、碰撞、交互轮廓将父类槽体与面板合并后通过 Catnip `VoxelShaper.forHorizontal` 一同旋转，栅条方向也与模型一致。

容量 2000 mB、区域搜索、雨雪与滴液收集、流体过滤校验、红石暂停、管道和容器存取、液面绘制、配方及掉落均沿用现有实现。没有新增库存或扫描任务。普通版模型和材质不受智能版生成器影响。

生成器为 `tools/create-smart-collection-grate.cjs`，参数为本地 Create 源码资源根目录，沿用基础版输出的 CCD 贴图。`outputs/smart-collection-grate/preview.png` 展示正面、存液和背面，`comparison.png` 为两版同角度比较；过滤槽未设置物品，游戏中由原生渲染器显示实际过滤图标。存水只为离线示意，临时水面模型在 finally 中删除。

本次仅检查源码和模型、材质、朝向及命中数据，并进行离线预览；未编译、运行测试或启动客户端。黄铜顶框改色基于 CCD 自绘铜面；工业铁、内衬、原生过滤面板 UV、smart_pipe_2 及管口资源属于 The Create Team / The Creators of Create，不标为 CCD 原创或 MIT 美术。离线预览中的原版水纹属于 Mojang。
