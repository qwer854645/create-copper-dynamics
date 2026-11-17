# 提示与说明

CCD 的物品说明直接使用 Create 的 `ItemDescription.Modifier`、`TooltipModifier.REGISTRY` 和 `STANDARD_CREATE` 配色。动力设备同时使用原生 `KineticStats`，按 Create 自身设置显示应力消耗。

## 物品说明

- 默认仅显示流体名称、存量、收件地址或损坏提示等当前物品数据。
- 按住 Shift 查看用途与工作条件；有独立操作说明的物品可按住 Ctrl 查看按键操作。按键提示、文字换行和下划线高亮均由 Create 生成。
- 使用 `物品翻译键.tooltip.summary`、连续编号的 `conditionN/behaviourN` 和 `controlN/actionN`。仅在这些原生说明字段中使用 `_重点_` 标记。
- 普通操作反馈保持简短，说明结果或需要执行的下一步；错误用红色，无法继续的条件用黄色。移液臂的取液点与注液点沿用蓝色、金色区分。
- 不在玩家说明中展示库存代理、流体接口实现、耗液预付或轮询调度等实现细节。配置项仅在影响玩家选择时提及。

已覆盖 23 项物品／设备说明，包括所有对玩家开放的 CCD 物品、流体物流标记，以及新增流体用途对应的原版细雪桶。原版细雪桶的说明通过注册表提供器补充，不覆盖其他模组的直接注册。

## 护目镜信息

`CCDTooltip` 调用原生 `LangBuilder.forGoggles` 提供分组和缩进。标题说明信息类别，正文显示当前状态、连接、流体和数值；完整操作教程放在物品说明中。

数字使用 `CreateLang.number`，流体单位使用原生 `generic.unit.millibuckets`。存量为金色，参数为青色，红石暂停或连接异常等需要关注的条件使用黄色。储罐结构尺寸在潜行时显示，流体清单的显示顺序与无指定流体的抽取顺序一致。

## 配置与界面

配置项使用 NeoForge 原生 `translation` 名称与 `.tooltip` 说明，明确倍率、数量单位、特殊值和重入世界生效的要求。配置路径与默认值不变。流体仪表精确数量输入补充范围和单位提示。

## 参考与检查

实际阅读了 Create 的 `ItemDescription`、`TooltipHelper`、`TooltipModifier`、`KineticStats`、`ClientEvents.addToItemTooltip`、`IHaveGoggleInformation`、动力方块实体及物品提示，中英文语言文件，以及 `CFluids` 配置说明。另核对 NeoForge `ModConfigSpec` 与 `ConfigurationScreen` 对翻译键的处理。

本次只做代码与资源静态检查，包括翻译引用、占位符、成对高亮、说明编号、注册物品覆盖与资源格式；未编译、启动客户端或进行界面实测。
