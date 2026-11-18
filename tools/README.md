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
| `block/copper_water_supply/cap.png` | 顶部圆环 + 凹槽底共用的铜板，按环几何生成 |
| `block/copper_water_supply/body.png` | 潮涌箱专用的**边框图集**，按模型切片的 2px 条带排布 |
| `block/fluid_arm/core.png` | 深色轴孔按环几何生成，落在深色阶 |

`copper_water_supply` 原来把 `body` 指向 `create:block/copper_underside`，但它的 UV 是按边框图集
采样的（`v14..16` / `v0..2`），正好落在那张图的反弹光带上，于是整个箱体发白。现在改用专用边框图。
另外整块铜比默认深一档（`SUPPLY_TONE_SHIFT = -1`），见文档 §12。

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

### 智能储罐（铜身 + 黄铜机壳框）

**不是整块黄铜**。Create 的铜身原样保留，只有框带换成黄铜，暗缝换中性灰。按行分区：

| 行（16px 侧板） | 区域 | 材质 |
|---|---|---|
| 0~2 / 4 / 13~14 | 横向框带 | 黄铜金（`brass_block` 金段） |
| 3 / 12 / 15 | 板缝 | 中性灰（`industrial_iron_block`） |
| 5~11 | 板面 | **Create 原铜色，一像素都不动** |

**所有棱都包黄铜**，但横竖必须分开处理（`zone_side`）：

`fluid_tank.png` 是**边框图集**，不是一整张面。模型 UV：第 0~3 行 = 顶盖侧带，12~15 行 = 底部带，
第 0~3 / 12~15 列（4~11 行）= 竖直棱条，中间 4~11 行被玻璃窗挡住、不采样。按"整面描边"处理
会给模型不读的像素上色，真正的横带反而错。

| 轴 | 取值 | 原因 |
|---|---|---|
| 竖直棱 | 亮金 → 次亮 → 1px 暗缝（`POST_HI/POST_LO`） | 每块自己的角柱，要有倒角深度，不然显"浅" |
| 横向棱 | 暗金 `EDGE_DIM`，仅 2 行 | 和上下方块共用，每层亮金会变成一摞横条 |

调深浅就改 `POST_HI / POST_LO / EDGE_DIM` 三个 GOLD 索引。

顶盖与内壁改用外圈环。顶盖按用户要求"包裹黄铜机壳"，用 **2px 黄铜框 + 1px 暗缝**，
语法与侧板框带一致；内壁保持 1px（几乎看不到）。

**64px 的 `*_connected` 图不是 16px 图块网格**——这是一次踩坑：当成 4×4 网格处理，每格套一个
黄铜环，一整块连续面板被切成可见的 3×3 格子。实际布局是：

| 区域 | 位置 | 内容 |
|---|---|---|
| 连接面板 | `x16..63 / y0..47`（48×48） | 一整块连续面板 |
| 独立面 | `x0..15 / y48..63`（16×16） | 普通 16px 面 |
| 其余 | — | 透明 |

所以按**区域**分区：侧板的 48×48 用肋带行（亮带 `0..2,4,9,25,41,45`、暗缝 `3,8,24,40,44,47`）
加上外圈 2px 黄铜；顶盖/内壁只给 48×48 外圈一圈（2px+1px / 1px）。改 CT 图前先看这张表。

两个坑：**① 别整体换色板**——Create 铜色跨亮度 63、8 阶黄铜跨 95，同一个秩映射会把
Create 细腻的肋纹放大成噪点（std 30.5 vs 原版 18.7）。**② 暗缝别用 `brass_casing` 的
`#442B15`/`#53381A`**——低饱和暖褐，小尺寸下就是深色木板；用中性灰才不发木。

### 流体舱口（继承 Create 物品舱口模型）

`6e4f834` 之后两个舱口模型只有一行 `{"parent": "create:block/item_hatch/block_closed"}`，
所以默认渲染得和 Create 物品舱口一模一样。父模型只开放 3 个可覆盖键，全部由
`tools/create-fluid-hatch-textures.py` 填：

| 键 | 贴图 | 做法 |
|---|---|---|
| `0` | `block/{fluid,smart_fluid}_hatch/housing.png`（32×32） | Create `hatch.png` 的**中间灰**换成铜/黄铜，黑与银原样保留；黄铜版额外压一道亮金包边 |
| `1` | `block/{fluid,smart_fluid}_hatch/flap.png`（16×16） | 同上 |
| `particle` | 同 `housing` | — |

