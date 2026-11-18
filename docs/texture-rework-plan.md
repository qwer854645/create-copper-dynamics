# CCD 贴图返修记录

参考对象：Create 6.0.10-280 的 `assets/create/textures/block/*`。

预览：
- `outputs/texture-rework/before-after.png` —— `git HEAD` 现状 / 返修后 对照（含等轴测方块）
- `outputs/texture-rework/reuse-map.png` —— 现在引用了哪些 Create 贴图、哪些是派生的
- `outputs/texture-rework/before-after.json` —— 逐项前后指标

---

## 1. 一句话结论

现在的做法是 **能引用 Create 原生贴图就不自己画**：

- 12 个「平铜/黄铜/机件面」在模型里直接指向 `create:block/copper_underside`、`brass_block`、
  `industrial_iron_block`，CCD 里对应的 png **已删除**；
- 只有 6 张因为像素布局承载语义（轴孔、箭头、同心凹槽、32×32 面）必须保留，且全部由
  Create 的像素派生，**没有引入新调色板**；
- 智能储罐 4 张从 Create 的 `fluid_tank*` 出发，只做色阶换色。

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
| 增压泵 body、流体传讯器 body | `create:block/copper_underside` |
| 流体舱口 plate | `create:block/copper_underside` |
| 智能龙头 body、智能舱口 plate | `create:block/brass_block` |
| 智能集液栅 body（32×32） | `create:block/brass_block` 2×2 铺贴 |
| 移液臂金属件 | `create:block/industrial_iron_block` |

涉及的模型文件共 23 个、45 处引用（`models/block/*`、`models/block/**/*`、`models/item/*`）。

---

## 5. 保留的 6 张派生贴图

| 文件 | 派生方式 | std / flat / tones |
|---|---|---|
| `block/booster_pump/shaft_face` | `copper_underside` + 原生 gearbox 8×8 轴孔 | 42.1 / 20.7% / 19 |
| `block/fluid_transmitter/side` | `copper_underside` 2×2 + CCD 箭头 | 19.6 / 24.4% / 9 |
| `block/fluid_transmitter/top` | 同上 | 19.6 / 24.2% / 9 |
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

# 增压泵 / 传讯器原生细节
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
