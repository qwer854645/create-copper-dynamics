# Create 原生机制复用说明

本轮依据实际依赖的 Create `6.0.10-280` 源码包实现，未使用其他流体附属的实现代码。

## 包裹地址与剪贴板接入

- 实际阅读 `PackageItem` 的地址组件读写与 `matchAddress`、`PackageEntity.interact`、`PackagePortBlockEntity.addAddressToClipboard`、`AddressEditBoxHelper`、`ClipboardContent`、`ClipboardEntry` 和 `ClipboardValueSettingsHandler`。没有读取其他三个流体附属的实现代码。
- 原生包裹类型通过 `instanceof PackageItem` 识别，CCD 流体罐直接继承，已有地址组件与通配符匹配无需额外注入。原生剪贴板设置复制入口针对方块实体，包裹实体交互则只支持空手取回；因此不能把“继承包裹”视为已经具备剪贴板地址粘贴。
- 沿用原生 `ClipboardContent` 与 `#地址` 条目格式，复制出的地址仍能被原生 `AddressEditBoxHelper` 用作地址建议；原生蛙港写入的地址簿也能作为 CCD 的粘贴来源。剪贴板的其他页面、图标、只读标记及已复制机器设置予以保留。
- 仅在 `FluidPackageItem.overrideOtherStackedOnMe` 接入标准物品槽点击：鼠标持剪贴板时右击复制、左击粘贴，方向与原生机器设置剪贴板的右击复制／左击粘贴一致。由标准菜单处理客户端预测和服务器同步，不新增数据包或实体交互 Mixin。
- 当前页唯一未勾选的地址决定粘贴目标；多个不同候选时拒绝修改。单独的 `#` 是 CCD 增加的显式清除标记，原生地址建议会忽略该空地址条目。原生并没有“对包裹点击粘贴地址”这一入口，这部分为 CCD 自行实现的交互。
- 本轮只做源码和资源静态检查，没有编译、游戏测试或客户端运行。

## 流体雾化器与可选 KubeJS

- 已核对 Create 6.0.10 的 `EncasedFanBlock`／实体、`IAirCurrentSource`、`AirCurrent`、加工类型注册表与 `FanProcessing`、原生转子渲染／Flywheel Visual、药水流体及原版药云的效果应用方式。核对 KubeJS 2101 分支插件发现、绑定、服务端脚本加载生命周期，并确认固定编译版本 `2101.7.2-build.382` 的 Maven 元数据与依赖。
- 方块／实体直接继承鼓风机，内部罐用 `SmartFluidTankBehaviour`；渲染与 Flywheel Visual 直接注册原生实现，保留原生框架开孔与转子。只替换侧板铜色材质，避免不透明材质封住原生风口。
- 自定义 `AirCurrent` 只扩展加工类型查询并在原生处理调用期间传递机器上下文，不复制气流搜索、遮挡、实体移动、传送带遍历或产物转换。沿途原生加工媒介继续优先；生物作用复用原生气流已发现且仍处于范围内的实体，避免重复区域扫描。
- 数据规则注册为标准配方类型，使用 NeoForge `FluidIngredient`、原生 `Ingredient`／`ItemStack`／`MobEffectInstance` codec 和带注册表的网络 codec，可由数据包和 KubeJS event.custom 同样提供。选择规则按优先级与 ID 稳定排序，流体身份／规则重载失效缓存，不每 tick 扫描全部配方。
- 自定义物品加工注册一个真实的 Create `FanProcessingType`，其 `isValidAt` 始终为 false，只有雾化器自身选择它。两处定点 Mixin 只在该类型与当前雾化器上下文同时成立时初始化原生计时；普通鼓风机与原生加工类型不受影响。换液／规则重载更换上下文键，原生进度不能跨定义错误复用。
- KubeJS 仅 compileOnly，不作为必装运行依赖；插件由 `kubejs.plugins.txt` 按 KubeJS 原生方式发现。核心代码不引用 KubeJS 类型，服务端绑定提供 CCDAtomizer，beforeScriptsLoaded 清理旧回调。回调上下文只提供副本和值，库存扣液与物品／生物结果提交由 CCD 控制。
- 本版数据规则、字段、脚本结果语义和重载方式详见 `fluid-atomizer.md`。没有启动期脚本动态注册任意新的 Create 类型；现有类型映射和共享适配器覆盖首版用途，重载不修改冻结注册表。
- 已补齐模型、注册、能力、应力、配方、普通掉落、工具标签、中英文说明、可选依赖声明与数据包／KubeJS 示例。只做静态检查，未编译、运行或验证脚本／视觉／性能。

## 铜桶

