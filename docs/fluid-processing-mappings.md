# 雾化器流体加工自动映射

新增数据包映射、公开 Java API、KJS 配置及明确标签约定的自动发现。不伪造世界调用 isValidAt，不执行 process 来猜测加工，不自动复制配方。

## 数据包

文件位置为 `data/<命名空间>/ccd/fluid_processing/<规则名>.json`，例如：

```json
{
  "fluid": "#example:coolants",
  "processing_type": "example:freezing",
  "priority": 10,
  "enabled": true
}
```

`fluid` 可以是具体流体 ID 或以 # 开头的流体标签。具体流体匹配其 isSame 家族，包括源／流动态；标签只匹配真正加入标签的流体，建议同时标记源和流动对象。`processing_type` 必须是 Create 注册表里已有的加工类型。未知流体／类型暂不匹配，便于可选模组数据共存；语法错误记录错误并跳过该文件。

支持 `/reload`；整个数据包规则集合在重载后替换，删除文件即删除对应数据规则。相同资源路径遵循资源包覆盖；enabled=false 禁用该数据资源。无需把文件放进 recipe 目录。示例位于 examples/datapack/data/example/ccd/fluid_processing/water_haunting.json，默认不参与游戏。

## 标签自动发现

约定：`#<类型命名空间>:fan_processing_catalysts/<类型路径>`。例如注册 `example:freezing`，并把新流体放入 `data/example/tags/fluid/fan_processing_catalysts/freezing.json`，雾化器即可在回退识别时自动发现。

只识别这一个明确约定，不推断任意标签名称或扫描世界。多个自动标签匹配时按完整标签 ID 字典序选择，不按加工类型优先级。自动发现位于现有 CCD／原生／专用附属识别之后，避免改变原有选择结果。

## KubeJS

在 server_scripts 顶层：

```js
CCDProcessing.script('example:coolant', '#example:coolants', 'example:freezing', 10)
// CCDProcessing.removeScript('example:coolant')
```

SERVER 脚本重新加载前清理旧脚本映射，正常重载后重新注册。`clearScripts()` 清空全部脚本映射。Java 模组可调用 `FluidProcessingMappings.register(id, fluidOrTag, processingType, priority)` 和 remove(id)，Java 注册不会随脚本重载清除。不要在脚本中使用 register 代替 script，否则不会参与脚本自动清理。

## 选择顺序及边界

自定义 `ccd:atomizing` 配方仍最高优先；随后选择显式映射；再走原有流体识别；最后尝试约定标签自动发现。同一显式规则 ID 的覆盖顺序为脚本 > Java API > 数据包。不同 ID 按 priority 降序、ID 升序决定。映射到 ccd:atomizing 本身会被跳过，防止无实际规则的自引用。默认使用普通介质耗液量和20 tick周期；需要定制耗液、RPM等仍使用雾化配方。

映射仅扩展雾化器，不修改普通风扇触媒识别，不注册新流体、不新增处理类型，也不根据产物过滤跨类型搜索。目标产物过滤继续使用已验证路径；未知处理类型在有过滤器时暂停，空过滤正常调用上游。上游配置、配方和概率结果仍由上游负责。

## 验证

真实 KubeJS build.382／Rhino 环境先通过 114 项测试；加入临时 JSON 映射与约定标签后115项通过，包括数据加载及自动发现断言。测试资源已删除，不会把水错误绑定为粘接介质。API 流体家族匹配、标签、优先级、覆盖、自引用跳过、删除，以及真实 JS 映射注册和资源重载均通过。夹具专项复现需临时添加上述资源并使用 -PmappingFixture。未安装任意未知附属验证其具体执行行为。错误探针抛异常为原有预期测试日志。

JAR 未更新。
