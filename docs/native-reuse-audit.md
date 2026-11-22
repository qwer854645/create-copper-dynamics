# Create／NeoForge 复用检查

检查基线：8eb23f8 后的当前工作区，Create 6.0.10、NeoForge 21.1.219。范围为 content、logistics、atomizing、client、compat 与 mixin 的主实现；检索完整源文件清单，重点逐段审阅设备状态机、液体所有权、交互、过滤、燃烧与渲染。不是对每个方法的逐行正确性证明，也不是第三方全组合运行验证。本轮仅检查，不修改业务代码，不重复运行既有测试。

## 优先收敛的复用缺口

### 1. 储液同步还没有统一使用原生节流机制

位置：`TankBlazeBurnerBlockEntity` 第 16–17、31–40 行，`BufferPumpBlockEntity` 的储罐回调，`OmniPumpBlockEntity` 第 21–23 行。

燃烧室在储液变化时立即 sendData，Liquid Fuel 按刻燃烧又显式 sendData；一个燃烧 tick 可能触发重复同步。缓存泵也立即同步，而万向泵只 setChanged，各设备同步策略不一致。

Create `SmartFluidTankBehaviour` 已有 whenFluidUpdates、sendDataLazily、syncCooldown／queuedSync、储液插值和序列化。建议优先复用其节流更新，或提取一个调用原生懒同步的适配，而不是各设备手写包节奏。

约束：燃烧室的实际 BLOCK 能力必须保持 SmartFluidTank，避免再次触发 Liquid Fuel 强转；原有 FuelTank／Buffer NBT 需要保留或明确迁移，不能直接替换行为后丢失储液。

### 2. 相邻流体能力查询可以复用缓存

位置：`AdjacentFluidTransfer` 第 12–15、26–28 行，`FaucetBlockEntity.inputHandler/tick`，`FluidHatchBlockEntity.access`，`FluidPackagerBlockEntity.targetFluidHandler`。

这些路径反复执行相邻 getCapability。项目内的 `CopperCollectionGrateBlockEntity.pushDown`、`CopperWaterSupplyBlockEntity.ensureCaches` 已使用 NeoForge BlockCapabilityCache，证明已有可沿用模式。

建议对固定邻居／固定面缓存，邻居能力失效、方块转向、实体移除和关卡变化时更新。不要全局永久缓存 IFluidHandler，也不要缓存每次查询会改变来源集合的桥接器快照。收益在于明确能力生命周期与减少重复查询，实际性能收益需测量。

### 3. 手持容器入口仍各自实现，容易产生不同交互

位置：`FluidHatchBlockEntity.exchangeWith`、`FluidAtomizerBlock.useItemOn`、`TankBlazeBurnerBlock.useItemOn`、`CopperBucketItem.useOn`。

已有路径分别使用 Create FluidHelper.tryEmptyItemIntoBE／tryFillItemFromBE、NeoForge FluidUtil.interactWithFluidHandler、tryEmptyContainer／tryFillContainer。建议抽一个共享交互入口，内部继续调用这些原生 API，统一过滤槽优先级、潜行、创造模式、失败反馈和权限检查。

不能机械地把所有入口改成同一个 API：舱口是非公开代理，细雪桶有专用能力，铜桶定义了潜行倾倒／普通抽取和创造模式保留规则。这里缺的是可复用策略层，不是缺少原生 API 的调用。

### 4. 朝向状态和渲染还存在多份转换公式

位置：`PumpOrientation`、`FrequencyPumpRenderer`、`RedstoneFluidPumpRenderer`、`BidirectionalFluidPumpBlock.oriented`、`TurningBoosterPumpBlock.oriented`，以及模型 blockstates／ROLL 生成器。

两种新泵已经共用点击轴姿态计算，但渲染和 JSON 仍单独写同一套旋转。其他双轴设备重复了从 front／right 或 front／branch 反算 ROLL 的算法。建议收敛为共享局部坐标框架，并让客户端与资源生成器对照同一组朝向约定；通用方块仍走 IWrenchable、KineticBlockEntity.switchToBlockState 和 Direction 旋转。

原生 PumpBlock.getRotatedBlockState 只翻转 FACING，不提供完整的 FACING+ROLL，不能直接删掉 CCD 姿态适配。镜像、结构移动也要单独验证，当前扳手专项通过不代表这些变换已经覆盖。

### 5. 相邻转移存在可合并的手写模拟／执行循环

位置：`AdjacentFluidTransfer.input/output`、`FluidTransmitterBlockEntity.transfer`、`FaucetBlockEntity.tick`、`FluidTransfers`。

前三处重复模拟抽取、模拟接收、执行、处理剩余。NeoForge FluidUtil.tryFluidTransfer 已提供标准转移流程，但其实现不会把执行时目标少收的余量返还来源。因此建议普通合规接口使用原生流程；需要守恒恢复的路径共用一个明确的 CCD 扩展，而不是完全替换成 FluidUtil。