- 已核对实际 NeoForge 依赖中的 `FluidHandlerItemStack`、`SimpleFluidContent`、`FluidUtil`、`BucketPickupHandlerWrapper`、`DynamicFluidContainerModel` 与能力注册／模型加载注册，以及原版 `BucketItem`、Create 的 `FilterItemStack`、`GenericItemEmptying`／`GenericItemFilling`。未读取三个流体附属的铜桶实现。
- 直接注册原生 `FluidHandlerItemStack` 作为物品流体能力，使用原生 `SimpleFluidContent` 的持久化与网络 codec，不另写储液增减、模拟、组件比较或空桶转换。容量 4000 mB，单流体且单物品，原生填充／抽取与过滤样本识别因此可直接使用该能力。
- 普通接口存取使用 `FluidUtil.tryFillContainer`／`tryEmptyContainer`，桶不可堆叠所以无需额外物品归还／拆堆逻辑。通过 NeoForge `onItemUseFirst` 在方块之前进入现有 `useOn` 路径，只在服务器执行真实转移，并在生存和创造模式下均更新返回的容器 ItemStack；遵守点击面的能力和玩家操作权限，接口拒绝时不继续在世界放液。已核对客户端 `MultiPlayerGameMode` 与服务端 `ServerPlayerGameMode` 的实际调用顺序；这是为避免 Create `FluidHelper.tryFillItemFromBE` 的创造模式分支消耗来源却不返还装好容器，未改动全局流体助手或复制其逻辑。
- 世界放液直接调用 `FluidUtil.tryPlaceFluid`，每次提供 1000 mB，沿用含水、蒸发、可放置性和原生声效处理。先检查实际数量及操作权限，失败不更新桶内内容；不手写流体方块放置或维度蒸发规则。
- 世界取液仅为源液体，先模拟确认完整 1000 mB 可装入。标准 `LiquidBlock` 直接移除源方块并存入其真实流体类型；原生含水方块委托 `BucketPickup`。这是必要的窄适配：NeoForge 的 `BucketPickupHandlerWrapper` 要求 pickup 返回物品是 `BucketItem`，而没有对应桶物品的源液体不能满足这个条件。既不依赖桶物品 ID，也不从流动液体虚构整桶量。
- 渲染使用原生 `neoforge:fluid_container` 加载器、桶形基础纹理和流体遮罩，颜色委托原生 `DynamicFluidContainerModel.Colors`，只为桶壳增加铜色着色。配置实际注册了模型加载器名字，未自行实现动态物品绘制或为每种流体生成模型。
- 新增单一物品、单一流体组件、创造栏、配方及中英文内容提示；不加入旧存档迁移。仅做静态检查，未构建、运行或验证世界交互和视觉效果。

## 铜集液栅

- 已阅读原生软管滑轮的实体／流体接口、`FluidDrainingBehaviour`／`FluidManipulationBehaviour`、排液口、`SmartFluidTankBehaviour`、工作盆侧向输出及 `DirectBeltInputBehaviour`。同时从实际 NeoForge 依赖源码核对原版 `CauldronBlock`／`AbstractCauldronBlock`、滴水石锥的炼药锅寻址与调度，以及 `FluidUtil.tryFluidTransfer`。未读取其他流体附属的实现代码。
- 方块继承原版 `CauldronBlock`，覆盖存液接收与手持交互而不让原生桶逻辑替换方块；继承雨水概率与滴水石调度，不用自写随机滴液扫描。`codec` 保持父类的 `MapCodec<CauldronBlock>` 返回类型，工厂仍构造集液栅。环境接收量复用 Create 的 `CauldronSpoutingBehavior.CAULDRON_INFO`。
- 实体继承 `SmartBlockEntity`，2000 mB 单罐直接使用原生 `SmartFluidTankBehaviour`，沿用其完整组件保存、流体组件同步、8 tick 同步节流与液面追踪。区域抽液直接注册 `FluidDrainingBehaviour`；仅覆盖 tick 调度，以每 5 tick 的位置错峰时机推进原生增量搜索，且要求缓冲有整桶空位、入口液体与缓冲兼容。不重写区域队列、搜索方向、范围或无限判定。
- 工作入口为方块上方一格，入口位置或流体变化时重建原生搜索上下文。先模拟完整 1000 mB 入罐，再调用原生真实抽取，满罐时不调用区域抽取。只承接原生对源方块／含水方块／流动液体清理的处理，不把每段流动液体都计算成新的一桶。
- 底部出口使用 NeoForge `BlockCapabilityCache`，每 5 tick 通过 `FluidUtil.tryFluidTransfer` 执行标准模拟／抽取／填充；目标遵守面限制、过滤及剩余容量。出口移动或维度变化后重建，失效时释放引用。转移基于标准流体接口契约，未增加独立恢复库存或新的异常包裹逻辑。
- 工作盆用 `DirectBeltInputBehaviour` 判定侧向输出目标，因此注册该原生行为并仅允许水平方向发现；不注册物品能力，原生默认物品插入返回原物品，纯流体输出仍通过标准流体能力接入。手持交互复用 `FluidHelper`，潜行入口扩展现有交互钩子。
- 外壳使用底板、边框和栅条实体模型，真实孔洞下方使用 Catnip 原生流体绘制接口与储罐行为的液面动画。补齐注册、配方、自身掉落、镐标签、中英文提示和比较器。未增加旧存档迁移。仅做静态检查，未构建、运行或实测视觉／性能。

## 智能集液栅

- 先核对 Create 的 `SmartFluidPipeBlockEntity`、`FilteringBehaviour`、`ValueBoxTransform` 和 `SmartBlockEntityRenderer`，参考本项目智能舱口、智能龙头对原生过滤与管道更新的接法。新方块／实体分别继承基础集液栅，开放实体类型构造参数，不复制搜索、储罐、环境收集或排液实现。
- 基础库存通过 NeoForge `FluidTank.setValidator` 接入可覆盖的 `acceptsFluid`，智能版只将其委托给原生 `FilteringBehaviour.test`。标准能力、内部区域抽液及环境收集共用该校验器；区域搜索前也过滤入口，避免不匹配时仍调用原生的流动液体清理。
- 原生 `.forFluids()` 处理容器／过滤器、过滤数据保存、空过滤接收全部和实际过滤器掉落。过滤变更仅使能力失效并通知相邻原生泵／管路重新处理，不修改库存，不限制旧液抽取。顶面过滤槽使用原生值设置交互；基础绘制器改为继承 `SmartBlockEntityRenderer` 并调用父类，使两版共用原生过滤图标和原有流体绘制。
- 自动工作钩子同时覆盖搜索行为 tick、抽取／主动排液 tick 和环境回调；收到红石时暂停这些路径，保留受过滤约束的被动输入和正常抽取。POWERED 状态在初次初始化与邻居事件中更新，沿用每 5 tick 的错峰工作和底部能力缓存。
- 已补齐注册、原生渲染器注册、继承基础几何体的黄铜材质模型、升级配方、本体掉落、镐标签及中英文说明。未加入旧存档迁移，仅做静态检查，未构建或运行游戏。

