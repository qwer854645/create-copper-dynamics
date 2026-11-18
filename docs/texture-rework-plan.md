# CCD 贴图返修记录

参考对象：Create 6.0.10-280 的 `assets/create/textures/block/*`。

预览：
- `outputs/texture-rework/before-after.png` —— `git HEAD` 现状 / 返修后 对照（含等轴测方块）
- `outputs/texture-rework/reuse-map.png` —— 现在引用了哪些 Create 贴图、哪些是派生的
- `outputs/texture-rework/before-after.json` —— 逐项前后指标

---

## 1. 一句话结论

现在的做法是 **能引用 Create 原生贴图就不自己画**：

- 11 个「平铜/黄铜/机件面」在模型里直接指向 `create:block/copper_underside`、`brass_block`、
  `industrial_iron_block`，CCD 里对应的 png **已删除**；
- 只有 4 张因为像素布局承载语义（轴孔、同心凹槽、32×32 面）必须保留，且全部由
  Create 的像素派生或按几何生成，**没有引入新调色板**；
- 智能储罐 4 张从 Create 的 `fluid_tank*` 出发，只做色阶换色；
- **流体传讯器 3 张已回退**到返修前的设计，理由见 §9。

---

## 2. 为什么要动：三处结构性错误

原贴图的丑不是画功问题，是三个可定位的错误。

### 2.1 对称化把机壳做成万花筒

`tools/symmetrize-fluid-housings.py` 曾用 6×6 的 `PATCH` 双向反射 + 90° 旋转生成 16×16 面，
并在结尾断言：

```python
assert image.tobytes()==image.transpose(FLIP_LEFT_RIGHT).tobytes()
assert image.tobytes()==image.transpose(ROTATE_90).tobytes()
```

16×16 有 256 个像素，折两次只剩 **10 个自由像素**。实测 `booster_pump/body.png` 与
`fluid_transmitter/body.png` 的 `LR/UD/R180/R90` 差异**全部 0.0%** —— 四向完全对称，
读起来是模糊的径向渐变而不是金属板；断言还顺带禁止了左上打光。

（`booster_pump/body` 已改为引用 Create；`fluid_transmitter/body` 连同它的 side/top 一起回退
到原设计，见 §9 —— 那三张靠四角铆钉和黄色指示面板撑住了可读性，不是纯粹的糊面。）

### 2.2 换色用绝对亮度阈值，把 8 阶压成 3~4 阶

`recolor-smart-fluid-tank.cjs`、`repaint-smart-tank.py`、`recolor-fluid-arm.cjs` 都用固定亮度梯子分档：

```js
const thresholds=[35,55,75,90,108,128,150,180];
let index=thresholds.findIndex(t=>luminance<t); if(index<0) index=8;
```

Create `fluid_tank.png` 的 8 个铜色亮度是 86.4 / 94.0 / 104.5 / 113.1 / 123.3 / 131.7 / 140.0 / 149.0，
套进去得到 `[3,4,4,5,5,6,6,6]` —— **只剩 4 个不同输出**，而且目标色板比源色亮 70~100，
暗部全丢，于是储罐像刷了层金漆。`recolor-fluid-arm.cjs` 的梯子更糟，8 阶只剩 3 阶。

### 2.3 手绘底板 = 2px 平涂边框 + 一块低振幅色块

`create-faucet.cjs` 的外圈 2px 全写死 `copper[4]`，而 `copper[]` 相邻两阶只差 7~15，
16px 尺度上看不出来。最平的一张 `copper_water_supply/body.png` **79.7% 的像素是同一个颜色**，
只有 3 种颜色。

---

## 3. 量化对照

指标：`std` 面内亮度标准差；`flat` 出现最多的单色占比；`tones` 不同颜色数。

### 3.1 参照：Create 自己的金属面

| 贴图 | std | flat | tones |
|---|---:|---:|---:|
| `copper_underside` | 19.1 | 25.0% | 8 |
| `copper_casing` | 23.5 | 17.2% | 15 |
| `fluid_tank` | 18.7 | 20.7% | 8 |
| `industrial_iron_block` | 19.7 | 19.9% | 11 |
| `brass_block` | 42.6 | 20.3% | 13 |
| `brass_casing` | 62.6 | 14.1% | 17 |

