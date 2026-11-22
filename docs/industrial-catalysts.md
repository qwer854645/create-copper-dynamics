# 更多触媒工业联动

新增八项雾化器介质映射，直接调用更多触媒注册的 FanProcessingType，沿用其配置、配方、概率输出、粒子及实体行为，不新增切换控件。

- 沉浸工程杂酚油：`#c:creosote` → `create_more_catalysts:preservation`（防腐）。
- Mekanism 盐水：`#c:brine` → `create_more_catalysts:salout`（盐析，注意上游拼写）。
- 氢氟酸：`#c:hydrofluoric_acid` → `etch`（蚀刻）。
- 二氧化硫：`#c:sulfur_dioxide` → `sulfon`（磺化）。
- 硫酸：`#c:sulfuric_acid` → `dehyd`（脱水）。
- 氧：`#c:oxygen` → `oxidize`（氧化）。
- 氢：`#c:hydrogen` → `reduce`（还原）。
- Mekanism Generators 生物乙醇：`#c:bioethanol` → `ferment`（发酵，需要 Generators）。

代码匹配更多触媒自己的 `fan_catalysts/*` 流体标签，这些标签已引用上述公共标签。其他模组提供的相同标签流体也可以使用；前提是更多触媒实际注册了相应处理类型。没有安装对应依赖时，不创建假的加工类型。

实测 Mekanism 10.7.19.85 已为上述介质提供 NeoForge 源／流动流体，Generators 10.7.19.85 提供生物乙醇，因此无需转换化学品能力，也不另造 CCD 替代液体。使用该模组正常生产线制备介质，经标准流体管道存入雾化器。原生世界风扇触媒识别仍由更多触媒负责，本轮不修改其规则。

雾化器已有目标产物过滤继续生效：通过 RecipeApplier 执行的加工在掷骰前按配方可能产物筛选，允许后保留全部副产物。不增加模式选择，也不修改为基于过滤器自动跨类型搜索。上述八项在默认标签下各自对应不同介质，因此不需要新切换界面。实体效果直接继承上游，本轮未额外强化第三方实体效果。

验证：实际加载更多触媒 1.2.4、Mekanism 10.7.19.85、Generators 10.7.19.85、Immersive Engineering 12.4.2-194，使用 NeoForge 21.1.264，96 项 GameTest 通过。八项源／流动介质映射和雾化选择均断言检查；额外加工检查对上游存在且启用的配方测试拒绝过滤和空过滤执行，禁用或无配方的类型会跳过该加工检查，不能代表每种配方全覆盖。未进行客户端整套产线与每种实体变换的实机验证。
