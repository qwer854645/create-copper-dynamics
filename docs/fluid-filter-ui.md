# 流体过滤器外观与界面

- 两种物品图标基于 Create 6.0.10 的 `item/filter` 与 `item/attribute_filter`，只替换左右边框的材质色阶，保留中心与透明像素。资源属于上游美术的改色衍生，非原创图标。
- `tools/recolor-fluid-filters.py <Create资源目录>` 可重建贴图；预览位于 `outputs/fluid-filters/items.png`。
- 界面直接引用 Create 的 `AllGuiTextures.FILTER`、`ATTRIBUTE_FILTER`、`PLAYER_INVENTORY` 和 `IconButton`／`AllIcons`，不复制 GUI 贴图。
- 普通过滤器保持 18 个流体样本槽、允许／拒绝列表与组件匹配。属性过滤器使用参考流体、属性选择、添加／反向添加、任一／全部／拒绝模式及规则数量列表提示。清空按钮删除全部规则。
- 流体规则继续由 CCD 菜单与数据包保存；Create 的物品属性菜单不能直接处理流体温度、密度等规则，因此未直接实例化其物品筛选菜单。

后续 UI 修正：使用 Create `AbstractSimiContainerScreen` 的控件提示渲染、背包居中与标题绘制；样本槽按原生 FilterMenu 的 (23,25) 内容坐标对齐；属性选择改用原生 SelectionScrollInput 和 Label。JEI／EMI 的属性版拖放仅指向参考样本槽，不直接添加规则。添加与反向添加原子保存、重复属性禁用、模式选择幂等。普通版使用显式 slot 字段保留稀疏槽位，兼容旧版顺序数据，删除不再移动其他样本。背包保留数量／耐久等叠加与提示，拖动结束后不再一直绘制鼠标流体图标。

属性版规则摘要沿用原生只读列表提示，删除全部使用清空按钮。

验证：Java 编译通过，66 项 GameTest 全部通过，新增覆盖稀疏槽位、删除、模式幂等、反向添加、重复属性与清空。尚未客户端视觉及 JEI／EMI 联机复验。本轮未更新 JAR。
