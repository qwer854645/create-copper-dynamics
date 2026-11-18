# 流体雾化器与 KubeJS

## 机器

雾化器继承 Create `EncasedFanBlock`／`EncasedFanBlockEntity`。背面接入动力，正反转的吹吸、气流范围、遮挡、物品移动和原生加工计时均沿用鼓风机；应力系数也与鼓风机相同。

内置 4000 mB 单流体罐，使用 `SmartFluidTankBehaviour` 保存完整流体组件。四个非旋转轴方向的侧面提供标准流体接口；前方风口与后方转轴不提供有向流体接口。侧面普通右键为手持容器装液，潜行右键倒入液体；也支持管道、铜桶和流体舱口。

原生催化流体标签分别映射到水洗、熔炼、烟熏、缠魂加工；例如水和熔岩可直接使用。多重标签按原生加工类型优先级选择。其他模组新增的加工类型可以通过下面的规则 ID 显式映射，不假定任意注册类型都能自动从流体推导。

默认每 20 tick 预付一段喷雾的液体：普通催化流体 10 mB，Create 药水流体 25 mB。世界 `serverconfig/ccd-server.toml` 的 `[atomizer] normalConsumption`／`potionConsumption` 可修改，重开世界生效。规则可覆盖耗液量和间隔。

- 有动力、无红石信号且液体匹配规则时开启喷雾，没有目标也消耗喷雾液。
- 当前一段已经扣除的液体会继续作用到该段结束，即使这次扣液使储罐变空；不会从空罐免费开启下一段。
- 无动力或红石信号立即停止雾化；换成不同流体或重载规则后重新判定。停止的旧段不返还已雾化的液体。
- 雾化暂停时仍保留原生普通风，沿途真实世界催化媒介也仍按原生规则转换气流。实体与物品实际所处的世界媒介优先于雾化器提供的初始加工类型。
- 原生药水流体读取 `POTION_CONTENTS` 的全部效果与等级。持续效果最多保留 60 tick 并每 20 tick 刷新，瞬时效果每 20 tick 一次、强度系数 0.25。采用原版药效实现，不逐 tick 创建滞留药云实体。
- 拆除仅掉落雾化器本体，内部液体随设备清除。合成需要鼓风机、分液池、精密构件各 1 个和铜机壳 2 个。

当前外观是铜色原生鼓风机外壳与原生转子，雾粒子使用流体自身的粒子表现；护目镜显示库存和运行状态。没有独立配置 GUI，也没有 JEI 自定义配方分类。

## 数据包与脚本配方

注册的配方类型为 `ccd:atomizing`，每个配方描述一种流体媒介的用途。JSON 放入 `data/<命名空间>/recipe/`；也可通过 KubeJS 的 `ServerEvents.recipes(event => event.custom({...}))` 添加。

最小例子，将巧克力映射为原生烟熏：

```json
{
  "type": "ccd:atomizing",
  "fluid": {"fluid": "create:chocolate"},
  "processing_type": "create:smoking",
  "consumption": {"amount": 10, "interval": 20}
}
```

可直接复制 `examples/datapack/` 作为测试数据包。这个示例不会自动加入正常游戏。

字段：

- `fluid`：必填，NeoForge `FluidIngredient`。支持单个流体、流体标签及该接口提供的复杂条件；例如 `{"tag":"create:fan_processing_catalysts/splashing"}`。
- `processing_type`：已有 Create 加工类型 ID。原生值包括 `create:splashing`、`create:blasting`、`create:smoking`、`create:haunting`。第三方类型必须已在原生注册表注册。
- `consumption`：`amount` 为 1～4000 mB，`interval` 为 1～1200 tick，默认 10 mB／20 tick。运行按间隔预付，不额外按每件产物重复扣液。
- `priority`：默认 0。流体同时匹配多个定义时取最高优先级；同优先级按配方 ID 排序，保证顺序稳定。
- `enabled`：默认 true。高优先级匹配规则设为 false 时，可禁用某种媒介，包括其原生标签回退。
- `minimum_rpm`：默认 0，范围 0～256，使用转速绝对值。
- `items`：自定义物品规则列表，按列表顺序取首个匹配项。若物品未匹配任何自定义项，回退到 `processing_type`；没有回退类型就不加工该物品。
- `effects`：原版 `MobEffectInstance` 列表，例如 `[{"id":"minecraft:speed","duration":60,"amplifier":0}]`。持续时间为 tick，等级从 0 开始。这里使用配置原值，不应用默认药液的 60 tick 截断。
- `damage`：每次生物作用时的魔法伤害，默认 0，不能为负值。
- `entity_types`：可选实体类型 ID 列表，空列表表示全部生物；例如 `["minecraft:player"]`。
- `entity_interval`：1～1200 tick，默认 20。生物规则和回调只按此间隔运行。
- `entity_script`：可选 KubeJS 生物处理器 ID，见后文。

没有匹配的数据定义时，才回退到内置的原生催化标签和 Create 药水流体。未知加工类型且没有其他处理动作时不雾化，不消耗液体。

## 自定义物品加工

`items` 中每个对象包含：

- `input`：必填，原生物品 `Ingredient`，支持物品或标签。
- `results`：产物列表，每项为 `{"stack":{"id":"minecraft:clay_ball","count":1},"chance":0.5}`。使用原生 `ItemStack` codec，可保留产物组件。静态产物对输入堆中的每件物品独立掷概率，并按最大堆叠数拆分结果。
- `processing_time`：1～72000 tick，默认 100。输入整堆的时间倍率沿用原生鼓风机，每增加 16 件增加一份基础时间。
- `script`：可选处理器 ID。设置时由脚本提供全部产物，静态 `results` 不参与。

