# 移液臂铜色试版

按用户要求，大面积将原生动力臂的上下臂节及夹爪外壳换成铜色。底座与旋转底座保持原生黄铜，这是用户在试制过程中的明确补充。使用 pixel-art 技能按像素替换暖色区域，保留原生几何、UV、分辨率、透明度及像素布局；灰色关节、金属接触面、传动齿轮、升降滑轮材质与护目镜纹理不改。

CCD 的 `mechanical_arm.png` 是对应 Create 6.0.10-280 原始 32×32 贴图的铜色变体。仅处理暖色像素（R≥G≥B、R−B≥20、G−B≥8），按原亮度映射到九级铜色，非暖色和全部 alpha 保持逐字节一致；具体映射与改色数量见 `outputs/fluid-arm-copper/palette.json`。

静态底座和旋转底座完整使用原生模型及材质。六个臂节、夹爪动态 partial 分别继承原生模型，仅覆盖暖色改铜的图集。物品继承原生显示变换，保留全部几何参数，只为 `Base` 与 `Joint 1` 两个元素的图集引用增加原色别名，确保物品里的底座也不被改铜。没有复制或重写关节、拾取、搬运、爪端持物的动作。传动齿轮仍完整引用原生 `ARM_COG`，保留其原始金属／黄铜颜色。

`FluidArmModels` 在烘焙前持有并注册这六个 partial。两个仅客户端生效的 Mixin 在原生 `ArmRenderer.renderArm`、`ArmVisual` 构造与护目镜更新时替换局部模型读取，且只在方块是 CCD 移液臂时生效，不修改 Create 的全局 partial 字段，普通动力臂保持原样。底座 `ARM_BASE` 与齿轮 `ARM_COG` 不注入、不替换。Visual 构造的注入点不含父类构造前的齿轮表达式，访问方块状态时父类已经初始化。沿用现有 renderer/visual 注册与持物绘制，地面／倒挂、护目镜切换和原生动画继续由 Create 处理。

`tools/recolor-fluid-arm.cjs` 接收解压后的 Create 源码根目录，生成贴图、材质继承模型和 `outputs/fluid-arm-copper/comparison.png`。预览使用原生部件与 `ArmRenderer` 的关节变换次序，展示原版、铜色版和带护目镜的反向视角；为空爪离线静止姿态，不代表游戏运行截图。本次仅静态检查与离线渲染，未构建或启动游戏。

原始模型、贴图和护目镜美术归 The Create Team / The Creators of Create 所有，本项目只进行了所述材质改色，不标注为原创上游美术；许可见 [Create LICENSE.md](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md)。
