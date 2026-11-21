# 用户美术修订 8

来源：`aaa (8).zip`。导入创造储罐模型和接口贴图、机械臂铜色贴图、两种过滤器物品贴图及流沙桶贴图。

按用户要求不导入包内两份旧物品模型：`item/flow_meter`、`item/vertical_turning_booster_pump`。其余文件与当前资源相同的不重复修改。创造储罐顶部横条四侧的 cullface 从统一 west 修正为对应面，保留 UV 与几何。创造储罐物品通过 parent 自动继承更新。

导入工具：`tools/import-art-revision-8.py`。原资源备份在 `outputs/art-revision-8-backup`。贴图引用与物品／方块主体一致性检查通过。未构建或启动客户端，JAR 未更新。
