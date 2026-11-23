# 取消流体反应

在 `server_scripts` 中调用：

```js
// 双向禁止水与熔岩反应，源液和流动液均匹配。
CCDFluids.cancelInteraction('pack:no_lava_water', 'minecraft:lava', 'minecraft:water')

// 禁止熔岩与蓝冰接触反应；方块也可写为 #namespace:tag。
// mode: both / source / flowing。
CCDFluids.cancelBlockInteraction('pack:no_basalt', 'minecraft:lava', 'minecraft:blue_ice', 'both')

// 撤销本规则；恢复其他规则正常处理。
CCDFluids.remove('pack:no_lava_water')
```

取消规则优先于 CCD 替换规则。匹配后不生成替代方块，不触发本次放置结果事件；其他未匹配接触反应仍可执行。流体对匹配双向，流体与方块规则仅检查指定流体。规则随服务器脚本重载清空并重新注册。

覆盖 NeoForge FluidInteractionRegistry 的标准接触反应，以及调用 FlowingFluid/LavaFluid.spreadTo 的流入行为。为保持匹配目标不被流动替换，向该目标的流入也会阻止，其他方向继续流动。多条件反应以传给标准注册表的当前邻居为匹配目标（例如玄武岩反应匹配蓝冰邻居，不是脚下灵魂土）。

这不是全局方块写入拦截器：第三方独立 tick/事件直接修改世界、或覆盖 spreadTo 且不调用父类的逻辑，仍需该模组专用适配。没有规则时原方法原样执行，不删除上游注册。
