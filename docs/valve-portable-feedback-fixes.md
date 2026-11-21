# 三通与便携泵反馈修正

三通仪表从 blockstate multipart 中移除，改由 ThreeWayValveRenderer 在两个空闲面绘制同一份盘面。盘面与指针使用相同正交基，避免静态基准旋转与动态指针不一致；盘面增加不透明铜底板，避免透明区域暴露不同方向的底面。主体和三个管口仍由原生 multipart 绘制。未复现用户所见的错误贴图截图，因此客户端异常方向需要复验。

便携泵显式处理扳手 useItemOn，委托原生 onWrenched/onSneakWrenched；手动 useWithoutItem 增加主手空物品限制，避免拦截工具交互。流量默认提高至 1000 mB/t，单侧距离保持 4 格。已有配置保持原值，需要手动调整 portablePump.flow。编译和资源引用检查通过；未启动客户端、未重新打包。