## 铜供水箱

- 已核对实际依赖 NeoForge 的 `BlockCapabilityCache` 创建参数、有效性与失效回调约束，以及 Create 的机械合成配方格式。供水调用标准有向 `IFluidHandler.fill`，先模拟再执行，不另写管道压力或网络搜索。
- 方块和实体分别继承铜水槽对应类，只开放实体类型构造参数。固定纯水库存仍继承原生 `CreativeSmartFluidTank`；手持操作、原生同步／移除和六面被动抽水完全共用。封闭箱体不需要内部流体渲染器，不新增动画或逐 tick 渲染更新。
- 每个方向持有一个 `BlockCapabilityCache`，缓存包含 null 能力。失效回调只唤醒供水时间，不查询世界或立即重建；位置或维度改变时重新建缓存，移除时释放引用。回调有效性检查方块实体与原位置，缓存按 API 自动处理区块卸载，不强制加载。
- 每 5 tick 运行一次六面供水，无接收量则延长至 20 tick；没有每 tick 的邻居能力扫描。供水轮次中检查原生控制器身份，CCD 舱口代理通过现有解析器识别实时后方容器，防止重复配额；空目标不进行额外方块实体查询。
- 配置 `[waterSupply] rate` 默认 1000 mB/t，每轮配额按 5 tick 折算，0 关闭，-1 取消本机配额。无限模式汇总可接收水的槽位余量后有限注水，支持总容量超过 int 的多槽接口，并在目标只接受部分时停止本轮，避免对吞液口或限流接口无界循环。原生创造罐及 CCD 无限源直接跳过。
- 注册了方块、物品、能力、5×5 原生机械合成配方、普通本体掉落、镐标签与中英文护目镜信息。仅做静态检查，未编译、运行或进行性能实测。

## 铜水槽

- 先阅读原生 `FluidTankBlock` 的创造模式手持容器交互、`FluidTankBlockEntity` 的构造／连接／读写流程、`CreativeFluidTankBlockEntity.CreativeSmartFluidTank`、`SmartFluidTank`、NeoForge `FluidTank` 及 `FluidTankRenderer`。
- `CopperWaterBasinBlock extends FluidTankBlock` 并启用原生创造储罐交互；实体继承 `FluidTankBlockEntity`，内部罐继承 `CreativeSmartFluidTank`，fill 与两个 drain 重载保持原生实现：输入直接销毁，抽取只模拟而不消耗。实体使用普通父类，是因为 `FluidHelper` 对 `CreativeFluidTankBlockEntity` 特判为不消耗手中容器，放到生存机器会复制桶／瓶；普通实体既保留原生全部储罐行为，也让取放正常消耗和归还容器，无需重写交互流程。
- 扩展仅固定纯水与 1,000,000 mB 容量：覆盖 `setFluid`、`setContainedFluid`、`readFromNBT` 和容量设置。特别处理原生手持取水后的清空调用、倒液后的换液调用，不允许这些入口改变内部流体。构造期直接初始化水，不在父类尚未初始化时触发更新回调。
- 六面能力直接提供该原生库存；无需重新实现抽灌、同步节流、光照／比较器。普通右键只调用 `FluidHelper.tryFillItemFromBE`，潜行右键只调用 `tryEmptyItemIntoBE`，不在失败后切换模式，防止普通取水误销毁已有内容。桶、瓶、标准流体容器、单次倒液量与余液保留均复用原生实现。满容器或不接受水的流体容器拦截后续倾倒行为，接口输入销毁仍走原生 fill。扩展现有 `FluidHatchInteraction` 的限定方块匹配，让潜行持容器仍进入方块交互，遵守事件拒绝且不拦截扳手。连接更新停用以保持单方块，锅炉停用，开口容器不切换窗口。渲染器直接注册原生 `FluidTankRenderer`，外壳使用五个实心模型构件留出开口，避免拿不透明纹理覆盖原生透明孔洞。
- 合成、创造栏、普通本体掉落、镐标签、中英文护目镜文本已注册。不写水量到掉落物，不添加拆除补偿或旧存档迁移。只做代码及资源静态检查，未构建或运行游戏。

## 智能储罐

- `SmartFluidTankBlock extends FluidTankBlock`、`SmartFluidTankBlockEntity extends FluidTankBlockEntity`、`SmartFluidTankRenderer extends FluidTankRenderer`，物品也使用原生 `FluidTankItem`。方块状态、碰撞、放置、窗口扳手操作、声音、比较器入口、实体 tick、位置变化、同步节流及窗口布局沿用父类，不重新实现储罐方块体系。
- updateConnectivity 直接包装调用 super.updateConnectivity，父类更新路径继续调用原生连接器；仅在原生控制器设置／移除回调中执行多流体库存转移。正常合罐集中到控制器并清空成员原库存，拆分时按原生遍历顺序分配到独立储罐，再由连接器重新组罐。原生单流体迁移分支关闭，防止只转移第一种流体；其兼容字段不拥有实际液体，也不写入 TankContent。
- `SmartTankInventory` 是控制器唯一实际库存；所有成员的 `SmartTankClusterInventory` 仅转接到它。共享容量由原生尺寸乘单罐容量计算。类型和完整组件相同时合并，普通填充检查种类数和共享余量，指定抽取跨同身份的存储项扣除。合罐转移不受构建期间的临时单块容量限制，最终容量取父类结构尺寸；拆分按剩余容量分配，超量消失，不会产生第二份资源。
- 只在控制器写完整 Fluids；成员使用父类原生 Controller 结构引用，尺寸、窗口及位置等字段也由父类读写。不再写独立的 Width／StorageVersion 或添加旧版分块存档迁移。蓝图安全数据不包含实际液体。
- 拆除顺序与原生一致：先从世界移除被拆成员，再处理剩余结构；按其剩余容量保留液体，超出部分直接销毁。该主动移除路径与原生一样访问原结构成员，不额外生成包裹。移动结构已携带控制器 NBT 时不同时向原地分配，避免双份库存。控制器查找与区块访问复用父类行为，不再添加独立加载范围限制或缺块修复。
- 原生窗口布局和全部 24 个模型变体保持使用；继承渲染器只替换单流体绘制为多层真实流体。总占用比例提供给父类比较器逻辑；选择最高发光流体交给父类光照更新。锅炉仍未适配多流体，保持停用，不将其他流体当作水使用。
- 重新核对了父类构造期虚方法、protected 字段与 public write 签名、控制器 nullable 约定、移除回调和原生连接器。仅做静态检查，未构建或运行游戏。

