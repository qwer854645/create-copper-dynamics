# 流体过滤器外观与界面

- 两种物品图标基于 Create 6.0.10 的 `item/filter` 与 `item/attribute_filter`，只替换左右边框的材质色阶，保留中心与透明像素。资源属于上游美术的改色衍生，非原创图标。
- `tools/recolor-fluid-filters.py <Create资源目录>` 可重建贴图；预览位于 `outputs/fluid-filters/items.png`。
- 界面直接引用 Create 的 `AllGuiTextures.FILTER`、`ATTRIBUTE_FILTER`、`PLAYER_INVENTORY` 和 `IconButton`／`AllIcons`，不复制 GUI 贴图。
- 普通过滤器保持 18 个流体样本槽、允许／拒绝列表与组件匹配。属性过滤器使用参考流体、属性选择、添加／反向添加、任一／全部／拒绝模式及规则数量列表提示。右键规则摘要删除最后一条规则；清空按钮删除全部规则。
- 流体规则继续由 CCD 菜单与数据包保存；Create 的物品属性菜单不能直接处理流体温度、密度等规则，因此未直接实例化其物品筛选菜单。

验证：Java 编译与资源引用检查通过；尚未客户端复验。本轮未更新 JAR。
