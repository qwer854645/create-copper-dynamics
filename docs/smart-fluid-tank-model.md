# 智能储罐黄铜外观

智能储罐完整继承 Create 6.0.10-280 的 24 个原生储罐模型，不新增几何、不改 UV、窗口位置、裁切面或物品显示变换。外框、顶盖和内壁统一采用黄铜色，玻璃仍保持原生颜色与透明度；包括两张玻璃图集中各四个铜色角点也一并改为黄铜，避免窗口残留铜色。

使用 pixel-art 工作流按整数像素将原生暖色像素映射到九级黄铜色阶，保留原生磨损和像素簇布局。暖色条件为 R≥G≥B、R−B≥20、G−B≥8，其余通道不变。所有 alpha、图集尺寸与连接纹理格位保持不变。共生成八张图：外框、顶盖、内壁及三者的 64×64 连接图集，加两张 16×16 窗口贴图。

原有模型只覆盖顶盖材质，现改为按原生模型实际使用的纹理别名覆盖外框、顶盖、内壁和窗口；库存物品继续继承单块窗口模型。窗口、库存、合罐拆罐及流体分层渲染代码不改。预览右侧仅用颜色示意三种流体，游戏内继续使用实际流体纹理与存量比例。

客户端 `SmartFluidTankModels` 在图集加载前注册外框、顶盖和内壁三个原生 `CTSpriteShifter`，全部使用 `AllCTTypes.RECTANGLE`。模型烘焙时为智能储罐状态包装原生 `CTModel` 与 `FluidTankCTBehaviour`。连接判定由原生 `ConnectivityHandler.isConnected` 比较实际控制器，不把邻近独立罐或普通储罐连起来；图集排列直接沿用原生资源。目标图集通过 `assets/minecraft/atlases/blocks.json` 加入实际方块图集，未修改原生全局精灵偏移。

生成器为 `tools/recolor-smart-fluid-tank.cjs`，参数为解压后的 Create 源码根目录。`outputs/smart-fluid-tank/preview.png` 展示原生单罐、黄铜单罐与三格高结构分层示意，`model.meta.json` 记录每张图的尺寸与改色数量。离线模型预览不模拟运行时 CT 烘焙；静态检查核对模型继承、纹理、透明度及连接图集注册，未构建、测试或启动游戏，运行时接缝仍需实测。

原生罐体模型、玻璃、内壁及所有图集归 The Create Team / The Creators of Create 所有。CCD 黄铜改色属于派生美术，不将上游资源标为原创或 MIT 美术。许可见 [Create LICENSE.md](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md)。
