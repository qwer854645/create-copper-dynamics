# 流体扩展端口与流体桥接器（功能原型）

## 使用

- 流体扩展端口 `ccd:fluid_access_port`：对容器表面放置，背面连接源容器，其他五面供管道、打包机、龙头等标准流体设备访问。可继续接端口；整个解析路径最多经过 16 个代理块。支持六向放置与扳手转向。
- 流体桥接器 `ccd:fluid_bridge`：朝向轴的两个端面连接容器，其他四面提供聚合接口。两个端面分别用 Create 原生流体过滤槽设置过滤，空过滤默认允许全部；过滤同时约束库存显示、填充和抽取。端面本身不向外提供流体能力。
- 端口可转接桥接器侧面；桥接器的端点可经端口连接容器，但不能再转接另一个桥接器。代理不沿普通管道寻找远端容器。
- 两种设备均无内部储液、动力需求、压力或主动移液。接通两个罐不会平衡液位，必须由外部设备执行存取。拆除只掉本体。
- 填充优先已有同种流体的来源，再尝试其他来源，顺序固定；指定抽取严格比较完整流体组件，不指定时选第一个可抽取流体。一次调用可汇总两端的同种流体。
- 配方：铜机壳＋2 流体管道产出 2 端口；黄铜机壳＋2 端口＋2 电子管产出 1 桥接器。两者加入 CCD 创造栏。

## 接口与物流

每次操作有界解析当前实际来源，保留目标面的标准流体能力约束；失效、拆除或未加载时不继续使用旧库存，不强制加载区块。路径检测环路与深度限制，不增加每 tick 邻居扫描。本原型尚未加入长链解析缓存。

Create 多方块储罐以控制器位置识别真实库存；桥接两端指向同一控制器时采用第一端，避免容量与液体重复显示。此时另一端的过滤和接口不叠加，应将不同过滤出口配置在不同桥接器或不同设备上。第三方多个方块代理同一私有库存的身份仍需专门适配。

物流遍历通过小范围 Mixin 建立 CCD 专用计数作用域：网络统计和请求分配按维度、真实容器、流体和完整组件去重，单次遍历的每台打包机摘要保持一致。直接连接 A 和桥接 A+B 同网时，A 的内容只计算一次；接收端库存从同次统计中排除，避免向自身补货。原生物品打包机的摘要实现不变。

流体舱口仍是手持／移液臂专用接口；本功能不改变其能力暴露规则。机器通过端口和桥接器访问时沿用原设备的标准流体调用；移液臂直接选择这些新方块仍受 `allowDirectContainerInteraction` 配置限制。

## Connected 代码参考范围

已阅读 Create: Connected 的 `CCInventoryIdentifiers`、`InventoryAccessPortBlockEntity`、`InventoryBridgeBlockEntity` 和 `mixin/inventoryaccess/PackagerBlockEntityMixin`：端口转交后方库存标识；桥接器使用负向端库存标识，源码明确说明不能完整去重；代理接口类型用于拒绝嵌套。

CCD 参考其“真实来源身份”和“限制代理嵌套”的设计动机，使用自身的有界流体来源解析器与按流体计数作用域独立实现；未复制 Connected 实现代码、模型或贴图。功能及代码结构并非逐句转换。

上游阅读链接：
- https://github.com/hlysine/create_connected/blob/main/src/main/java/com/hlysine/create_connected/registries/CCInventoryIdentifiers.java
- https://github.com/hlysine/create_connected/blob/main/src/main/java/com/hlysine/create_connected/content/inventoryaccessport/InventoryAccessPortBlockEntity.java
- https://github.com/hlysine/create_connected/blob/main/src/main/java/com/hlysine/create_connected/content/inventorybridge/InventoryBridgeBlockEntity.java

## 验证与外观

新增 GameTest 覆盖端口链模拟／执行、断链后旧接口失效、双端过滤及过滤变更、环路、直接容器与桥接聚合库存重叠。`runGameTestServer --offline` 通过全部 18 项测试。

外观使用完整铜／黄铜方块壳、灰铁连接座和原生铜管口。16×16 机壳及灰铁座由 pixel-art Canvas 逐像素绘制，结构参考 Create 存量转信器的套框；原生 `fluid_pipe/rim_connector/south`、`fluid_pipe/rim/south` 几何及 `pipes` 材质沿用 Create，桥接器两端过滤面板复用 `smart_pipe_2`。原生资源及衍生几何权利归上游，不标为 CCD 原创；未使用 Connected 美术。

生成器为 `tools/create-fluid-access-art.py`；功能资源生成器最后也调用它以避免覆盖美术。`outputs/fluid-access/preview.png` 是实际模型离线预览，贴图拼图为 `textures-source.png` 和最近邻放大的 `textures.png`。过滤行为的位置与新面板中心对齐。此轮只静态检查及离线预览，未构建、测试或启动客户端验证显示效果。
