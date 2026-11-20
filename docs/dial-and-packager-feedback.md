# 仪表动画与打包设备显示

三通阀静态盘面不再包含指针，独立 pointer partial 由 ThreeWayValveRenderer 在两个相对空闲面绘制，客户端 LerpedFloat 按 POWERED 平滑追踪 0/1。盘面和管口颜色保持固定。物品模型仍保留未通电指针。

流体打包机水平／竖向状态绑定已有 packager_horizontal_powered、packager_vertical_powered 图集，未通电继续使用 unpowered，不重画原图。

流体传输器过滤中心向背面（大头）偏移四像素，使图标与点击区域对应大头侧面预留位置，六向统一按 FACING 计算。

流体理包机原先使用原生 PackagerRenderer，它在 Flywheel 可用时不画舱门和托盘，而 CCD 未注册其 visual。现在与流体打包机共用 CCD 完整渲染路径，理包机使用原版舱门与理包托盘，不再依赖不存在的 visual。

46 项 GameTest 和构建通过；客户端资源加载通过。尚未在世界内实际观察指针过渡、过滤点击和理包动画。
