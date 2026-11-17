# 移液臂铜色与针管式末端

底座以原生黄铜为主，仅静态底座侧面的上下包边与角部加入铜色，旋转底座及齿轮保持原色。上下臂节换为铜色，灰色关节、金属接触面、升降滑轮材质和护目镜保持原色。采用 pixel-art 技能处理整数像素，不改变原生图集的分辨率、UV、像素布局与透明度。

mechanical_arm.png 是 Create 6.0.10-280 原始 32×32 图集的铜色变体。仅将暖色像素（R≥G≥B、R−B≥20、G−B≥8）按原亮度映射到九级铜色，其余像素及 alpha 不变。mechanical_arm_base.png 从原始黄铜图集出发，只替换底座侧面 UV 对应区域的包边像素。映射见 outputs/fluid-arm-copper/palette.json。

原夹爪换成固定针管式头部：保留原生腕关节，新增铜箍、四根铜框、四面透明观察窗、深色空载内壁和灰色短针嘴。玻璃以稀疏像素高光表达，流体位于高光与内壁之间。针嘴末端位于原腕局部 Z=-15，不增加搬运距离；两个旧夹指 partial 改为空模型，仍沿用原生关节及动作。物品模型保留原生显示变换及前十一段几何，将新头部按原生腕关节位置平移进去。

针管外壳始终可见。FluidArmCargoModel 只绘制现有货载里的液体，经原生 ArmRenderer 的 FIXED 持物变换放进针管窗口；1000 mB 对应现有 FluidArmPoint.BATCH，不增加储罐或改变转移逻辑。使用实际流体 still sprite、颜色与动画，液量按二十档显示，低液量保留闭合液面；缓存随模型重载更新。原生非方块货载变换为 RX(90) → T(0,-10/16,0) → S(.625)，加上 ItemRenderer 的居中后，物品模型点 p（模型像素）映射为腕局部 q：

```
qx = .625 * (px - 8)
qy = -.625 * (pz - 8)
qz = .625 * (py - 8) - 10
```

货载模型的 X/Z 范围 3.76..12.24、Y 范围 6.72..15.68，因此液体位于局部 X/Y ±2.65、Z -10.8..-5.2，处于铜框和高光之内。普通与 Flywheel 路径都使用原生持物绘制；地面/倒挂和护目镜继续沿用 Create 行为。

FluidArmModels 注册六个 scoped partial。客户端 Mixin 只替换 CCD 移液臂在原生 ArmRenderer.renderArm、ArmVisual 构造与护目镜更新中的局部模型读取，不修改 Create 全局字段。ARM_BASE 与 ARM_COG 不注入、不替换，普通动力臂保持原样。

生成命令为 node tools/recolor-fluid-arm.cjs <解压后的 Create 源码根目录>。全臂预览位于 outputs/fluid-arm-copper/comparison.png，头部特写位于同目录 syringe.png；蓝色仅为离线装水示意，游戏内显示实际货载。静态检查及离线预览不代表游戏内验证，未构建或启动游戏。

原始臂节、关节、底座、图集与护目镜美术归 The Create Team / The Creators of Create 所有，改色不视为原创上游美术；许可见 [Create LICENSE.md](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md)。CCD 新增针管几何、玻璃高光及内壁/针嘴像素贴图；铜框复用本项目 faucet/body 贴图。Create 美术资源不因代码 MIT 许可而成为 MIT 资源。
