# 舱口原生材质试稿

按用户要求停用两种舱口的旧 housing/flap 纹理。普通版直接引用 Create `fluid_tank` 与 `copper_underside`；智能版直接引用 `brass_casing` 与 `brass_block`。没有生成或复制新的 PNG 材质，旧 PNG 暂留作历史资源，当前模型不再引用。

保留原版舱口几何及显示变换，显式重设表面 UV 以显示完整的 16×16 材质，不使用原物品舱口图集的保险库边条、锁扣或把手图案。几何内容来自 Create，不标为 CCD 原创；重新生成脚本为 `reference-native-hatch-materials.py`。

仅模型材质和 UV 改动，交互、过滤位置不变。离线预览见 `outputs/tank-style-hatches/preview.png`，不含运行时过滤图标。尚未构建或启动客户端。
