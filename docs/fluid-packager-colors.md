# 流体打包机铜色预览

本版按要求保留 Create 打包机的模型形状，只将原贴图中的深蓝／蓝紫色阶替换为铜色阶。灰绿外框、银色包边、木质部件、红石灯、UV、分辨率和透明度均保留。

原始贴图来自本地 Create 6.0.10-280 参考资源，共复制 11 张，替换 840 个有色像素。中高亮铜色取自 Create 铜机壳色板，深色部分补充对应的铜色阴影。贴图保存在 `assets/ccd/textures/block/fluid_packager/`。机身、竖向模型、物品模型、舱门及托盘均引用 CCD 的改色资源；舱门和托盘的几何及动画状态继续使用原生实现。

`outputs/fluid-packager-copper/comparison.png` 为实际 JSON 模型的离线渲染：左为 Create 原版，右为铜色替换；上下两排分别观察开口侧与舱门侧。使用相同视角、光照、最近邻纹理采样及透明裁切，未启动 Minecraft。离线渲染不包含游戏中的环境光遮蔽、光照贴图或光影包效果。

`outputs/fluid-packager-copper/palette.json` 记录颜色映射、贴图尺寸及修改像素数量。`tools/preview-fluid-packager.cjs` 使用 Node.js 与 sharp，接收解压后的 Create 源码根目录，可重新生成贴图与预览。

原始美术资源归 The Create Team / The Creators of Create 所有，本项目仅进行了所述换色，不将原模型及贴图标注为 CCD 原创。上游美术资源与代码分别列明许可，来源见 [Create LICENSE.md](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md)。