尤其 AdjacentFluidTransfer 的返还可能被单向接口拒绝，当前未检查返还的实际接收量。这是独立正确性风险，应在合并时用有限恢复缓存或明确返回剩余所有权解决。现有正常接口测试不能证明违规／变化接口下无损。

### 6. 流体代理可复用包装器的基础部分

位置：`FluidAccess.handler` 第 63–124 行，`FluidHatchBlockEntity.Proxy`，`OmniPumpBlockEntity.handler`。

Create CombinedTankWrapper 已实现多 handler 的虚拟槽位和基本 fill/drain；多个 CCD 代理又手写 getTanks、槽位定位和委托。建议评估只复用槽位委托层，过滤、流量预算、生命周期校验与重入保护保留自定义。

桥接器会去重同一原生储罐控制器，并动态解析两端；直接包装后容易重复计算容积／绕过过滤。单向端口、万向泵共用预算也不是原生组合包装器负责的功能。

### 7. 动画状态可以进一步复用 LerpedFloat

位置：`VariableFlowPumpBlockEntity.previousSignalDial/signalDial`、`FlowMeterBlockEntity.previousDial/dial`，客户端 renderer 中重复的插值处理。

红石泵和原生燃烧室已使用 Catnip LerpedFloat；另两种表盘自行维护前帧、当前帧和追赶。可用 LerpedFloat 的 chase／tickChaser／getValue(pt) 统一生命周期，保留信号表与实际流量表不同的目标值定义。它是小型维护改进，优先级低于同步与守恒。

## 已经复用，不应误判为重复造轮子

- 移液臂继承 ArmBlockEntity，保留阶段、抓取／投放动画、分配策略、红石锁和原生目标表示；FluidArmPoint 的 mB 载荷、舱口限制与燃烧室特判是流体适配。
- 仪表继承 FactoryPanelBehaviour／FactoryPanelScreen，复用网络、订购承诺和 ValueSettingsPacket；mB 数量与库存凭证转换是必要差异。
- 重打包机继承 RepackagerBlockEntity／PackageRepackageHelper，完整订单扫描走原生；混合流体货物合并、地址一致性及重复碎片检查有独立价值。
- 普通集液栅使用 FluidDrainingBehaviour 增量抽液、SmartFluidTankBehaviour 和 FluidUtil 转移；细雪是无 FluidState 的固体，需要 PickupHandler。
- 雾化器继承 EncasedFanBlockEntity／AirCurrent，原生物品处理仍通过 FanProcessing／RecipeApplier；自定义付费周期和脚本规则不属于原生风扇功能。
- 流体过滤器继承 FilterItem，交互使用 FilteringBehaviour／SidedFilteringBehaviour，界面使用原生背景和控件。原生 ItemAttribute 面向物品，不能替代温度、密度、流体标签及组件比较。
- 智能储罐复用 FluidTankBlockEntity／ConnectivityHandler，但原生单流体库存不支持多流体共享总容量。换成若干独立固定容量 FluidTank 会改变容量语义，无法直接替换 SmartTankInventory。
- 缓存／万向／创造端点通过 NativeEndpointPump 复用原生 distributePressureTo，由原生 FluidNetwork 搬液；没有恢复每刻管道 BFS。临时适配器是内部方法复用的折衷，并不是 Create 提供的正式端点压力 API。
- 燃烧室继承原生燃料和供热，苔化调用原生 performBonemeal，超热介质继承 LavaFluid，图形复用原生 PartialModel／OBJ；自定义高度变换属于所需外观。
- PnP 接管后原生压力通路禁用，专门的边界／拓扑钩子是两个引擎间适配，不能换成普通 Create 泵声明。

## 额外发现，单独处理

- LiquidFuelCompat.find 每次查找重复 Class.forName/getMethod，可缓存方法句柄；燃料数据本身须随重载变化，不能永久缓存 Fuel 结果。这是性能和兼容改进，不是原生 Create 功能缺口。
- BulkGluingType.result 每次扫全部工作台配方并尝试 72 个格子排列。可按 RecipeManager／重载版本建立候选索引，但必须继续调用真实 matches／assemble 和检查容器余物。
- 多个历史生成脚本能把已修正的配方、提示和模型写回旧版。应明确权威生成入口或拆分生成器，不要仅因存在生成脚本就重新运行全部脚本。
- 输出过滤当前主要钩住 RecipeApplier，自定义粘接直接返回结果，因此不能把“所有类型都支持产物过滤”作为已验证结论。应补跨类型契约测试，再决定过滤前置或统一处理入口。

## 建议执行次序

先处理同步节流和相邻转移余量所有权；接着统一容器交互与能力缓存；再收拢朝向／渲染和代理委托；最后优化动画、反射查找及配方索引。每轮保留原有交互和 NBT 约定，分别验证基础 Create、PnP、Liquid Fuel 及更多触媒环境。不要把所有自定义实现一次性换成原生类。