## 流体舱口

- 先阅读 `ItemHatchBlock`／`ItemHatchBlockEntity`／`ItemHatchHandler`／`HatchFilterSlot`、`FluidTankBlock`／`FluidTankBlockEntity`、`FluidHelper`、`GenericItemEmptying`／`GenericItemFilling`、`PortableFluidInterfaceBlockEntity`、`FluidPipeBlock`／`FluidPropagator` 和 `IHaveGoggleInformation`。仅使用 Create 源码及 NeoForge API，没有读取三个流体附属的实现代码。
- `FluidHatchBlock extends ItemHatchBlock`，直接复用水平侧面安装、含水状态、形状、扳手行为、10 tick 定时关闭及移除方块实体逻辑。模型继承原生舱口几何体，铜色框架通过材质覆盖实现，指定 `cutout_mipped` 渲染层。
- `FluidHatchBlockEntity extends ItemHatchBlockEntity`，普通版覆盖 `addBehaviours` 和 `clearContent`，不创建原生物品过滤行为、物品库存或流体储罐。原生护目镜的 `containedFluidTooltip` 显示的是后方容器实时内容。
- 通过 NeoForge `Capabilities.FluidHandler.BLOCK` 注册正面及无方向的代理；每次操作查询背后容器朝向舱口面的能力。`FACING` 沿用原生定义，指向容器；正面与被查询容器面均为其反方向。代理保留填充／抽取、模拟／执行和流体组件，不调用无方向的后方接口来绕开面限制。
- 手动取放直接调用原生 `FluidHelper.tryEmptyItemIntoBE`／`tryFillItemFromBE`，优先灌入，再尝试取出，兼容其玻璃瓶、药水及标准流体容器路径。原生取出方法只检查显示存量、不模拟源端抽取，因此该次交互期间根据 `GenericItemFilling.getRequiredAmountForItem` 取得本次容器所需数量，仅向它显示经精确 `drain(SIMULATE)` 确认可取出的流体，避免绕过只读、限流或定量抽取限制；普通能力读取和护目镜仍显示原始库存。临时容器副本只在该次调用期间使用，finally 清除，不存档、不对外提供库存。执行偏离模拟等接口违约情况没有另建缓冲或事务机制。
- 原生 `ItemHatchHandler` 只匹配 Create 注册的普通舱口，因此增加限定 CCD 舱口、流体容器且未明确禁止方块交互的潜行交互钩子；扳手操作不拦截。手动交互沿用原生不接受 FakePlayer 的规则，自动化通过流体能力操作。
- 稳定代理重新解析后方目标，不保留过期容器能力；逐方向代理每次复核朝向，旋转时也调用原生能力失效通知。每次查询检查区块加载及实体有效性，线程内调用链最多 64 层并拒绝重入，防止舱口或第三方代理循环递归。
- `FluidInventoryBehaviour` 在取得物流库存身份前解析 CCD 舱口链，再沿用 Create 储罐控制器归一化，避免经舱口重复统计同一库存。第三方自有代理的身份归一化范围没有扩大。
- 已补齐注册、创造栏、模型、配方、掉落、镐标签及中英文提示。仅做静态检查，没有构建或游戏运行结论。

## 智能流体舱口

- 阅读原生 `SmartFluidPipeBlockEntity`、`FilteringBehaviour`、`FilterItemStack`、`HatchFilterSlot`、`ValueSettingsInputHandler` 和 `SmartBlockEntityRenderer`，直接接入 `FilteringBehaviour.forFluids()` 与 `HatchFilterSlot`，不另写过滤匹配、配置网络或保存格式。父类构造期间的行为注册使用继承自 `ItemHatchBlockEntity` 的 `filtering` 字段，避免子类字段初始化覆盖行为实例。
- `SmartFluidHatchBlock`／`SmartFluidHatchBlockEntity` 继承普通舱口，父类增加带类型的构造及流体接收、按数量抽取扩展点。公共流体能力在 `isFluidValid`、`fill` 和指定流体的 `drain` 入口统一检查过滤；不指定流体的 `drain` 遍历匹配罐，先模拟再执行指定流体抽取。普通版保留直接委托的行为。
- 智能版未配置过滤时明确拒绝进出，这是相对原生空过滤器的唯一默认语义调整。读取库存仍返回后方真实数据，保留物理库存身份去重；手动取液所使用的临时视图同时检查过滤和精确抽取量，防止原生装瓶方法绕过限制。模拟和执行均不通过无过滤的后方能力提供额外入口。
- 过滤变更失效能力缓存并从正面相邻实际管路传播压力变化；紧邻泵时使用其 `updatePressureChange`。旧代理每次仍检查当前过滤，移液臂到达前改变过滤后也必须重新验证。串联舱口逐层执行各自过滤，不绕开中间代理。
- 移液臂的舱口类型检查和物流身份解析使用基类 `instanceof`，因此智能子类直接兼容默认舱口交互限制和实际容器身份归一化。客户端注册原生 `SmartBlockEntityRenderer` 绘制过滤图标；几何和过滤槽位置沿用普通舱口及 Create，材质改为黄铜机壳。
- 已补齐注册、创造栏、配方、掉落、模型、镐标签和中英文提示。仅做静态检查，未构建或运行。

