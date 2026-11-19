# 移动流体接口造型试稿

用户所称“连通器”按现有流体传输器 `ccd:fluid_transmitter` 实施。原模型、物品模型、方块状态及整套贴图在修改前复制至 `outputs/portable-transmitter/backup`，可逐文件恢复。

新模型使用短铜机身和双端 Create 移动流体接口头，接口几何来自原生 `portable_fluid_interface/block_top`，只压缩伸缩方向长度，保留端面尺寸与 UV；机身直接引用 copper_underside，接口直接引用 portable_fluid_interface。上游几何与美术归 Create，不标为原创。未生成或覆盖原有 PNG。

本次仅试验外观，不赋予移动结构连接能力，也未加入伸缩动画。过滤交互、输送逻辑、朝向和碰撞箱保持原样。实际模型离线预览为 `outputs/portable-transmitter/preview.png`；未构建或启动客户端。
