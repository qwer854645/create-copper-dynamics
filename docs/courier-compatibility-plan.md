# 三种快递模组兼容测试与方案

## 已加入的兼容实现

- Couriers 2.3.1：到达目标前，若包裹携带 CCD 流体，强制走完整包裹交付，普通包裹不受影响。
- Phantom 1.0.3：包裹明细包含 CCD 物品与流体，数量使用真实 mB；其手动开包检查对含液 CCD 包裹返回阻止，避免由外部原生物品拆包路径丢掉流体。CCD 本身的安全开包行为不变。
- Mobile Packages 0.7.6：ETA 通知的图标列表包含 CCD 流体标记，标记名称附实际 mB，纯流体不再显示空内容。仅通知取样被替换，不向物理库存插入标记。
- 以上使用 @Pseudo 字符串目标 Mixin，未安装对应模组时目标不存在则跳过，不引入外部编译或运行依赖。
- 终端当前仍走原生物品身份订单路径，CCD 标记保持 1 单位 = 1 mB。没有将 FLUIDLOGISTICS 安装标志伪造为真，也没有让 CCD 标记进入旧版 CompressedTankItem 强制转换路径。桶级步进、JEI 流体配方转移与客户端显示仍未完成专项适配，不能宣称这部分已支持。

生产 GameTest 增加混合包裹预览数量及只读性验证。外部集成测试另断言 Couriers 流体包裹自动拆包开关被关闭、Phantom 流体明细完整、Mobile 保存与玩家交付内容不变。

## 测试范围与环境

2026-11-19；CCD 0.3.0-alpha.2 当前工作区、Minecraft 1.21.1、NeoForge 21.1.219、Create 6.0.10。
Mobile Packages 0.7.6、More Package Couriers 2.3.1、Create Phantom 1.0.3。
后两个为 Modrinth 发布 JAR，SHA-1 分别为 a4629f9222bd4222d63388de62a46679896078a1、d7880193cfd52bcaa70c73f502d7291876f3430c。

通过外部 Gradle init 脚本加载模组及临时 GameTest 源码，使用独立游戏目录，不向 run/mods 安装，不修改项目依赖。测试输入为装有 3500 mB 水的铜包裹，以及 3 个钻石、2500 mB 水、1000 mB 熔岩的黄铜包裹。

## 已运行结果

- Mobile、Couriers、Phantom 分别与 CCD 合装，以及三者同时与 CCD 合装，均通过 35 项原有 GameTest 和 1 项额外兼容测试，共 36 项。
- Mobile：调用实际 VirtualRobo 构造、NBT 保存与恢复，比较完整 ItemStack，纯流体及混合包裹均一致；实际 BeePortBlockEntity.sendPackageToPlayer 交付给测试玩家后，包裹组件完整保留。
- Couriers：调用实际 CardboardPlaneItem.withPackage/getPackage，通过飞机物品的 ItemStack 保存与读取，再调用真实飞行任务构造与 getPackage，两类货物均完整保留。此项不代表飞行到达流程已测试。
- Phantom：调用实际 MiniPhantomCargo.packageCopy 和 AirCourierHelper.deliverPackageOnly，两类包裹均保留完整数据。
- Phantom：调用实际包裹内容预览入口，纯流体包裹返回 0 项，混合包裹仅返回 1 项物品，未包含两种流体。确认显示适配缺口。
- Couriers 单独合装即报告 deploying/jar_plane 配方引用未安装的 create_factory_logistics:copper_jar_package_8x8；三者合装还报告便携终端配方引用不存在的 cmpackagecouriers:portable_stock_ticker。服务端仍完成测试；未做完全不含 CCD 的对照，不能直接断言原因在上游。

首次使用 makeMockServerPlayerInLevel 的测试触发 Create 向未协商连接发送 payload 的异常。改用 FakePlayer 后通过；这是测试方法限制，不作为产品兼容故障。

## 尚未实测

完整飞行、端口自动收发、跨维度、满背包／端口、途中区块卸载；客户端模型与通知；便携终端实际查询、数量调整和下单；Couriers 自动拆包的真实玩家交付。后者发布包字节码已确认直接调用 PackageItem.getContents，未读取 CCD 流体组件，存在明确流体遗漏风险，但尚未复现完整自动拆包丢液链路。

## 实施方案

1. 建立 CCD 共用包裹适配帮助类：识别所有铜／黄铜包裹尺寸，输出物品与流体的显示条目，提供流体库存标记与 mB 数量转换，以及完整包裹交付策略。保持现有数据格式。
2. Couriers 优先防止自动拆包遗漏流体：在只针对该模组的可选注入中，CCD 含液包裹到货时关闭 unpack 分支，交付完整包裹；普通 Create 包裹保留原逻辑。优先采用此小范围方案，不往全局 PackageItem.getContents 中伪造物品。
3. Phantom：在其包裹预览适配入口为 CCD 包裹补充流体标记及实际 mB 数量；完整包裹交付无需替换。手动打开接口需要单独验证，保留 CCD 铜包裹不可徒手拆液及黄铜包裹安全分装语义。
4. Mobile：保持现有机器人完整包裹运输；对通知内容补流体明细。便携终端与 Couriers 分别识别 CCD FluidStockItem，避免只依赖 FluidLogistics 专用分支；准确使用 1 单位 = 1 mB，提供桶级步进和精确数量。
5. Phantom 终端沿用原生 StockTicker，先测试原生查询与请求路径是否已支持 CCD 标记，只补缺失的单位／显示。不要预先替换可正常工作的订单逻辑。
6. 三个适配均按 modId 隔离，未安装对应模组时不加载其类，不引入必需依赖，不假扮 FluidLogistics。
7. 验收：普通包裹、全部 CCD 尺寸、纯流体、多流体混合、组件不同的同类流体；实际下单至拆包的数量守恒；自动拆包开／关；满库存、取消、退回、存档重载、离线玩家；单装和三者合装；客户端及服务端均验证。

建议顺序：Couriers 自动拆包保护 → 三者内容显示 → 三者终端数量与请求 → 完整运输回归。此文为方案，尚未加入生产兼容代码。

## 证据位置

测试日志位于 `C:/Users/Administrator/AppData/Local/Temp/opencode/ccd-courier-{mobile,couriers,phantom,all}.log`，最后补充交付的合装日志为 `ccd-courier-all-final.log`。临时测试源码为同目录 `ccd-courier-test-src/CourierCompatibilityTests.java`，加载脚本为 `ccd-couriers.init.gradle`。