## 动力移液臂

- 已阅读 `ArmBlock`、`ArmItem`、`ArmBlockEntity`、`ArmInteractionPoint`／`Type`／`Handler`、`ArmPlacementPacket`、`ArmRenderer`／`ArmVisual` 及 Create 的交互点注册表。原生目标选择物品判断硬编码了普通动力臂，取放与货载使用 ItemStack，因此保留原生机械部分并专门适配流体选择和交互点。
- `FluidArmBlock extends ArmBlock`、`FluidArmBlockEntity extends ArmBlockEntity`。状态机 tick、输入和输出搜索、三种输出策略、转速影响、动画角度、红石锁与持货存档直接复用；手动卸货时将液囊转换成普通流体罐，拆除则清空爪端流体后调用父类销毁，不生成恢复包裹。
- `FluidArmPoint` 将每个输入的流体罐视作一个可查询槽位，明确保存点击面，通过标准有向流体能力取放。`FluidArmCargoItem` 继承 CCD 流体罐，以现有流体组件桥接原生货载，但用独立液囊模型呈现，并且不加入配方、创造栏或原生随机包裹池。它保存已从源移出的真实在途流体，不对外暴露库存或流体能力。每次最多 1000 mB，目标暂时拒收后仍由原生 heldItem 保存剩余量。卸货转换复制完整组件；读取旧版普通包裹货载时转换为液囊。
- 覆盖 `getDistributableAmount` 将原生物品数量计算换成真实 mB 接收量。预检使用单个有效输出的最大接收量，不累加可能重叠的能力视图；实际交付与部分接收继续走原生输出调度及自定义交互点。`collectItem` 仅在原生取货后追加空货载检查，处理流体接口模拟成功、执行为空时的阶段恢复；不替换原生取货调度。
- 原生交互点反序列化没有机械臂调用者参数，全球目标选择又与普通物品动力臂共享。因此将 fluid_arm_point 类型注册用于原生序列化，但不加入普通臂的可选目标；流体子类通过专用加载器验证类型、数量、相对距离、点击面和已加载区块，构造输入输出列表。保留原生 writeInteractionPoints 和相对坐标格式，结构变换同时旋转目标相对位置及接触面。
- 一个 `FluidArmAccessor` 暴露原生包可见状态，供目标初始化和配置时安全重置当前阶段使用，没有替换原生调度方法。客户端收包后刷新目标列表，避免目标数量相同但面或模式已改变时沿用旧数据。
- 配置物品通过 NeoForge 标准 `onItemUseFirst` 在容器打开前选择目标，使用正常物品使用同步，服务端保存带维度的 CustomData 并在应用时验证范围和流体接口；没有另做配置网络包。客户端以原生 Outliner 标记所选面。
- 服务端 `fluidArm.allowDirectContainerInteraction` 默认 `false`，要求经 CCD 流体舱口访问；开启后才允许任意具有标准流体能力的容器。`FluidArmPoint.canTarget` 统一目标类型规则，物品选择与客户端预选标记使用该规则；配置应用通过 `isValid` 复核，每次能力查询也复核，覆盖旧存档目标、模拟、实际取放及目标方块被替换的情况。仍由选中面的能力决定能否交互，不会把舱口后方容器当成新目标而跳过过滤。配置需要重新加载世界生效，旧目标数据及爪端货载不因关闭配置而清除。
- 应力通过公开 `BlockStressValues.IMPACTS` 延迟读取原生机械臂系数。普通渲染与 Flywheel Visual 同时注册，显式保留普通渲染路径以显示爪端液囊。液囊模型使用透明孔洞外壳及标准流体颜色，不重新实现机械臂渲染器；静态底座和机械臂物品模型引用原生资源。护目镜只显示爪端流体与工作状态。
- 首版限标准容器搬运；只有静态代码与资源核对结果，没有构建、运行或游戏测试结论。

## 智能龙头

- 进一步阅读 `BeltProcessingBehaviour`、`TransportedItemStackHandlerBehaviour`、`TransportedResult`、`DepotBehaviour`、`TransportedItemStack`、`FillingBySpout` 和 `GenericItemFilling`，确认原生回调的位置为下方两格，置物台与传送带均可通过行为发现处理器。
- `SmartFaucetBlock`／`SmartFaucetBlockEntity` 继承 CCD 基础龙头，基类开放有向源查询、流体过滤判定与异常即时回流方法，原来的普通龙头仍默认允许全部流体。智能版新增原生 `FilteringBehaviour.forFluids()` 和 `BeltProcessingBehaviour`。
- 物品是否可加工、需要多少流体以及产出结果，直接调用 `FillingBySpout`，由其查找原生注液／序列装配配方并回退到 `GenericItemFilling`；不重新编写一套物品配方系统。
- 接收与停留回调决定 PASS／HOLD。计时仅记录待处理对象的弱引用、输入身份副本和完成时刻，不预扣液体，不拥有额外物品库存。完成前重新扫描当前源与过滤。关闭或暂停放行；区块重载重新计时，不产生在途流体预留。
- 在输入物品及流体副本上计算原生结果，确认消耗符合预期后模拟并执行真实源抽取。只有完整抽出准确数量及组件的流体，且原生目标仍存在、物品仍匹配时，才经 `handleProcessingOnAllItems`／`TransportedResult.convertToAndLeaveHeld` 交付；未提交时即时回流实际流体。丢失目标、模拟执行偏差时不消耗原输入物品。
- 原生通用物品灌装 API 假定物品流体能力正确实现模拟／执行语义，本实现沿用该接口边界。空产出配方按原生消耗语义处理，不把空产出简单视为失败。
- 客户端使用原生 `SmartBlockEntityRenderer` 显示过滤图标，复用基础龙头开关模型并替换黄铜材质，无新 Mixin、内部储液或对外流体库存能力。仅完成静态检查，未构建、测试或运行。

