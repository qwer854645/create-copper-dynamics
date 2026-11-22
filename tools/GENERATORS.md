# 资源生成脚本使用约定

不要批量运行 tools 下所有生成脚本。旧 create-*、import-art-revision-* 和 register-* 脚本保留为历史来源，其输出可能覆盖用户批准的美术、配方或提示。

当前资源文件是最终依据。修改前先查看 git diff，明确生成器的写入路径；运行后检查差异，只提交授权内容。

储罐燃烧室最新外壳来自用户 `tank (1).json`，入口为 `import-tank-burner-user-model.py`。旧 `create-tank-blaze-burner.py` 会生成旧OBJ笼架并覆盖新版外壳／物品，禁止为常规更新运行。新模型保留所有用户坐标（包括反向界限）与UV，玻璃贴图来自下载目录；物品补原生idle烈焰人，世界原生动态渲染按底座5/16与上部11/16适配。静态预览不代表完整动画。旧资源备份位于outputs/tank-burner-user-model/backup。

## 当前维护入口

- 生存配方：`rebalance-recipes.py`。新增 `tank_blaze_burner` 配方当前由 `create-tank-blaze-burner.py` 提供；该脚本同时生成美术，不能仅为配方随意运行。
- 语言与思索：最终 Java 场景和 lang JSON 为准；`add-new-ponder-hints.py` 按场景顺序同步字幕。其他 update-* 属于特定修订，运行顺序可能覆盖新提示。
- 红石泵 ROLL 模型：`fix-redstone-pump-roll.py`；变流量泵 ROLL 模型：`fix-burner-hand-and-pump-roll.py`，后者也修改燃烧室手持显示。
- 变流量泵固定指针移除：`remove-static-variable-pointers.py`，重新导入机身后再核对是否带入固定指针。
- 用户桶图：`import-media-bucket-art.py`。禁止用 `create-static-fluid-buckets.py` 全量重生成后覆盖用户图。
- 新介质生成器只用于其明确列出的介质；生成后重新核对平衡配方、最终提示和已批准桶图，不能将旧生成配方视为权威。
- `preview-*` 原则上只生成 outputs；部分历史脚本导入会执行顶层代码，调用前检查副作用。

## 只读检查

`py tools/check-lang.py` 检查语言与思索字幕；`py tools/check-pump-model-frames.py` 检查两泵 48 个模型姿态与客户端矩阵。

## 可重复验证

GameTest 参数 `-PkjsProbe` 仅配合临时复制的 `examples/kubejs/server_scripts/ccd_integration_probe.js` 使用，结束后移除临时脚本。该示例故意注册报错回调用于验证失败返回契约。

`-PccdBenchmark` 输出粘接查询 CPU 样本；另加 `-PgluingBaseline` 使用旧式全配方扫描作对照。不要把一次查询基准换算成整套工厂 TPS 收益。