`results` 和 `script` 至少有一项。例子：

```json
{
  "type": "ccd:atomizing",
  "fluid": {"fluid": "minecraft:water"},
  "items": [{
    "input": {"item": "minecraft:cobblestone"},
    "results": [{"stack":{"id":"minecraft:clay_ball","count":1},"chance":0.5}],
    "processing_time": 100
  }]
}
```

物品仍通过原生 `FanProcessing` 转换，包括掉落物、置物台和传送带上的物品。CCD 只为自定义媒介提供上下文、匹配规则和计时初值；不复制原生物品生成与传送带转换代码。

## 可选 KubeJS 回调

安装兼容的 KubeJS 1.21.1／2101.7.2 后，服务端脚本中提供 `CCDAtomizer` 绑定。没有安装 KubeJS 时，核心机器和上述数据包规则照常工作，不加载兼容类。

```js
CCDAtomizer.itemHandler('example:clay', ctx => {
  return CCDAtomizer.output(Item.of('minecraft:clay_ball', ctx.getInput().getCount()))
})

CCDAtomizer.entityHandler('example:regeneration', ctx => {
  return ctx.getHealth() < 10
    ? CCDAtomizer.effect('minecraft:regeneration', 60, 0)
    : CCDAtomizer.damage(0)
})
```

配方物品规则写 `script: 'example:clay'`；生物规则写 `entity_script: 'example:regeneration'`。

物品上下文：`getInput()`、`getFluid()`、`getSpeed()`、`getPosition()`、`getDimension()`。生物上下文：`getEntityType()`、`getUuid()`、`getHealth()`，以及相同的流体、转速、机器位置和维度字段。

输入和流体都是副本；流体是当前已付费喷雾段的液体样本，不是可提取的库存引用。上下文不提供真实物品槽、可直接修改的生物或原始世界对象。回调返回结果后再由 CCD 提交：

- `CCDAtomizer.output(stack)`：一个产物。
- `CCDAtomizer.outputs(stack1, stack2, ...)`：多个产物。
- `CCDAtomizer.effect(id, ticks, amplifier)`：一个药效，瞬时效果强度系数 0.5。
- `CCDAtomizer.damage(amount)`：魔法伤害，可为 0 表示不处理。
- 回调返回 `null` 表示不提交该次处理。显式返回 `outputs()` 空列表则按原生语义消耗原料而不产出物品。

物品回调对完成加工的整个输入堆调用一次，产物数量由回调决定，不自动再乘输入数量。生物回调每个作用间隔对符合类型和范围的生物调用一次。没有逐 tick 的 JavaScript 回调，也不生成额外药云实体来搜索生物。

回调异常时不提交本次物品／生物结果，日志记录处理器 ID 和机器位置，同 ID 的错误限频到重载为止；已经作为实际喷雾支付的液体不返还。Java API 位于 `dev.ccd.api.AtomizerScripts`，兼容插件只提供绑定和生命周期，不把 KubeJS 类型带入核心实现。

## 重载与例子

`examples/kubejs/startup_scripts/ccd_atomizer_fluids.js` 演示注册新流体，需要重启。`examples/kubejs/server_scripts/ccd_atomizing.js` 演示完整规则、静态概率产物、物品回调和生物回调，可通过 `/reload` 更新。

兼容插件在服务端脚本加载前清空旧回调；数据包重载使规则缓存失效。规则缓存按配方管理器保存，机器只在流体身份或规则版本变化时重新匹配。原生自定义加工类型只在模组启动期注册一个共享适配器，脚本规则重载不修改 Create 已冻结的注册表。

换液、规则或脚本重载会重新判定自定义加工计时，避免旧媒介的进度直接生成新媒介产物。原生加工类型的计时仍保持原生规则。

本版只做代码与资源静态检查；没有编译、运行游戏、KubeJS 脚本实测或性能／视觉实测。
# 外观调整：后移转子与前部储液

最新调整：转子再后移 2 像素，累计后移 6 像素；前部液体展示深度增加到方块中线，容量仍为 4000 mB。桶、瓶等原生通用容器可从任意面右击，优先倒入、不能倒入时尝试取出；管道仍只开放侧面。铜桶从任意面进入其原有存取流程（普通右击取液，潜行右击倒液）。已编译并更新离线预览，未进行客户端交互验证。

原生风扇转子向背面移动 4 模型像素，普通渲染与 Flywheel 仅对 CCD 雾化器替换同一 partial model，普通鼓风机不受影响。前部按实际储罐流体和液量绘制水平液面，六种放置方向均使用世界竖直液位。Flywheel 下继续绘制非实例化液体；轴、风扇速度与气流规则保持原生实现。

背板使用 gearbox 原生轴孔，周围棕色像素改用 CCD 铜色纹理；轴孔暗部及灰色孔壁保留。模型／原图来源为 Create，未标为 CCD 原创。生成器 `tools/create-atomizer-model.py`，离线预览 `outputs/fluid-atomizer/preview.png`；蓝色液体仅为示意。Java 编译通过，未启动客户端验证透明层、转子动态或 Flywheel Mixin 实际效果。