## 铜龙头

- 已阅读 `SpoutBlock`、`SpoutBlockEntity`、`FillingBySpout`、`GenericItemFilling`、`BlockSpoutingBehaviour`、`CauldronSpoutingBehavior` 与原生默认注液行为注册。原生分液池使用 1000 mB 的 `SmartFluidTankBehaviour`、传送带停留加工和下方两格的方块目标；基础龙头的直接容器供液与该结构不同，因此使用新的 `SmartBlockEntity`，不继承其储液和传送带处理。
- 复用原生 `IBE`、扳手、含水机制、`SpoutBlockEntity.FILLING_TIME` 周期和 `FluidFX` 粒子数据。源、目标使用标准有向流体能力，背面取液、下方供液，先模拟双方再执行；没有内部储罐、流体能力注册或液体数量 NBT。
- 炼药锅复用公开的 `CAULDRON_INFO` 注册表读取流体、用量与目标状态，基础的层数增长自行适配。`BlockSpoutingBehaviour.fillBlock` 需要真实的分液池上下文，基础版没有调用该扩展接口；其他方块注液配方和传送带注液留待智能龙头接入。
- 手动开关与红石暂停独立保存于方块状态。放置及实体初始化时复核实际信号，供液只在服务端发生，不查询未加载的源／目标区块。转移失败时当场回流，不能回流则生成已有的 CCD 密封包裹，不持久化余液。
- 模型含水平四向的打开／关闭手柄状态，通电暂停时显示关闭。无需额外动态渲染器，不受 Flywheel 对方块实体渲染的跳过逻辑影响。本轮只静态检查，没有构建、测试或启动客户端。

## 红石三通阀门

- 三通不继承原生二通 `FluidValveBlock`，因为其静态管轴判定与直线连接假设不适用于切换后的弯路。新方块使用 `DirectionalBlock`、原生 `IBE`、`IWrenchable`、`ProperWaterloggedBlock` 和结构变换接口；实体继承 `SmartBlockEntity`。
- 直接使用 `FluidTransportBehaviour`，`canHaveFlowToward` 每次只开放公共口和选中的支路，`canPullFluidFrom` 同步检查有效端口。原生泵发现、压力传播、路径分支和 `FluidNetwork` 实际流体转移均复用，不增加抽液 tick 或独立流体库存。
- 依据实际源码中的 `wipePressure` 行为：它先删除不再开放的接口条目，因此先逐一 `resetNetwork` 清理旧连接缓存，再重建连接并传播压力变化。额外通知全部六个相邻位置的管路，覆盖旋转前后的旧支路；紧贴泵时直接调用其 `updatePressureChange`，避免只从泵所在位置扫描却未通知该泵自身。
- 方块切换、邻居变化、放置和实体初始化将更新合并至延后一 tick 的原生方块 tick；拆除前也清理连接。邻接查询检查区块已加载，不强制加载。保持完整方块碰撞面，防止关闭接口被原生开放端检测误认为可向世界排液的位置。
- `FACING` 表示直通口，反向为公共口，`ROLL` 的四个取值表示垂直侧口朝向，红石决定选择直通或侧通。旋转、镜像与 `StructureTransform` 同时变换直通和侧口方向，再重新确定 ROLL，保持端口关系。
- 静态 multipart 模型按同一方向规则组成主体及三个管口，并用原生 `PipeAttachmentModel.withAO` 增加连接附件。无需额外方块实体渲染器或 Flywheel Visual，不会因动态渲染后端开启而跳过静态模型。
- 额外核对原生泵的 `distributePressureTo` 后发现：它在进入邻接管道前不总是检查对方反向接口是否开放。增加仅针对 CCD 三通关闭接口的 `ThreeWayValveEndpointMixin`，让该管面成为压力图末端，阻止压力搜索穿过关闭口干扰选中通路；该面没有流体能力，也不属于开放排液端，不会成为实际收发目标。
- 无流体能力注册、无液体库存或液体数量持久化；仅含原生管道流动描述数据。本轮只静态核对源码与资源，没有构建、游戏测试或客户端检查。

## 红石流体阀门

- 已阅读实际依赖版本的 `FluidValveBlock`、`FluidValveBlockEntity`（含内部 `ValvePipeBehaviour`）、`FluidValveRenderer`、`FluidValveVisual`、`PipeAttachmentModel` 与 `DirectionalAxisKineticBlock`。
- `RedstoneFluidValveBlock extends FluidValveBlock`，`RedstoneFluidValveBlockEntity extends FluidValveBlockEntity`。原生通断依据 `ENABLED`，原生 tick 根据指针到达端点切换该状态；关闭判断在继承的 `canPullFluidFrom` 中。CCD 只将指针目标改由 `POWERED` 驱动，仍让原生 tick 负责开关和原生方块回调负责管网更新。
- 指针字段 `pointer` 为包可见，增加只读 `FluidValvePointerAccessor`，用于调用其现有 `chase` 方法。未替换原生 tick、管道行为或 NBT 序列化；覆盖速度变化回调后重新应用红石目标，避免继承的旋转执行器目标覆盖红石控制。
- 放置时初始化 `POWERED` 和开关状态，邻居变化与方块重新初始化时复核实际信号。关闭传动轴连接及轴连接放置偏好，不虚构 RPM；流体管轴、含水、碰撞、扳手和结构旋转仍继承。
- 原生阀门方块状态资源作为方向与开关映射依据，CCD 子模型直接继承对应模型并声明 `cutout_mipped`，不替换透明纹理。模型烘焙沿用原生 `PipeAttachmentModel.withAO` 包装。
- 同时注册原生 `FluidValveRenderer` 与 `FluidValveVisual`，已通过本地 Flywheel 类签名静态确认 Visual 工厂支持子类型，避免只注册普通渲染器后被 Flywheel 跳过造成缺件。
- 没有内部储液与额外流体能力。本轮未构建、运行或检查客户端画面；运行时 Accessor 应用、红石开关及显示仍待验证。