### 3.2 返修前后

| 面 | std | flat | tones |
|---|---|---|---|
| 龙头 body | 11.8 → **19.1** | 57.4% → **25.0%** | 8 → 8 |
| 三通阀 body | 11.8 → **19.1** | 57.4% → **25.0%** | 8 → 8 |
| 增压泵 body | 8.6 → **19.1** | 62.5% → **25.0%** | 5 → **8** |
| 流体舱口 plate | 15.7 → **19.1** | 56.6% → **25.0%** | 6 → **8** |
| 水槽 body | 3.9 → **19.1** | 79.7% → **25.0%** | 3 → **8** |
| 集液栅 body | 11.8 → **19.1** | 62.1% → **25.0%** | 9 → 8 |
| 智能龙头 body（黄铜） | 20.8 → **42.6** | 57.4% → **20.3%** | 8 → **13** |
| 移液臂金属件 | 6.3 → **19.7** | 71.9% → **19.9%** | 3 → **11** |
| 智能储罐 | 16.0 → **30.5** | 34.8% → **20.7%** | 4 → **8** |
| 智能集液栅 body（32×32） | 12.5 → **42.6** | 93.8% → **20.3%** | 6 → **13** |

---

## 4. 现在引用了哪些 Create 贴图

模型里只改了 `textures` 的值，**几何、UV、`render_type` 一律没动**。

| CCD 面 | 引用的 Create 贴图 |
|---|---|
| 龙头、三通阀、水槽、集液栅、水盆外侧、流体访问口 | `create:block/copper_underside` |
| 增压泵 body | `create:block/copper_underside` |
| 流体舱口 plate | `create:block/copper_underside` |
| 智能龙头 body、智能舱口 plate | `create:block/brass_block` |
| 智能集液栅 body（32×32） | `create:block/brass_block` 2×2 铺贴 |
| 移液臂金属件 | `create:block/industrial_iron_block` |

> `fluid_transmitter/body` 已回退成 `ccd:block/fluid_transmitter/body`，不再引用 Create，见 §9。

涉及的模型文件共 22 个、44 处引用（`models/block/*`、`models/block/**/*`、`models/item/*`）。

---

## 5. 保留的 4 张派生贴图

| 文件 | 派生方式 | std / flat / tones |
|---|---|---|
| `block/booster_pump/shaft_face` | `copper_underside` + 原生 gearbox 8×8 轴孔 | 42.1 / 20.7% / 19 |
| `block/smart_collection_grate/body` | `brass_block` 2×2 | 42.6 / 20.3% / 13 |
| `block/copper_water_supply/cap` | 同心凹槽按环几何生成，落在 Create 8 阶铜色上 | 18.3 / 24.2% / 7 |
| `block/fluid_arm/core` | 深色轴孔按环几何生成，落在深色阶上 | 9.6 / 34.8% / 5 |

智能储罐 4 张（`fluid_tank`、`_top`、`_inner`、`_connected`）继续存在，但改为**按秩 1:1 映射**：

| 源（Create 铜色） | `#904931` | `#9A5038` | `#A75A40` | `#B26247` | `#C26B4C` | `#C87456` | `#D67B5B` | `#E3826C` |
|---|---|---|---|---|---|---|---|---|
| 目标（黄铜） | `#724731` | `#7F4E2B` | `#8C5834` | `#9E6947` | `#AF794F` | `#BF8E55` | `#CEA05A` | `#D7AA5E` |

非暖色（灰色机械件、玻璃）一律不动，透明度逐字节保持不变。

---

## 6. 代码改动

