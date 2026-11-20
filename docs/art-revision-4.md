# aaa (4) 导入

双向动力泵四份基础模型原样导入，新增 body/gauge/shaft 三张独立贴图，物品使用正向主体并补静态轴。没有生成逐朝向副本。

便携泵导入新底座、气囊及汽笛材质压板。旧目录内气囊资源迁移至 portable_fluid_pump，渲染器同步；缺失的 reciprocating_fluid_pump/base 引用改为已提供的 portable_fluid_pump/base。物品主体、气囊、压板同步，保留两段静态轴。两格活塞复进泵未修改。

红石双向泵规范盘体上下坐标，恢复不透明底板，将遮挡面放到盘面上方。沿用此前确认的绿色双档，移除本次导入的红色覆盖层，指针与物品同步。

流沙桶和沸腾试剂桶 PNG 原样导入，所有用户 PNG 不重绘。导入前已有文件备份于 outputs/art-revision-4/backup。资源引用检查通过，离线预览位于同目录 preview.png。未构建或启动客户端，JAR 未更新。