## 流体传输器

- 已阅读 `FunnelBlockEntity` 的红石暂停与节流机制、`SmartFluidPipeBlockEntity` 的原生流体过滤行为及过滤渲染。复用 `SmartBlockEntity`、`IBE`、`FilteringBehaviour.forFluids()` 和 `SmartBlockEntityRenderer`；直接转移由 CCD 实现。
- 按用户要求，完全移除内部流体库存、异常余液字段、流体 NBT 读写和流体能力注册。方块实体只保存过滤行为及运行时调度状态；查询的 `IFluidHandler` 属于源／目标容器。
- 最末端节点沿背面逐格查找同向传输器和源容器，最多 64 台，不递归代理能力、不强制加载区块。任一节点暂停、反向、正在转移或端点缺失即中止；每种候选流体必须通过所有节点过滤。
- 仅末端节点发起转移，成功解析通路后统一预留本周期，避免串联设备重复调度或拓扑变化造成重叠转移。默认整链每 8 tick 最多 250 mB，间隔读取原生漏斗配置。
- 先模拟源和目标，缩小到双方可接受数量，并再次确认缩小后的源抽取量，然后执行。未交付部分当场退回源；无法退回的部分转成密封包裹掉落，不持久化到设备里。正常转移没有跨 tick 的在途流体。
- 不新增 Mixin，不参与管道压力传播。本轮只做源码和资源静态检查，没有构建或运行。

## 增压泵

- 已阅读 `PumpBlock`、`PumpBlockEntity`（包括内部 `PumpFluidTransferBehaviour`）、`FluidPropagator`、`FluidNetwork`、`RotationPropagator`、`GearboxBlock`／`GearboxBlockEntity`、原生动力渲染器、`BlockStressValues`、`CStress` 和 `KineticNetwork`。实现只参考实际依赖版本的 Create 源码与 NeoForge 接口。
- 方块、方块实体分别继承 `PumpBlock`／`PumpBlockEntity`。保留原生流体轴、两侧连接、压力图搜索、分支分配、流体身份、模拟及执行转移、存档和过载停机，不重写流体网络。只改变压力和范围输入。
- 应力通过公开的 `BlockStressValues.IMPACTS` 注册一个延迟读取配置的供应器：`原生机械泵系数 × CCD stressMultiplier`。默认倍率按用户要求为 1.5，通常为 6 SU/RPM；网络仍按真实转速绝对值乘算，四个轴口只对应一台设备。Create 的 `CStress.setImpact` 限制非原生方块使用，因此配置注册在 CCD 自己的服务端配置中。
- 压力在原生 `distributePressureTo` 中倍率提升；内部传输行为每 tick 写入的泵口压力同步提升。两处都只针对 `BoosterPumpBlockEntity`，防止只增强外围管道而在泵口形成原生压力瓶颈。没有篡改 `getSpeed()` 或重复调用 `FluidNetwork.tick()`。
- 原生 `FluidNetwork` 从入口压力计算每 tick 转移上限，直接复用即可得到更高输送能力。默认压力 2 倍，在简单无瓶颈管路中流量约 2 倍；实际流量仍取决于接口、分支和整数取整。
- 泵自身搜索距离仅对 CCD 增压泵增加。另将 `propagateChangedPipe` 的泵发现扫描范围扩到增压泵距离，让未建立压力的远端管道编辑也能通知泵；这不修改普通机械泵实际分布压力的距离。默认距离随 Create 配置乘 2，通常每侧 32 格。
- 原生齿轮箱的轴向转换在 `RotationPropagator.getAxisModifier` 内按其实体类型特判，`PumpBlockEntity` 无法通过单继承同时成为齿轮箱。仅对增压泵的四个轴口提供固定参考方向的 1:1 转换因子，让原生传播过程在传入、传出两侧使用同一组因子；渲染共用该因子，未添加全局改速规则。关闭继承来的小齿轮识别，避免无外露齿轮的外壳仍与邻近齿轮啮合。
- 四个轴口的几何开孔为 CCD 自建方块模型，纹理和旋转半轴引用原生资源；渲染器直接渲染这些半轴，不因 Flywheel 开启而跳过尚未另注册 Visual 的 CCD 方块实体。
- 重新载入方块实体时主动刷新压力，以覆盖保存转速未变时原生速度变更逻辑可能跳过重算的情况。配置需要重进世界；没有实现运行中的配置热重载。
- 本轮只静态检查，尚未确认运行时 Mixin 应用、游戏内传动、流量与视觉效果。

## 流体仪表直接复用

- `FluidGaugeBlock extends FactoryPanelBlock`：安装方向、含水状态、命中槽位、碰撞形状、四格布局、初次放置。
- 原生 `FactoryPanelBlockItem`：存量转信器调频、已调频提示、放置前检查、网络频率数据。
- `FluidGaugeBlockEntity extends FactoryPanelBlockEntity`：面板集合、增加／移除面板、存档同步。
- `FluidGaugeBehaviour extends FactoryPanelBehaviour`：原生 tick、库存监控、请求时序、满足状态、连线、原生配置界面及数据包。
- `LogisticsManager`、`LogisticallyLinkedBehaviour` 和 `PackagerLinkBlockEntity`：网络发现、优先级、库存汇总、库存身份去重、请求分配。
- 原生 `PackagingRequest`：地址、数量、订单编号、链路、分片计数及上下文。
- 原生打包机 `heldBox` 与 `queuedExitingPackages`：立即抽取真实流体生成待发包裹，沿用队列收发与保存；不另建原料预留系统。
- 原生 `RequestPromiseQueue`：收货端在途承诺及超时配置。流体入库差量交回该队列。
- 原生 `FactoryPanelModel` 与 `FactoryPanelRenderer`：面板模型、状态灯、路径和过滤显示。