| 文件 | 改动 |
|---|---|
| `tools/copper-material.py` | 重写为「派生贴图生成器」：`derive()` 产出上面 6 张，全部基于 Create 像素；附带 `metrics` / `symmetry` / `--check` / `--verify` |
| `tools/recolor-rank.py` | 新增：按秩 1:1 映射，自动在 Create 源里找结构匹配的同名贴图并校验 alpha |
| `tools/repaint-smart-tank.py` | 旧的重复循环删掉，改成 `recolor-rank.py` 的入口包装；去掉对不存在的 `pxl` 技能的依赖 |
| `tools/symmetrize-fluid-housings.py` | 删掉 1/8 反射和两条对称断言，改为调用 `copper-material.derive()`；现在断言**不允许**四向对称 |
| `tools/recolor-smart-fluid-tank.cjs` | 阈值分档 → 秩映射，黄铜色板换成 8 阶 |
| `tools/recolor-fluid-arm.cjs` | 阈值分档 → 秩映射（贴图未重跑，见 §8） |
| `tools/README.md` | 新增：贴图归属表与生成器用法 |
| 12 个 `create-*.cjs` / `create-fluid-access-art.py` | 顶部加「贴图归属」声明，避免重跑时回退旧材质 |

---

## 7. 复现 / 校验

```powershell
$jar = "<create-1.21.1-6.0.10-280.jar>"

# 派生贴图
python tools/copper-material.py src/main/resources/assets $jar
python tools/copper-material.py src/main/resources/assets $jar --check    # 只看指标
python tools/copper-material.py src/main/resources/assets $jar --verify   # 比对，有偏差返回 1

# 智能储罐换色
python tools/recolor-rank.py $jar

# 增压泵原生轴孔
python tools/symmetrize-fluid-housings.py
```

验收门槛（16×16 不透明金属面）：`std ≥ 15`、`flat ≤ 30%`、`tones ≥ 6`、
`LR/UD/R180/R90` 至少一项 > 5%（禁止全向对称）。

---

## 8. 已知取舍

1. **平铜面全部长一样**。所有「一块铜板」的面现在都是 `copper_underside`，靠几何（12×12 内嵌、
   外框、折边）区分而不是靠贴图。这正是 Create 自己的做法（所有 andesite casing 同款），
   但如果你希望每个方块有各自的铜面，需要再引入 2~3 张变体。
2. **智能集液栅 32×32 面有十字缝**。`brass_block` 是 16×16，2×2 铺贴会看到四块板的接缝 ——
   这是它自带边框造成的，视觉上读作「四块黄铜板拼接」。
3. **移液臂贴图没有重跑**。`recolor-fluid-arm.cjs` 的映射已改成秩映射，但 `mechanical_arm.png`
   保持原样：它 `std 54.4`、结构匹配 0.98，本身已达标，重跑需要 `sharp` 且会牵动针管模型，
   收益不划算。改动生效需要下次跑那个脚本。
4. **12 个 `create-*.cjs` 里还留着旧的 `copper[]` 数组**，它们生成的贴图已无人引用。没有删，
   是因为删掉会改变模型生成流程，而本机跑不了 `sharp` 无法验证；改成在文件头声明归属。
5. **未构建、未启动游戏**。全部为离线像素与资源校验：166 个 JSON 全部可解析、
   0 处悬空贴图引用、0 个 blockstate 指向不存在的模型。

---

## 9. 流体传讯器：已回退

第一版把这个方块的 32×32 面也换成了 `copper_underside` 2×2 铺贴，用户反馈「有点丑」，回退。
原因是那次替换除了观感，还丢了一个功能特征：

| | 返修前（现恢复） | 第一版（已废弃） |
|---|---|---|
| 32×32 面的中心 | **黄色指示面板** + 深色描边、四角铆钉 | 无，被铜板覆盖 |
| 箭头标记 | 位于设计好的位置 | 压在 2×2 铺贴接缝上，读成一坨十字 |
| `side` / `top` 的 std | 39.1（16 色） | 19.6（9 色） |

已回退的文件：`block/fluid_transmitter/{body,side,top}.png` 与
`models/block/fluid_transmitter.json` 的 `body` 键，全部恢复到 `f6e8ff2` 的版本。
`tools/copper-material.py` 与 `tools/symmetrize-fluid-housings.py` 里对传讯器的派生逻辑已删除，
不会再被重跑覆盖。

