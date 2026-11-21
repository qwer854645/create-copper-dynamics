# alpha.10 后模型核查

## 已确认并修复

垂直转向增压泵生成器按数组下标选择侧板，但正向／反向源模型元素排序不同。反向状态误将管口作为轴孔侧板处理，并复制到错误位置。现改为按名称和几何厚度识别左右侧板，保留原来的两组管口。新增检查覆盖正向与反向的管口、侧板数量。

万向泵输入／输出模型与三通管口完全重复。现改为 parent 引用，生成脚本同步，避免两份几何独立漂移。

## 资源与同步结果

- 211 个模型、587 条方块状态模型配置解析通过；48 个垂直转向泵方向／反向状态的管口轴向检查通过。
- 本地 CCD 模型面贴图变量和 CCD 贴图路径检查通过。
- 三通、红石双向泵的完整物品重建一致性检查通过。
- 缓存泵：28/28 元素；调频泵：14/14；创造储罐：17/17；万向泵默认主体：1/1，均匹配。
- 垂直转向泵：11/11 主体元素，物品多出的 2 个为半轴。流量计：9/9 主体与表头元素，物品多出的 1 个为指针。
- 流体访问口、桥的外壳匹配；物品中的额外接口为其默认展示状态。

## 保留的冗余候选

`tools/audit-model-redundancy.py` 为只读引用扫描，不自动删除。以下资源没有发现游戏运行时入口，但部分仍供旧生成器、导入脚本或审计使用：

- `block/reciprocating_fluid_pump/{body,bellows,plate}`：旧调频泵模型；当前便携泵使用 `portable_fluid_pump` 目录。
- `block/fluid_access_port_powered`、`block/fluid_bridge_powered`：旧完整通电模型；当前方块用拆分 casing／port 资源。
- `block/smart_collection_grate_filter`：旧独立筛选面板，已有历史脚本引用。
- `block/three_way_valve/dial_powered` 与 `dial_unpowered` 完全相同，当前动画用动态指针；`dial_unpowered_cross` 也无运行时入口。
- `block/buffer_pump` 为审计／物品源组合，实际方块使用拆分模型；`buffer_pump/cog`、`frequency_pump/cog` 为物品齿轮源，世界内复用原生动态模型。这些不应单凭没有运行时引用删除。
- 包裹 study 模型有两对完全重复；机械臂上下夹爪模型相同但以两个动态部件标识加载。未删除。

## 范围

本轮为资源与代码静态核查，没有启动客户端或构建 JAR。通过检查不代表所有视角与所有动画帧均已视觉验证；用户导入的倒置 cuboid 边界保持不变，不作为一般错误归一化。

新增只读工具：`tools/audit-item-parts.cjs`、`tools/audit-model-redundancy.py`。
