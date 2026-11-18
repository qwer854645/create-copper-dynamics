# 可选批量加工兼容

最新显示规则：七种可选加工介质只在对应模组存在时加入 CCD 创造栏并加载搅拌配方；注册仍固定保留，以免卸载附属导致旧存档流体 ID 丢失。超热对应 D&D，富集对应 Nuclear，剥魂对应 Nether Industry，其余四种对应 More Catalysts。未实现 JEI／EMI 专用隐藏，命令和旧存档内容仍可访问。

所有 CCD 专用装液桶已使用单帧静态 PNG 和原生 generated 物品模型，不再使用动态流体容器加载器。通用铜桶使用固定铜壳图标，实际内容由提示和容量条显示；原版细雪桶不变。新静态桶图标沿用 Mojang 桶轮廓并改绘液体颜色，不标为 CCD 完全原创美术。

## CCD 默认介质

新增七种可桶装、管道输送及世界放置的流体：`seething_reagent`（超热介质）、`enriching_reagent`（富集介质）、`soul_stripping_reagent`（剥魂液）、`explosive_slurry`（爆破浆液）、`resonant_solution`（共鸣溶液）、`sculk_suspension`（幽匿悬液）、`purifying_solution`（净化液），均有对应 `flowing_` ID 和 `_bucket` 物品。加上细雪、流沙，目前共九种流体、十八个源／流动流体 ID。

每种默认配方使用 1000 mB 水与一种材料搅拌，产出 1000 mB：烈焰粉、铀粉标签 `c:dusts/uranium`、灵魂沙、火药、紫水晶碎片、幽匿块、荧石粉。超热配方要求超级加热；富集配方仅在 Nuclear 安装时加载，铀粉标签是否由目标发布版本提供仍需合装核对。上述为 CCD 新设计配方，不是上游原配方。

七种均绑定对应加工媒介标签，不安装附属时可存储／运输，但不会凭空得到该附属加工能力。上游已有蜂蜜、巧克力、龙息、染液及其媒介标签映射继续有效；这七种 CCD 默认流体始终注册，未实现根据上游流体存在自动替换获取配方。如此保持存档 ID 稳定，也意味着同用途上游流体可能并存。未新增生物沉陷、主动爆炸或核辐射机制，生物作用来自实际使用的上游加工类型。

构建成功，新增测试验证所有七种桶的流体能力、无 Nuclear 时其余六种配方实际加载；未合装测试各附属，也未验证客户端纹理。

所有适配通过准确的模组 ID、加工类型 ID 和流体标签查询，无上游类的硬依赖，不复制注册上游加工配方。自定义 `ccd:atomizing` 配方优先于默认兼容映射。

## 已实现映射

- Dragons Plus：`create_dragons_plus:sanding`、`freezing`、`ending`、`coloring_<原版颜色>`。流沙用于喷砂、细雪用于冷冻；龙息和染液按其上游标签匹配。流沙方块加入其喷砂标签以激活上游可选喷砂功能。
- D&D：`dndesires:sanding`、`freezing`、`dragon_breathing`、`seething`。流沙、细雪使用 CCD 媒介；龙息与超热仅读取上游同名 `fan_processing_catalysts/<类型>` 流体标签，不把熔岩强行解释为超热。
- Garnished：`garnished:freezing`、`<颜色>_dye_blowing`。细雪及上游冷冻标签用于冷冻；原版 16 色使用 `c:dyes/<颜色>` 或上游 `fan_processing_catalysts/dye/<颜色>`。
- More Catalysts：`create_more_catalysts:honey_coating`、`chocolate_coating`、`exploding`、`resonance`、`sculking`、`purifying`。匹配其 `fan_catalysts/<类型>` 流体标签；蜂蜜／巧克力另外直接支持 Create 的对应流体。
- Shimmer：`create_shimmer:glooming`、`transmutation`，读取 `fan_glooming_catalysts`／`fan_transmutation_catalysts` 流体标签。
- Nuclear：`createnuclear:enriched`。上游当前只提供方块媒介，CCD 提供空流体标签 `ccd:fan_media/enriched`，数据包加入选定媒介后雾化器才会启用；未擅自注册核流体或新增富集配方。
- Nether Industry：`createnetherindustry:soul_stripping`。同样为原生方块媒介，使用数据包选择标签 `ccd:fan_media/soul_stripping`；默认没有流体映射。

喷砂优先 Dragons Plus，再 D&D，最后 CCD 砂纸配方适配器；冷冻优先 Dragons Plus，再 D&D，再 Garnished，选择顺序固定，不重复执行。上游类型自己的 canProcess/process 继续生效；仅在 isValidAt 中检查的全局功能开关，尚需逐版本适配，不能声称已覆盖所有上游开关。

## Connected 触媒交互

流沙、细雪、龙息、原版染液及支持的媒介可用标准容器向空触媒灌入 1000 mB。转换目标依据映射表选择，Connected 对应功能必须已启用。通过反射调用其 `FeatureToggle.isEnabled(ResourceLocation)`；接口变化时拒绝转换而不绕过开关。

生存模式在容器副本上模拟并执行完整扣液，成功替换方块后才写回容器；失败不改变手中内容。创造模式不扣液，堆叠容器保留其余数量并归还结果。暂未新增从触媒恢复媒介的拆卸交互，仍沿用 Connected 自身回收配方；它可能不返还媒介。

## 参考与验证范围

核对 DragonsPlusMinecraft/CreateDragonsPlus 的 `1.21.1/6.0.0-dev`（提交 56f7a4a9）、LopyLuna/Create-Dreams-and-Desires 的 `1.21.1`、DakotaPride/create-garnished-forge 的 `1.21.1`、WitherRedstone/Create-MoreCatalysts 的 `1.21.1-neoforge`、adonis-baffin/CreateShimmer 的 master、Create-Nuclear-Team/CreateNuclearNeoForge 的 main、Aupoex/CreateNetherIndustry 的 Create_6.0.6 分支接口。仅按接口独立编写适配，不复制上游实现代码或美术。

当前开发依赖没有安装以上七个附属。基础构建和无附属测试不等同合装验证；各模组发布版本与 Create 6.0.10 的兼容范围、实际加工、实体副作用及 Connected 转换仍需分别合装检查。Dye Depot 及方案中其后的其他模组不在此轮范围。此轮不新增缺乏上游定义的媒介获取配方。

本次 `build runGameTestServer --offline` 成功，全部 27 项测试通过，包括缺失可选模组时的准确类型查找回退、普通水不被兼容层覆盖和流沙整组打磨。未测试七个附属的实际合装环境。
