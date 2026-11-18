# 文案规范：提示与思索

本文件约束 `assets/ccd/lang/*.json` 里所有玩家可见的**物品提示**（`*.tooltip.*`）与**思索**（`ccd.ponder.*`）。
规范不是自创的，是从 Create 自己的语言文件里归纳出来的（`create.ponder.*` 与 `create:*:tooltip`）。

## 参考

Create 的原句，注意它怎么用 `_…_` 和怎么断句：

```text
create.ponder.chute.header             使用溜槽向下运输物品
create.ponder.chute.text_1             溜槽可在两个容器之间垂直传输物品
create.ponder.chute.text_2             使用扳手右击可以在溜槽上设置观察窗

block.create.item_hatch.tooltip.summary     _Quickly solves_ your _inventory_ clutter and makes it
                                            someone else's problem.
block.create.item_hatch.tooltip.condition1  When R-Clicked
block.create.item_hatch.tooltip.behaviour1  _Deposits_ your _held item_ into the _container_ it is
                                            placed on.
```

结论：**summary 是一句带态度的话**，condition 极短，behaviour 只讲一个结果。

## 规则

| 字段 | 中文 | English | 要点 |
|---|---|---|---|
| `ponder.*.header` | ≤ 14 字 | ≤ 6 词 | 动词开头，不加句号 |
| `ponder.*.text_N` | 12–24 字 | ≤ 70 字符 | 一行一个意思；按顺序讲"怎么摆 → 怎么给动力 → 怎么控制 → 有什么限制" |
| `tooltip.summary` | ≤ 22 字 | ≤ 80 字符 | 一句钩子，允许有态度；`_…_` 落在**动词 + 关键名词**上 |
| `tooltip.conditionN` | ≤ 9 字，无句号 | Title Case，≤ 5 词 | 只写触发条件（"右击时"、"接收红石信号时"） |
| `tooltip.behaviourN` | ≤ 28 字 | ≤ 90 字符 | 只讲一个后果，不堆分号 |
| `tooltip.controlN` | ≤ 12 字 | ≤ 8 词 | 只写手势 |
| `tooltip.actionN` | ≤ 18 字 | ≤ 70 字符 | 只写结果，一句话 |

补充：

- `_…_` 是金句强调，必须成对；**别拿它标数字或方块名**（那是说明书习惯），要标动作：
  `_装满_`、`_暂停_`、`_调换_`，而不是 `_4000 mB_` 之外的整串事实。
- 一条里出现第二个分号，就说明该拆成两组 condition/behaviour。
- 中文用全角标点；数字与单位之间留空格（`4000 mB`、`1×1`）。
- 不在文案里写"本模组"、"该方块"这类第三人称自称。
- 不承诺未实现的行为。改写只改语气与结构，**事实一个字都不能变**。

## 这次改写解决的四个问题

1. **说明书体**。原文 `summary` 例如"为流体管路提供更强的_泵送压力_和更远的_输送距离_，同时消耗更多应力。"
   —— 三个事实塞一句。已改成"把流体推得_更远、更有劲_，代价是更吃应力。"
2. **condition 过长**。`When a Container or Cauldron Directly Below` → `With a Container or Cauldron Below`。
3. **emphasis 打在事实上**。`_1×1, 2×2 or 3×3_` 这类强调改到动作上；尺寸留在正文里。
4. **7 个介质桶是空壳**。原文只有 `A fluid for _seething_.`，现在写明它给气流哪种加工配方。

## 没有改的部分

- `ccd.status.*`、`ccd.gui.goggles.*`、`ccd.package.*` 等**运行时消息**：它们是 HUD 与状态回报，
  一条对应一个代码分支，改语气收益低、破坏测试的风险高，本轮不动。
- 所有键名一个都没改，只改值。Java 侧引用不受影响。

## 新增文案时的自检

1. `summary` 读起来像不像一句人话？有没有态度？
2. 有没有把两件事塞进一个 `behaviour`？
3. `_…_` 是否成对、是否落在动作上？
4. 中英是否表达同一件事、同样的强调位置？
5. `python tools/check-lang.py` 是否通过（键齐全、强调成对、占位符一致）。