**结论性教训**：带指示灯、刻度、箭头这类「信息像素」的面，不能套通用金属板；
`copper_underside` 只适合纯结构面。

---

## 10. 集液栅改成一格高之后的贴图

`d48566a` 把两个集液栅从 8px 深的水盆改成 16px（一格高），`tools/resize-collection-grates.py`
在改几何的同时把侧壁 UV 一起放大了（`uv[3] = uv[1] + 6`），所以侧壁现在是 **一个模型像素对一个纹素**。
继续套 `copper_underside` 就出问题了：

| 现象 | 原因 |
|---|---|
| 两层墙壁读成一整块平板 | 上层采样 v3..9、下层采样 v5..11，而 `copper_underside` 没有垂直衰减 |
| 悬挑底面是**亮**的 | 悬挑底面采样 v1..3，而 `copper_underside` 的高光正好在第 1 行 |
| 凸沿只是一条平色带 | 凸沿上表面只采样最外 2 行/2 列，那圈在原图里没有设计 |
| 智能版凸沿发绿、有接缝 | 用的是 `brass_block` 2×2 铺贴，采样的 v2..3 正好是拼缝 |

改为 `tools/create-collection-grate-textures.py` 按模型的采样条带专门排布：

| 贴图 | 条带 | 设计 |
|---|---|---|
| `copper_collection_grate/wall.png` | v0..2 悬挑底面 / v3 上层顶边 / v5 层间接缝凹槽 / v7 地板侧沿 / v9 上层底 / v11 下层底 | 垂直衰减 + 层间暗缝 |
| `copper_collection_grate/rim.png` | 最外 2 行/2 列组成的环 | 外圈亮倒角 + 内圈暗影线 |
| `smart_collection_grate/body.png` | v2..3 | 黄铜凸沿侧面亮带 |
| `smart_collection_grate/rim.png` | 环 | 同上，黄铜色 |

模型的唯一改动是 `copper_collection_grate.json` 的 `copper` 键：
`create:block/copper_underside` → `ccd:block/copper_collection_grate/wall`。几何、UV 未动。

关键约束：`v3..9` 与 `v5..11` 在 5..9 重叠，所以那几行被两层共用，不能分开调。
第 5 行同时是「下层墙顶（在悬挑下面，应该是暗的）」和「上层墙顶下方 2px」，
两者都偏暗，于是定成一条刻意的凹槽。

预览：`outputs/texture-rework/collection-grate.png`（`git HEAD` 前后对照，4 个视角）。
渲染用的是把 `tools/preview-fluid-packager.cjs` 的 `mesh()`/`render()` 一比一移植过来的
Python 版（同样的面角顺序、UV 基准四边形、v=0 在图像顶部、光照与 z-buffer 规则），
因为本机没有 `sharp`，跑不了原脚本。

---

## 11. 流体舱口：继承物品舱口模型后的贴图

`6e4f834` 把两个舱口模型换成一行 parent 引用：

```json
{"parent": "create:block/item_hatch/block_closed"}
```

没有任何贴图覆盖，所以两个方块都渲染得和 Create 物品舱口完全一样。父模型只开放 3 个键：
`0`（房体，32×32 图集 `hatch.png`）、`1`（闸门，16×16 `hatch_closed.png`）、`particle`。

### 两个渲染探针测出来的事实

**① 正面是 `#0`，不是 `#1`。** 把 `#0` 涂成品红、`#1` 涂成青色后渲染：

| 视角 | 品红（#0 房体） | 青色（#1 闸门） |
|---|---|---|
| 正面 | 整个面板 | 无 |
| 侧 3/4 | 整个可见面 | 无 |
| 俯视 | 其余部分 | **只有斜门的顶面** |

所以「管口」必须画在 `#0`。第一版我把凹槽画在闸门上，从正面一个像素都看不到。

**② 可见面板在图集里的窗口是 x3..15 / y3..17。** 帧的 north 面 uv 为 `[1.5,1.5]-[7.5,8.5]`，
图集 32px → uv×2。这块窗口内部 x5..12 / y5..14 是 Create 画的**物品格栅百叶**。

