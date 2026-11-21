# KubeJS 兼容检查与接口说明

当前目标：Minecraft 1.21.1、KubeJS NeoForge 2101.7.2-build.382。插件通过 `kubejs.plugins.txt` 发现，只在 SERVER 脚本绑定 `CCDAtomizer`；未安装 KubeJS 时公共代码不引用其类。

## 支持内容

- `ServerEvents.recipes` 中使用 `event.custom({type:'ccd:atomizing', ...})` 添加规则；通过常规 remove／ID 替换修改规则。没有 `event.recipes.ccd` 专属构建器。
- `fluid` 使用 NeoForge FluidIngredient，可按流体或标签匹配。
- `priority` 越高越优先，同优先级按配方 ID 排序。第一条匹配规则为最终选择，不叠加；`enabled:false` 会阻止该流体回退至内置加工。
- `processing_type` 指向已注册风扇加工类型，如 `create:splashing`、`ccd:bulk_gluing`。只对没有匹配自定义物品规则的输入回退，未知类型忽略。
- `minimum_rpm` 0–256；`consumption.amount` 1–4000 mB，`interval` 1–1200 tick；费用按雾化器活跃时段支付，不是每个加工物品支付。
- `items` 按数组顺序匹配第一条：`input`、`results`（stack 与 chance）、`processing_time` 1–72000 tick，或 `script` 回调 ID。脚本优先于同条 results。
- `effects`、`damage`、`entity_types`、`entity_script`、`entity_interval` 定义气流中的生物效果；类型列表为空时不限制类型。只处理气流中的 LivingEntity。固定效果与脚本效果可同时生效，伤害使用 magic 类型。
- `CCDAtomizer.itemHandler(id, ctx => ...)`：返回 `output(ItemStack)` 或 `outputs(ItemStack...)`；回调一次接收整叠输入，返回值也是整叠的总产量，不会再次乘输入数量。
- `CCDAtomizer.entityHandler(id, ctx => ...)`：可返回 `effect(id,ticks,amplifier)`、`damage(amount)`。当前没有 JS 对象自动转换或组合效果辅助构建器。
- 物品上下文有 getInput／getFluid／getSpeed／getPosition／getDimension；生物上下文另有 getEntityType／getUuid／getHealth。物品与流体为副本，不提供实体或世界的可变引用。
- 回调返回 null 或抛出 RuntimeException：放弃本次处理，保留原物品；异常每个 ID 在重载前只记录一次。空输出或 EMPTY 输出表示主动销毁输入。
- SERVER 脚本加载前清空旧回调，资源重载刷新规则缓存，服务器停止清空回调。startup 注册流体需要重启，不能仅 /reload。

## 本轮修正

1. 旧 active 规则的 minimum_rpm 检查会提前停止本刻，使重载后的新低转速规则延后一刻生效。现在仅在规则版本未变化时复用旧检查。
2. 直接替换回调未刷新规则版本，旧加工进度可能继续。注册回调现在同步 invalidate。
3. processing_type 查询改用注册表安全查找，未知 ID 返回 null，不调用可能抛异常的 parse 路径。

## 限制与尚未验证

兼容是雾化器专用，不提供管道、泵、世界接触效果或手持烫伤的专用 KJS 事件。普通配方／标签可以用 KubeJS 常规数据接口修改。没有自动把工作台配方变为雾化配方的 KJS API；批量粘接由独立内置加工读取工作台配方。

KubeJS 回调驻留服务端；客户端只同步配方数据和雾化状态。脚本加载失败后回调可能部分缺失，不具备事务回滚。实体脚本返回 null 会跳过该实体本次固定效果，若需不追加伤害请返回 `CCDAtomizer.damage(0)`。

示例：`examples/kubejs/startup_scripts/ccd_atomizer_fluids.js`、`examples/kubejs/server_scripts/ccd_atomizing.js`。依赖 Rhino；KubeJS build.382 要求已安装 JEI 时版本至少 19.25.0.322，旧开发 JEI 19.21 不满足要求，当前全模组开发环境为 JEI 19.27。

验证：本轮基础环境 GameTest 通过，新增副本隔离、回调替换失效与未知类型测试。此前全模组客户端日志确认 KubeJS 找到 CCD 插件，但仅加载默认 Hello World 脚本；尚未在真实 Rhino 环境执行上述业务示例，也未验证多人重载及客户端配方同步。因此不能把 Java API 测试视为完整 JS 联调通过。JAR 未更新。
