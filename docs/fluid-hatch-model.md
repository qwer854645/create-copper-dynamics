# 流体舱口模型

普通流体舱口保留 Create 6.0.10-280 物品舱口的三个原生几何构件、安装轮廓和物品显示变换，将框架换成铜色，并在原生斜面中央加入一个朝外的流体管口。管口直接使用原生 `fluid_pipe/rim_connector/south` 与 `fluid_pipe/rim/south` 的几何及 UV，整体绕 X 轴旋转 -45°，端面中心位于模型坐标 [8,11.4,5.9]。内嵌斜面不处于方块边界，因此移除管口面的邻块裁切标记，避免邻块遮蔽内部接口。

`frame.png` 是原生 32×32 `hatch.png` 的铜色变体，使用 pixel-art 技能按整数像素改色；保留原生深色凹槽、下方小金属构件及全部 alpha。框架的磨损和像素布局沿用原生资源，没有增加铆钉。斜面铜板复用 CCD 已有的 16×16 `faucet/body` 贴图。

管口侧壁与端面直接引用原生 `pipes.png`，始终露出深色管孔，不使用内盖、开合贴图或舱门动画。移除原来的灰色内盖派生贴图与开启模型。

普通版和智能版在手持容器交互成功后只播放原生音效，不切换 OPEN，不安排 10 tick 复位。为继续继承 ItemHatchBlock，保留父类构造要求的 OPEN 属性，但它不参与 CCD 的交互和模型选择；方块状态资源只按四个水平方向选择固定模型。含水、碰撞、安装、扳手、手持容器取放及后方容器访问逻辑继续沿用既有实现。普通版物品模型继续继承固定管口模型。

智能版暂不进行结构改造：固定使用原有黄铜机壳与过滤面板几何，过滤坐标及渲染不动；同样取消开合反馈，移除其开启模型。

生成器为 `tools/create-fluid-hatch.cjs`，参数为解压后的 Create 源码根目录。`outputs/fluid-hatch/preview.png` 展示原生舱口、CCD 正面和侧面，`textures.png` 展示铜色框架图集，`model.meta.json` 记录资源来源与变换。仅静态检查与离线渲染，未构建、测试或启动游戏；离线预览不是游戏内验证。

原生舱口、管口几何及贴图归 The Create Team / The Creators of Create 所有。上述改色和内盖像素属于派生修改，不将上游美术标为 CCD 原创或 MIT 资源；许可见 [Create LICENSE.md](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md)。
