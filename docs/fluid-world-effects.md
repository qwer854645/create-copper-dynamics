# 流体世界接触效果

以下接触效果由 CCD 实现，除辐射外不要求安装加工附属。仅放置在世界中的流体生效，储罐和管道不会向附近生物施加效果。创造／旁观玩家不受新增接触效果影响。

- 流沙：身体接触时水平运动缩至 35%，阻止向上运动；眼部浸没后每秒 2 点窒息伤害。加工联动 Create: Dragons Plus／Dreams n' Desires，Connected 提供触媒交互。
- 超级熔岩：继承熔岩行为，接触点燃并造成 20 点基础伤害；原生火焰免疫和抗火规则保留。独立可用，超热气流加工联动 Dreams n' Desires。
- 富集介质（用户称浓缩液，注册 ID enriching_reagent）：安装 Create Nuclear 时，接触每秒调用原生 RadiationCapability.applyContagion(entity,5,60)，防护与累积由上游控制。旧版本仅在存在 createnuclear:radiation 效果时使用该效果，否则不替换为其他伤害。尚未验证可选模组运行时剂量。
- 剥魂液：接触每秒刷新 3 秒凋零 I、虚弱 I；加工联动 Create: Nether Industry。
- 爆破浆液：接触刷新 3 秒中毒 I，燃烧时每秒额外 4 点火焰伤害；不产生地形爆炸。加工联动 Create: More Catalysts。
- 共鸣溶液：接触刷新 5 秒发光、3 秒缓慢 I；加工联动 Create: More Catalysts。
- 幽匿悬液：接触刷新 5 秒黑暗、3 秒挖掘疲劳 I；加工联动 Create: More Catalysts。
- 净化液：接触每秒移除有害状态效果并灭火，不恢复生命；加工联动 Create: More Catalysts。
- 细雪流体：现有流体形式用于运输、灌注及冷冻加工，没有新增可放置的液体方块；原版细雪方块保留原生冻伤。加工联动 Dragons Plus／Dreams n' Desires／Garnished。

验证：基础环境 GameTest 通过，覆盖减速和各状态效果／净化；此前超级熔岩接触与凝固测试继续通过。辐射附属运行时、实际流动边界与客户端效果尚未复验。JAR 未更新。
