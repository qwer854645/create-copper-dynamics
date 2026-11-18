# 舱口原生材质试稿

## 敞口槽体修订

当前模型由 `create-open-fluid-hatches.py` 生成，替代下述第一版平盖板方案。两侧为 1 像素深的阶梯三角板，不再包含越过安装面的旋转方体角。中央为薄斜槽底，顶部不加封盖；背面是 Create 原生 north 管口 rim 与 rim_connector，保留原 UV，去掉内部安装面的剔除标记。智能版斜槽底引用智能管道 smart_pipe_2 的过滤边框区域，中心对齐原生 HatchFilterSlot 的位置与倾角。没有添加装饰液体或内部储液逻辑。

两种舱口共用新的轮廓与碰撞形状；本次未构建或启动客户端，需在游戏内确认四向安装、过滤图标遮挡与手持点击效果。所有 PNG 保持不变。

按用户要求停用两种舱口的旧 housing/flap 纹理。普通版直接引用 Create `fluid_tank` 与 `copper_underside`；智能版直接引用 `brass_casing` 与 `brass_block`。没有生成或复制新的 PNG 材质，旧 PNG 暂留作历史资源，当前模型不再引用。

保留原版舱口几何及显示变换，显式重设表面 UV 以显示完整的 16×16 材质，不使用原物品舱口图集的保险库边条、锁扣或把手图案。几何内容来自 Create，不标为 CCD 原创；重新生成脚本为 `reference-native-hatch-materials.py`。

仅模型材质和 UV 改动，交互、过滤位置不变。离线预览见 `outputs/tank-style-hatches/preview.png`，不含运行时过滤图标。尚未构建或启动客户端。