### 做法

| 文件 | 做法 |
|---|---|
| `block/{fluid,smart_fluid}_hatch/housing.png`（32×32） | Create `hatch.png` 的**中间灰**换成铜/黄铜，黑与银原样保留 |
| `block/{fluid,smart_fluid}_hatch/flap.png`（16×16） | `hatch_closed.png` 同上 |

### 三次返工的教训

**第一版：整体亮度重映射。** 把三档材质全压到一条铜色阶上，用户反馈"像是直接加了滤镜，有点奇怪"。
Create 的舱口美术其实分三档：

| 亮度 | 材质 | 占比 | 处理 |
|---|---|---|---|
| ≤ 56 | 黑色凹槽 / 格栅缝 | 11% | 原样保留 |
| 56~145 | 工业铁本体 | 78% | 换铜 / 黄铜 |
| ≥ 145 | 裸银高光 / 包边 | 8% | 原样保留 |

只换中间灰之后，格栅结构和银色包边都回来了（色阶数从 10~11 回到 17~18）。

**第二版：在正面面板压一个凹陷管口。** 也不对。那个窗口（x5..12 / y5..14）正是 Create 画的
物品格栅百叶，压一个洞会把结构盖掉、看起来像铜板上挖了个方孔。已放弃。
如果后续要加强"流体"语义，建议在角落加一颗小水滴标记，不要动门面本身。

**色阶是同一把梯子上的一段切片，起点决定深浅。** 这一点来回调了三次，记下来避免下次再绕：

| 起点 | `std` | 反馈 |
|---|---|---|
| `#3D161E`（亮度 31，10 阶） | 53.7 | "颜色看起来太深" |
| `#9A5038`（亮度 94，7 阶） | 59.0 | "又有点浅" |
| **`#724731`（亮度 79，9 阶）** | **55.0** | 采用 |

第一次以为是自己压暗了，实测换色的 441 个像素平均亮度只差 +3.3，说明**问题不在数值，而在
饱和的铜色比同亮度中性灰看起来更沉**。但把起点一路抬到 94 又走向反面：切片只有 7 阶、
跨度 55，本体丢掉色阶、发平，于是"浅"。

所以正确做法不是"提亮"，而是**在亮度与切片长度之间取中间**：起点 79、跨度 70、9 阶。

| 材质 | 色阶 |
|---|---|
| 铜 | `#724731` → `#E3826C`，9 阶 |
| 黄铜 | `#724731` → `#FFEB8C`，12 阶 |

每个颜色都是 Create 真实存在的色，没有插值。要微调就换起点：往亮一档 `#904931`，
往暗一档 `#6C3528`（取自 `mechanical_arm.png`）。

### 过滤槽位

用户已把过滤槽位改成 Create 原生 `HatchFilterSlot`。从它的字节码常量看是
`Vec3(8.0, 5.15, 9.5)`、`-45°`、`0.965` —— 图标跟着斜门一起倾斜，贴在门上，**不在正面面板上**，
所以不会和贴图打架。

预览：`outputs/texture-rework/fluid-hatch.png`（Create 原件 / 铜版 / 黄铜版 × 3 视角）。

模型文件只加 `textures` 覆盖，几何与 UV 未动：

```json
{
  "parent": "create:block/item_hatch/block_closed",
  "textures": {
    "0": "ccd:block/fluid_hatch/housing",
    "1": "ccd:block/fluid_hatch/flap",
    "particle": "ccd:block/fluid_hatch/housing"
  }
}
```

### 过滤槽位

用户已把过滤槽位改成 Create 原生 `HatchFilterSlot`。从它的字节码常量看是
`Vec3(8.0, 5.15, 9.5)`、`-45°`、`0.965` —— 图标跟着斜门一起倾斜、贴在门上，**不在正面面板上**，
所以不会和贴图打架。

预览：`outputs/texture-rework/fluid-hatch.png`（Create 原件 / 铜版 / 黄铜版 × 3 视角）。