## 必须适配的部分

1. 原生库存、过滤和订单接口接收 `ItemStack` 与整数数量。因此增加 `FluidStockItem` 作为逻辑身份；组件中的流体类型和附加属性确定身份，数量的一单位代表 1 mB。它不由 CCD 作为物理货物生成，也不放入流体罐内部。
2. 流体打包机只向自身的 `InvManipulationBehaviour` 提供只读身份视图，供原生库存身份与排除自身库存逻辑使用。没有把储罐对外伪装成可提取物品的容器；真实转移仍查询标准流体接口。
3. 原生面板补货判定对方块类型作了原生打包机精确匹配。流体仪表子类把该目标适配为 CCD 流体打包机。
4. 原生补货单次上限为 9 个物品堆叠。作用域限定为 `FluidGaugeBehaviour` 的小范围 Mixin，将它换成一个流体罐的容量。请求分配、排除自身库存和在途承诺仍由原生方法执行。
5. 面板单位设置改成 mB／B，实际保存值统一为 mB；其他设置仍由原生界面与数据包负责。
6. 原生新增面板、潜行扳手和额外面板掉落硬编码了普通工厂仪表物品，子类仅替换这些必要入口，避免流体仪表拆下后变成普通仪表。
7. 库存变化后的原生私有到货检测只识别普通工厂仪表。CCD 将流体库存正增量交给相邻流体仪表的原生 `restockerPromises`，不另建承诺队列。

## 仍保留的边界

- 0.3 已通过继承原生理包帮助器扩展 CCD 混合内容整理，详见 `composite-case.md`。自动混合拆包与分流尚未实现。
- 网络模式与补货模式采用原生含义：普通墙面面板监控网络，附着在收货端流体打包机上的面板维持所设目标存量。不是另做一套“一次性下单”按钮。
- 打包机上的流体过滤只约束红石自动打包；网络请求按请求中的流体身份执行。这让同一储存的多个打包机可以沿用原生的库存去重与选源逻辑。
- Create 多方块流体罐使用控制器位置作为库存身份。其他模组的单方块容器按方块位置识别；不同方块实体代理同一多方块存储的第三方容器，后续需增加其库存身份适配。
- 当前完成代码与编译打包。按照用户此前要求，本轮不启动游戏测试或客户端检查。


## 设备拆除策略

按当前约定，设备内部未封装流体随拆除直接清除。FluidArmBlockEntity 在父类掉落货载前清空爪端流体，FluidPackagerBlockEntity 在父类处理正常包裹前清空恢复余液，智能储罐缩减后的超量流体不生成包裹。普通物流实体包裹及运行时恢复路径不属于拆除补偿。默认不做旧存档兼容，后续开发优先继承、调用 Create 原生机制。

## 细雪流体与蜜脾模具

- 先阅读 Create `VirtualFluid`、`GenericItemFilling`／`GenericItemEmptying`、`CauldronSpoutingBehavior`、`VanillaFluidTargets`、`FluidPropagator`、`FlowSource` 和 `BasinRecipe`，以及 NeoForge `CauldronFluidContent`、`CauldronWrapper`、`FluidBucketWrapper` 与原版 `SolidBucketItem`／`PowderSnowBlock`。
- 细雪继承 `VirtualFluid`，世界状态引用原版细雪。原版细雪桶是 `SolidBucketItem`，只增加 `FluidBucketWrapper.getFluid()` 适配；空桶填充、容器装卸、过滤与自动化仍走标准处理路径。
- 炼药锅通过原生注册事件与包装器提供接口；分液池注液使用 Create 公开的 `CAULDRON_INFO`，完整桶与满锅统一为 1000 mB。已被其他模组注册的雪锅不重复注册。
- Create 的 `FlowSource.FluidHandler` 要求目标存在方块实体，不能直接发现原版细雪或雪锅接口。小范围 Mixin 只扩展 `VanillaFluidTargets` 的目标判断与抽取入口，液量转移保留原生管口实现；固态细雪的实际移除调用原版 `BucketPickup`。不为原版方块缓存未经失效处理的全局接口。
- 集液栅沿用天气回调与原生概率，智能版继承过滤、红石控制；单块细雪转移通过 `FluidUtil.tryFluidTransfer` 执行，不另写区域搜索。
- 蜜脾模具只实现物品余物接口，`BasinRecipe` 原生检查产物与返还模具的容量并提交；配方使用 `create:compacting`，没有复制原生压块逻辑。
- 未读取或复用 Repackaged、Create: Fluid、FluidLogistics 的实现代码；本次只静态检查，未运行构建、游戏或测试。

## 提示与物品说明

物品说明沿用 `TooltipModifier.REGISTRY` 提供器、`ItemDescription.Modifier` 和 `KineticStats`；原生客户端事件负责读取 Create 的提示开关、按键、语言缓存和排版。护目镜通过 `CCDTooltip` 复用 `LangBuilder.forGoggles` 与 `CreateLang.number`，不另写一套绘制或断行实现。配置名称与说明使用 NeoForge `ModConfigSpec.translation` 和 `.tooltip` 规则。中英文文本、动态数据和静态操作说明分工见 `tooltips.md`。本次未构建或启动客户端。