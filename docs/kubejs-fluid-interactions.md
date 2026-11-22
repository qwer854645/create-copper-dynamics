# KubeJS 双流体世界交互

## 流体接触方块

在 `kubejs/server_scripts/` 顶层调用（不放进 recipes 事件）：

```js
CCDFluids.blockInteraction('example:moss_contact', 'ccd:moss_suspension',
  '#minecraft:moss_replaceable', 'both', null, 'minecraft:moss_block')
```

参数依次为：规则 ID、流体 ID、目标方块 ID 或 `#方块标签`、状态模式、流体位置结果、目标方块位置结果。模式为 `source`（仅源液）、`flowing`（仅流动液）、`both`（两者）。流体 ID 匹配同一家族，以模式进一步区分状态。

两个结果分别设置：`null`／空字符串保留该位置；`minecraft:air` 移除；其他方块 ID 使用默认状态。可只改其中一边，也可同时改两边。流体结果可使用液体方块 ID，其默认状态一般为源液；目前不支持指定 blockstate 属性。两结果都为空会删除这个 ID 的方块规则。

六面直接相邻即可触发，包括下方。脚本方块规则先于 CCD 双流体规则和 NeoForge 原生接触反应；按规则 ID 字典序、方向枚举顺序寻找第一个实际改变世界的匹配，一次接触入口只执行一组变更。规则执行期间阻止自身及双流体规则递归，避免邻居通知造成无限递归。两位置结果先经过 NeoForge fluid-place 事件再顺序写入；不是跨方块事务，不保证第三方事件副作用可回滚。

该 API 是明确的方块替换规则，不自动执行骨粉逻辑；上述示例只改苔藓块，CCD 苔藓液自己的原生行为仍负责其骨粉扩散。`remove`／`clear`、服务器退出和脚本重载同时管理两种规则。静止流体在重载后需要放置或邻居更新才重新检测。未加载区块不主动加载。入口要求第三方流体遵循 NeoForge LiquidBlock 接触机制。

完整示例：`examples/kubejs/server_scripts/ccd_fluid_block_interactions.js`。91 项基础 GameTest 已覆盖源液方块单侧替换、流动液双侧替换、标签、空气移除及规则删除；真实 Rhino 调用尚未联调。

## 双流体交互

在 `kubejs/server_scripts/` 顶层调用：

```js
CCDFluids.interaction('example:lava_water',
  'minecraft:lava', 'minecraft:water',
  'minecraft:obsidian', 'minecraft:cobblestone', 'minecraft:stone')
```

六个参数依次为：规则 ID、第一流体、第二流体、第一流体为源时的结果、第一流体为流动态时的结果、第一流体向下流入第二流体时的结果。

- 侧面／上方接触：替换第一流体所在方块，不消耗第二流体，与原版熔岩遇水规则一致。下面有第二流体不触发这项规则。
- 向下流入：替换下方第二流体所在方块；只有原流动规则允许实际调用 spreadTo 时触发，不强行允许所有流体互相覆盖。熔岩覆盖方法也单独拦截，避免原版先生成石头。
- 后三个结果参数均允许 `null` 或空字符串 `''`，表示该情形不设置脚本反应，继续原生机制。三个都为空时删除该 ID 的脚本规则。结果为方块注册 ID，使用默认 blockstate；暂不支持方块属性、流体标签、脚本回调或双向自动注册。
- 匹配包含同家族源／流动态。规则有方向，交换两个流体是另一条规则。相同配对多条规则按规则 ID 字典序决定优先；相同 ID 重新注册替换原规则。
- 脚本规则先于 NeoForge 原生接触反应。`CCDFluids.remove(id)` 只删除脚本规则，不能删除原版反应；`CCDFluids.clear()` 删除全部 CCD 脚本反应。
- SERVER 脚本加载前自动清空，服务器退出清空，支持 `/reload`。已静止且没有邻居更新的流体不会仅因重载立即反应，需要下一次相应世界更新。
- 影响普通 LiquidBlock 使用的 NeoForge 交互入口和继承原生 spreadTo 的流体。完全重写接触／流动机制、且不调用这些入口的第三方流体不保证兼容。
- 非法 ID、未知流体／方块和同家族配对在注册时报告错误。新流体仍需 startup 注册并重启。

示例文件：`examples/kubejs/server_scripts/ccd_fluid_interactions.js`。

验证：Java 编译及基础环境 GameTest 通过，覆盖原生接触入口、源／流动结果、下方目标替换和移除规则。尚未实际 Rhino 脚本联调或第三方流体全量验证，JAR 未更新。
