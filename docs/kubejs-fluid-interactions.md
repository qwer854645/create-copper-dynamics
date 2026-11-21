# KubeJS 双流体世界交互

在 `kubejs/server_scripts/` 顶层调用：

```js
CCDFluids.interaction('example:lava_water',
  'minecraft:lava', 'minecraft:water',
  'minecraft:obsidian', 'minecraft:cobblestone', 'minecraft:stone')
```

六个参数依次为：规则 ID、第一流体、第二流体、第一流体为源时的结果、第一流体为流动态时的结果、第一流体向下流入第二流体时的结果。

- 侧面／上方接触：替换第一流体所在方块，不消耗第二流体，与原版熔岩遇水规则一致。下面有第二流体不触发这项规则。
- 向下流入：替换下方第二流体所在方块；只有原流动规则允许实际调用 spreadTo 时触发，不强行允许所有流体互相覆盖。熔岩覆盖方法也单独拦截，避免原版先生成石头。
- 空字符串 `''` 表示该情形不设置脚本反应，继续原生机制。结果为方块注册 ID，使用默认 blockstate；暂不支持方块属性、流体标签、脚本回调或双向自动注册。
- 匹配包含同家族源／流动态。规则有方向，交换两个流体是另一条规则。相同配对多条规则按规则 ID 字典序决定优先；相同 ID 重新注册替换原规则。
- 脚本规则先于 NeoForge 原生接触反应。`CCDFluids.remove(id)` 只删除脚本规则，不能删除原版反应；`CCDFluids.clear()` 删除全部 CCD 脚本反应。
- SERVER 脚本加载前自动清空，服务器退出清空，支持 `/reload`。已静止且没有邻居更新的流体不会仅因重载立即反应，需要下一次相应世界更新。
- 影响普通 LiquidBlock 使用的 NeoForge 交互入口和继承原生 spreadTo 的流体。完全重写接触／流动机制、且不调用这些入口的第三方流体不保证兼容。
- 非法 ID、未知流体／方块和同家族配对在注册时报告错误。新流体仍需 startup 注册并重启。

示例文件：`examples/kubejs/server_scripts/ccd_fluid_interactions.js`。

验证：Java 编译及基础环境 GameTest 通过，覆盖原生接触入口、源／流动结果、下方目标替换和移除规则。尚未实际 Rhino 脚本联调或第三方流体全量验证，JAR 未更新。