**只换色，不动像素布局。** `swap_material()` 只替换中间灰，黑色格栅、银色包边、亮边和暗缝原样保留。
框的层次来自 Create 自己画的亮边暗缝 + 色阶跨度，不靠重画。

两条色阶（每档都是 Create 真实存在的颜色）：

| 材质 | 处理 |
|---|---|
| 铜 | 色阶切片 `swap_material()` |
| 黄铜 | **明暗分布匹配** `match_distribution()` |

### 黄铜必须按"分布"而不是"色阶"来做

量一下就知道为什么之前一直"深色太多"：

| 素材 | p50（中位亮度） | 亮度<110 占比 | 平均 |
|---|---|---|---|
| `brass_block` | 174 | **11%** | 161 |
| `mechanical_arm` | 174 | **18%** | 162 |
| `brass_ingot` | 101 | 54% | 141 |
| **`hatch`（源）** | **79** | **87%** | **86** |

Create 的 item hatch 是**暗**灰色件，黄铜件是**亮**件。只要用"色阶切片"，暗的分布就原样保留，
所以不管挑哪段黄铜色，出来都是"深色太多"。

`match_distribution()` 把舱口本体像素按**自己的分位**映射到参考图的同一个分位：
参考集 = `mechanical_arm` + `brass_block` 的全部暖色像素（按像素计频，不只是取唯一色）。
映射是**单调**的，所以每条边、每根格栅、每道缝都还在原位——布局不变，只是明暗分布换成了
黄铜件该有的样子：暗像素从 87% 降到 16%。

不用 `brass_ingot` 做参考：它的分布是 54% 暗 + 极高对比，套上去会变成棋盘格。

> 曾被推翻的做法：给可见面描黄铜环、把凹槽压成石板灰、给门板加倒角。这些都在改 Create 的
> 像素布局，结果正面变乱、背面变闷。用户明确要求"尽量不要打乱纹理"，已全部撤回。
> 若要重做，先确认改动是**换色**而不是**改图**。

**要压在哪个窗口**（这点很容易搞错，虽然现在不重画了，坐标仍记在这里备用）：

| 窗口 | 是谁 | 可见性 |
|---|---|---|
| `x3..15 / y3..17` | 帧 north 面 | 贴着容器那侧，**游戏里看不到** |
| `x2..15 / y2..17` | 墙体 north + south | **玩家看到的那一面** |

只压前者等于白做。现在两个都压。墙体窗口右边界是 `x15`、下边界 `y17`——`x16`/`y18` 在
Create 原图里是透明像素。

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

色阶是**同一把梯子上的一段切片**，起点决定观感深浅。这一点来回调过三次：

| 起点 | 结果 |
|---|---|
| `#3D161E`（亮度 31，10 阶） | "看起来太深"——饱和的低段铜色比同亮度的中性灰更沉 |
| `#9A5038`（亮度 94，7 阶） | "又有点浅"——切片太短，本体丢失色阶、发平 |
| **`#724731`（亮度 79，9 阶）** | 现在的值，深浅中间 |

当前两段（每个颜色都是 Create 真实存在的色，没有插值）：

- 铜 `#724731` → `#E3826C`，9 阶
- 黄铜 `#724731` → `#FFEB8C`，12 阶

要微调就改这两个数组的起点，往亮走一档是 `#904931`，往暗走一档是 `#6C3528`（取自
`mechanical_arm.png`）。**别再往两头拉**：太暗会发闷，切片太短会发平。

## 生成器

| 脚本 | 职责 | 依赖 |
|---|---|---|
| `copper-material.py` | 上面 3 张派生贴图；色板与 `metrics`/`symmetry` 工具 | Python + Pillow + Create 源 |
| `create-collection-grate-textures.py` | 集液栅 4 张贴图，按模型的 UV 分带排布 | Python + Pillow |
| `create-fluid-hatch-textures.py` | 两个流体舱口各 2 张（housing / flap） | Python + Pillow + Create 源 |
| `recolor-rank.py` → `create-smart-tank-textures.py` | 智能储罐 8 张：铜身 + 黄铜框带（按行分区，暗缝用中性灰） | Python + Pillow + Create 源 |
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
