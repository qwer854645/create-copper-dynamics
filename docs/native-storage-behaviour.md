# 储液管理改用 Create 原生行为

缓存泵与储罐烈焰人燃烧室现使用 SmartFluidTankBehaviour.single，实际库存来自 getPrimaryHandler，仍为 SmartFluidTank。燃烧室通过 setValidator 保留燃料判断；缓存泵通过原有侧面接口限制输出。容量仍为2000／4000mB。

删除 FluidSyncBehaviour 自定义8tick节流，序列化、客户端同步及原生液位动画状态由Create行为管理。旧syncBaseline参数删除，旧性能记录是历史自定义同步实现的数据，不能套到本实现。

遵循用户不保留旧存档兼容的要求，不读取旧手写 Buffer／FuelTank 保存键，不添加迁移。新保存／读取走原生行为结构。万向泵已没有储罐，无需新增储液行为。

基础与Liquid Fuel 3.0.0环境均116项GameTest通过，含储液保存、直接供液、燃料切换和原生接口兼容。原生TankSegment提供液位插值状态，但当前自定义渲染器仍按实际同步量绘制液面，并未自动切换到插值液面绘制。客户端视觉未实测，JAR未更新。
