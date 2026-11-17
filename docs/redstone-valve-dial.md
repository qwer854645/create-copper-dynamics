# 红石流体阀门表盘修正

用户报告仪表盘在游戏中显示为黑色。静态核对表明，现有四种方块模型、原生刻度贴图、指针 partial 和普通／Flywheel 渲染器引用均存在，没有发现可直接确认的缺失资源。原生模型把刻度底板与指针放在阀体内部，通过表壳透明区域显示；本轮针对这条内部显示路径做局部替换，黑色问题的具体运行时成因未在客户端复现。

CCD 的 `RedstoneFluidValveRenderer` 在表壳外侧绘制原生开／关刻度，以及原生 `FLUID_VALVE_POINTER`。双面表盘在本地 Y=1.99/14.01，指针深度为 1.98/14.02，避免刻度被阀体遮挡和指针与刻度重叠。刻度使用独立光照与无 AO 的 partial。

按用户后续要求，原传动轴口的两面改为整面铜板，直接引用 `create:block/copper_underside`。四种方块模型仅替换本体上原轴孔贴图对应的两个面，其余原生几何、表盘和流体口保留。渲染器改为继承 `SafeBlockEntityRenderer<FluidValveBlockEntity>`，停用继承机械阀门的轴体绘制及 `FluidValveVisual` 注册，以免在铜板外继续露出传动轴。表盘使用普通 BER 绘制，不受 Flywheel 是否开启影响。

指针仍读取原生 `LerpedFloat`，沿用原生面向、管轴判断和角度偏移。刻度随实际 `ENABLED` 状态切换，关闭端为红色，开启端为绿色；不替换红石执行器、开关时机或流体逻辑。表盘不是自发光，仍使用传入的环境光照。

物品模型保留原生显示变换和非轴体几何，移除原生 `Axis` 元素、封闭两面轴口，并保留外侧刻度与静态开启指针。方块状态、阀体尺寸、流体口、碰撞与管网逻辑保持原值。普通 Create 阀门不受影响。

所有刻度和指针美术直接引用 Create 资源，没有重绘为黑色占位。原始美术归 The Create Team / The Creators of Create 所有，许可见 [Create LICENSE.md](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md)。

`tools/preview-redstone-valve.cjs` 接收解压的 Create 源码根目录，生成两个表盘 partial、物品模型和 `outputs/redstone-fluid-valve/preview.png`。预览展示关闭、开启两端的实际几何与原生贴图；离线渲染不包含游戏环境 AO。仅完成静态检查与离线渲染，未构建或启动游戏，游戏内修复效果仍待确认。
