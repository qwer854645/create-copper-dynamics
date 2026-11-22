# 附属加工产物过滤

验证目标：Dragons Plus 1.11.9、Dreams n' Desires 2.3a-BETA、Garnished 2.1.9.2、Shimmer 1.3.1（依赖 Twilight Forest）。

新增 ThirdPartyOutputFilters，仅识别经发布包检查的具体类，不按模组命名空间笼统放行。DnD 的吐息／冰冻／打磨／超级加热，Garnished 冰冻与染色，以及 Shimmer 幽暗／转化走 RecipeApplier，沿用现有概率产物声明检查。

Dragons Plus 冰冻／末影／打磨存在 Optional 回退链，需在执行前检查首选真实配方的可能产物，防止首选配方因过滤返回 null 后转去别的处理。无首选时只读取已核对的 DnD／Garnished 配方回退及砂纸配方；未知扩展回退不放行。

Dragons Plus 染色使用其公开 processForDyeFluidMixing 预览 API，返回 ProcessingOutput 声明列表，检查概率大于零的可能产物。这个 API 查询实际配方、兼容声明及自动染色工作台结果，不调用 process 或掷骰。传入输入副本，方法反射延迟缓存。执行仍使用原生 process，不克隆概率结果，不丢副产物。

自定义雾化规则的 nativeFallback 也执行同样的路径检查，避免通过 ccd:atomizing 间接绕开限制。没有新增模式切换或跨加工类型自动选择。

验证：附属组合环境和完全无上述附属环境各 113 项 GameTest 通过。组合测试遍历 390 条可匹配配方路径，检查过滤拒绝时传送带输入保持原样、匹配路径允许、实际 process 返回结果；自动红色羊毛染色检查匹配及不匹配预览、掉落物拒绝保护。样本数量是配方路径数量，不代表全部种类的概率分布或所有输入均覆盖。未安装 Aether，其附魔／孵化处理没有开放；未知兼容注册回调仍保持过滤器开启时暂停的行为。

普通风扇不在雾化器上下文内，不受该产物过滤影响。实体效果未修改，JAR 未更新。
