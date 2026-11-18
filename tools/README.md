# tools/ 说明

## 贴图策略：优先引用 Create 原生贴图

2026-11 的贴图返修确立了一条原则：**能引用 Create 原生贴图的，就不要自己画。**

原先 CCD 有一批 16×16「铜板」是自己生成的（2px 平涂边框 + 一块低振幅色块），实测
单色占比高达 57%~94%、`std` 只有 4~12，读起来像塑料。现在这些面直接在模型里指向
Create 的贴图：

| 用途 | 引用的 Create 贴图 |
|---|---|
| 平铜面（龙头、三通阀、水槽、集液栅、水盆、流体访问口、增压泵、流体舱口） | `create:block/copper_underside` |
| 平黄铜面（智能龙头、智能舱口） | `create:block/brass_block` |
| 灰色机件（移液臂金属件） | `create:block/industrial_iron_block` |

模型里只换了 `textures` 的值，几何、UV、`render_type` 一律没动。

## 还剩下的自绘贴图

只保留 3 张派生贴图 + 4 张集液栅专用贴图，全部使用 Create 的现成色板（没有新颜色）：

| 贴图 | 来源 |
|---|---|
| `block/booster_pump/shaft_face.png` | Create `copper_underside` + 原生 gearbox 轴孔 |
| `block/copper_water_supply/cap.png` | 同心凹槽按环几何生成，落在 Create 的 8 阶铜色 |
| `block/fluid_arm/core.png` | 深色轴孔按环几何生成，落在深色阶 |

### 集液栅（一格高模型专用）

`d48566a` 把集液栅改成了一格高（16px 深），侧壁 UV 是按 **每个模型像素一个纹素** 重新算的，
所以贴图必须按模型实际采样的条带来排布，不能再用通用铜板：

| 贴图 | 被谁采样 |
|---|---|
| `block/copper_collection_grate/wall.png` | 外壳四壁（上层 v3..9、下层 v5..11）+ 悬挑底面（v1..3） |
| `block/copper_collection_grate/rim.png` | 顶部凸沿的上表面，只采样最外 2 行/2 列组成的环 |
| `block/smart_collection_grate/body.png` | 智能版凸沿的侧面（只采样 v2..3） |
| `block/smart_collection_grate/rim.png` | 智能版凸沿的上下表面（同上，环） |

`create:block/copper_underside` 在这里不可用：它的高光正好落在第 1 行，而那一行是悬挑底面的
位置（应该是暗的），而且它没有垂直衰减，会把两层读成一整块平板。

另外 `block/fluid_transmitter/{body,side,top}` **保持原设计，不参与派生**：那两张 32×32 面带
一块黄色指示面板和箭头标记，用 `copper_underside` 2×2 铺贴会把指示器整个覆盖掉，箭头还会正好
压在铺贴接缝上变成一坨十字。这三张连同模型里的 `body` 键都已回退到返修前的版本。

### 流体舱口（继承 Create 物品舱口模型）

`6e4f834` 之后两个舱口模型只有一行 `{"parent": "create:block/item_hatch/block_closed"}`，
所以默认渲染得和 Create 物品舱口一模一样。父模型只开放 3 个可覆盖键，全部由
`tools/create-fluid-hatch-textures.py` 填：

| 键 | 贴图 | 做法 |
|---|---|---|
| `0` | `block/{fluid,smart_fluid}_hatch/housing.png`（32×32） | Create `hatch.png` 的**中间灰**换成铜/黄铜，黑与银原样保留 |
| `1` | `block/{fluid,smart_fluid}_hatch/flap.png`（16×16） | 同上 |
| `particle` | 同 `housing` | — |

两个关键约束（都是渲染探针测出来的，别凭直觉改）：

1. **正面是 `#0` 不是 `#1`**。把 `#0` 涂成品红、`#1` 涂成青色后渲染，正面/侧面全是品红，
   青色只出现在斜门上。所以任何"管口/门面"细节都必须画在 `#0`。
2. **只能换中间灰，不要整体重映射**。Create 的舱口美术是三档材质，不是一条灰阶：

   | 亮度 | 是什么 | 占比 | 处理 |
   |---|---|---|---|
   | ≤ 56 | 黑色凹槽 / 格栅缝 | 11% | **原样保留** |
   | 56~145 | 工业铁本体 | 78% | 换成铜 / 黄铜 |
   | ≥ 145 | 裸银高光 / 包边 | 8% | **原样保留** |

   第一版把三档全部压到铜色阶上，结果像套了层滤镜、结构全糊；保留黑与银之后格栅和包边都回来了。

另外**不要往正面盖管口凹槽**。那个窗口（x5..12/y5..14）正是 Create 画的物品格栅百叶，
压一个洞会把结构盖掉、看起来像铜板上挖了个方孔。想强化"流体"语义的话，角落加一颗小水滴
标记，别动门面本身。

色阶要**抬高起点**：饱和的铜色在同等亮度下看起来比中性灰更沉，如果色阶从最深的
`#3D161E`（亮度 31）起，整块舱口会发闷——实测平均亮度只差 3.3，观感却明显偏暗。
所以只取中上段：

- 铜 `#9A5038`…`#E3826C`（亮度 94..149，7 阶）
- 黄铜 `#9E6947`…`#FFEB8C`（亮度 114..232，11 阶）

两段都来自 Create 自己的 `hatch` 配色体系，没有引入新颜色。

## 生成器

| 脚本 | 职责 | 依赖 |
|---|---|---|
| `copper-material.py` | 上面 3 张派生贴图；色板与 `metrics`/`symmetry` 工具 | Python + Pillow + Create 源 |
| `create-collection-grate-textures.py` | 集液栅 4 张贴图，按模型的 UV 分带排布 | Python + Pillow |
| `create-fluid-hatch-textures.py` | 两个流体舱口各 2 张（housing / flap） | Python + Pillow + Create 源 |
| `recolor-rank.py` | 智能储罐 4 张按「秩」重新映射到黄铜（从 Create 像素出发） | Python + Pillow + Create 源 |
| `repaint-smart-tank.py` | `recolor-rank.py` 的旧入口包装 | 同上 |
| `symmetrize-fluid-housings.py` | 增压泵轴孔的原生细节合成 | 同上 |

用法（第二个参数是 Create jar 或解包后的资源根）：

```powershell
python tools/copper-material.py src/main/resources/assets <create.jar>            # 写入
python tools/copper-material.py src/main/resources/assets <create.jar> --check    # 只看指标
python tools/copper-material.py src/main/resources/assets <create.jar> --verify   # 比对，有偏差返回 1
python tools/create-collection-grate-textures.py src/main/resources/assets        # 集液栅 4 张
python tools/create-fluid-hatch-textures.py src/main/resources/assets <create.jar>  # 流体舱口 4 张
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
