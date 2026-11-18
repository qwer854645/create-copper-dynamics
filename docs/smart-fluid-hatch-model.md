# 智能流体舱口模型

沿用用户选择的平板方案，以黄铜区分普通铜制版。薄板为 [1,0,0]–[15,16,2]，下方管口中心为 [8,5,4]，上方过滤面板为 [5,10,2]–[11,16,3]。全模型四个元素，不加斜面、铆钉、盖板或开合。物品继承此固定模型与原生舱口显示变换。

铜管口沿用普通版已导入的 Create `fluid_pipe/rim_connector/south` 和 `rim/south`，仅整体下移 3，不改尺寸、贴图、UV。过滤面板直接复用原生 `smart_fluid_pipe/block` 的第三个元素的面 UV 与 `smart_pipe_2` 贴图，将北面转到朝外的南面，适配为 6×6、厚 1 的平板安装。两部分恰在 Y=10 交界，不覆盖彼此的正面。

黄铜贴图是 CCD 已有 `smart_faucet/body` 的 16×16 像素簇，加一像素亮边和暗边；按 pixel-art 工作流直接修改整数像素，不做渐变或抗锯齿。

`FilteringBehaviour.forFluids()`、原生过滤匹配、存档、同步、配置与 `SmartBlockEntityRenderer` 图标绘制保持使用。用原生 `ValueBoxTransform.Sided` 将显示与点击中心放在 [8,13,3.05]，比例 .375，对应 6 模型单位；仅 FACING 的反向启用，四向旋转由原生 Sided 处理。中心比面板端面向外偏移 .05，避免图标与面板重叠闪烁。未配置过滤时仍拒绝进出；只允许手持容器和移液臂显式交互，不再向管道或机器提供流体能力。

智能版碰撞为薄板、下方端圈及上方面板三个长方体的并集，由原生 VoxelShaper.forHorizontal 负责朝向；普通版保留自己的 12×12 平板形状。两者仍不创建独立储罐，交互成功只播放音效。

生成命令：`node tools/create-smart-fluid-hatch.cjs <解压后的 Create 源码根目录>`。依赖已生成的普通版模型与 CCD 黄铜材质。普通版生成器不再写入智能版模型，避免重新生成时覆盖设计。预览位于 `outputs/smart-fluid-hatch/preview.png`，未设置过滤，图中不虚构运行时图标。完成静态代码、坐标、资源引用及离线预览检查；未构建、测试或启动游戏，交互效果尚未实测。

薄板几何及黄铜像素贴图为 CCD 自有资源。原生管口、过滤面板 UV、贴图和物品显示变换归 The Create Team / The Creators of Create 所有，不标注为 CCD 原创或 MIT 美术；许可见 [Create LICENSE.md](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/LICENSE.md)。
