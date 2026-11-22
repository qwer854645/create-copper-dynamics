# Create Liquid Fuel 3.0.0 兼容修复

用户 alpha.16 崩溃由 BurnerStomachHandler.tick 将 CCD FluidTank 强转 SmartFluidTank 导致。修复将实际储罐改为 Create SmartFluidTank，保留 FuelTank NBT 键与 4000 mB 容量。

仅改类型不足以修复逻辑：该模组还在 tryUpdateFuel 中把燃料桶内容灌回接口储罐。现在仅对 CCD 储罐燃烧室跳过其通用 tick 与桶燃料拦截；CCD 读取 LiquidFuels.get 的实际燃料数据，以 amountConsumedPerTick、burnTime、superHeat 执行供燃。不经过桶的燃料也可根据其数据表被接收，避免重复耗液或将桶内容重新写回同一罐。

未登记在 Liquid Fuel 中的流体继续使用 CCD 原有按桶燃料路径。普通燃烧室若由 Liquid Fuel 提供 SmartFluidTank，则仍走其原生逻辑；只有能力实际为 CCD SuperLavaFuel 时跳过强转逻辑。第三方数据 API 通过可选反射访问，无安装依赖。

安装发布包 createliquidfuel-3.0.0-1.21.1、NeoForge 21.1.264 时 94 项 GameTest 通过；不安装时基础环境同样 94 项通过。覆盖储罐燃烧室 tick、储存、供热、普通燃烧室的延迟燃烧与原有超热介质路径。未验证用户整合包全部模组组合。

需要更换修复后的 CCD JAR 并重启才能修复现有世界；不需要删除燃烧室或更改存档。alpha.16 旧包本身不会自动获得修复。
