# tools/ 说明

## 贴图策略：优先引用 Create 原生贴图

2026-11 的贴图返修确立了一条原则：**能引用 Create 原生贴图的，就不要自己画。**

原先 CCD 有一批 16×16「铜板」是自己生成的（2px 平涂边框 + 一块低振幅色块），实测
单色占比高达 57%~94%、`std` 只有 4~12，读起来像塑料。现在这些面直接在模型里指向
Create 的贴图：

| 用途 | 引用的 Create 贴图 |
|---|---|
| 平铜面（龙头、三通阀、水槽、集液栅、水盆、流体访问口、增压泵、传讯器、流体舱口） | `create:block/copper_underside` |
| 平黄铜面（智能龙头、智能舱口） | `create:block/brass_block` |
| 灰色机件（移液臂金属件） | `create:block/industrial_iron_block` |

模型里只换了 `textures` 的值，几何、UV、`render_type` 一律没动。

## 还剩下的自绘贴图

只保留 6 张，且全部由 Create 的像素派生（没有新调色板、没有手绘笔触）：

| 贴图 | 来源 |
|---|---|
| `block/booster_pump/shaft_face.png` | Create `copper_underside` + 原生 gearbox 轴孔 |
| `block/fluid_transmitter/side.png` | Create `copper_underside` 2×2 铺贴 + CCD 箭头 |
| `block/fluid_transmitter/top.png` | 同上 |
| `block/smart_collection_grate/body.png` | Create `brass_block` 2×2 铺贴（32×32 面） |
| `block/copper_water_supply/cap.png` | 保留同心凹槽形状，重新打光到 Create 的 8 阶铜色 |
| `block/fluid_arm/core.png` | 保留深色轴孔形状，重新打光到深色阶 |

## 生成器

| 脚本 | 职责 | 依赖 |
|---|---|---|
| `copper-material.py` | 上面 6 张派生贴图；色板与 `metrics`/`symmetry` 工具 | Python + Pillow + Create 源 |
| `recolor-rank.py` | 智能储罐 4 张按「秩」重新映射到黄铜（从 Create 像素出发） | Python + Pillow + Create 源 |
| `repaint-smart-tank.py` | `recolor-rank.py` 的旧入口包装 | 同上 |
| `symmetrize-fluid-housings.py` | 增压泵 / 传讯器的原生细节合成 | 同上 |

用法（第一个参数是资产根，第二个是 Create jar 或解包后的资源根）：

```powershell
python tools/copper-material.py src/main/resources/assets <create.jar>            # 写入
python tools/copper-material.py src/main/resources/assets <create.jar> --check    # 只看指标
python tools/copper-material.py src/main/resources/assets <create.jar> --verify   # 比对，有偏差返回 1
python tools/recolor-rank.py <create.jar>                                          # 智能储罐
```

`symmetrize-fluid-housings.py` 和 `repaint-smart-tank.py` 不给 Create 源时会自己到
`~/.gradle/caches/.../com.simibubi.create/` 里找 jar。

## 关于 `create-*.cjs`

这些脚本仍然负责**模型、blockstate、物品模型和离线预览**。它们内部还留着旧的
`const copper=[[...]]` 数组和 `patches` 字符图，用来生成已经不再被引用的中间贴图
（跑完会重新写回 `textures/` 里的孤儿文件）。**改完模型后不需要再跑它们**；如果你确实
要改模型，跑完之后请再执行一次 `copper-material.py`，并用项目根目录的完整性检查确认
没有 `ccd:` 贴图引用指向已删除的文件。

这些脚本需要解包好的 Create 资源根作为第一个参数，并依赖 `sharp`——当前仓库没有
`package.json`，本地跑不起来；改动后请用 `node --check` 做语法校验。
