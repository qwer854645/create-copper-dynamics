"""Author bilingual Create ItemDescription text, keeping translation IDs stable."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
LANG=ROOT/'src/main/resources/assets/ccd/lang'
data={locale:json.loads((LANG/(locale+'.json')).read_text(encoding='utf-8')) for locale in ['zh_cn','en_us']}
def put(key,zh,en):
    data['zh_cn'][key]=zh; data['en_us'][key]=en
def describe(key,summary,conditions=(),controls=()):
    for locale in data:
        for old in list(data[locale]):
            if old.startswith(key+'.tooltip.'): del data[locale][old]
    put(key+'.tooltip.summary',*summary)
    for i,(condition,behaviour) in enumerate(conditions,1):
        put(f'{key}.tooltip.condition{i}',*condition); put(f'{key}.tooltip.behaviour{i}',*behaviour)
    for i,(control,action) in enumerate(controls,1):
        put(f'{key}.tooltip.control{i}',*control); put(f'{key}.tooltip.action{i}',*action)

redstone=('接收到红石信号时','When Powered by Redstone')
describe('block.ccd.booster_pump',('为流体管路提供更强的_泵送压力_和更远的_输送距离_，同时消耗更多应力。','Provides greater _pumping pressure_ and _range_ at a higher stress cost.'),[
    (('接入旋转动力时','When Supplied with Rotation'),('两端连接管路，四侧可连接传动轴。输送方向随_旋转方向_改变。','Connect pipes at either end and shafts to the four sides. _Rotation direction_ determines flow.'))],
    [(('使用扳手右击','R-Click with a Wrench'),('_反转_泵送朝向。','_Reverses_ the pump facing.'))])
describe('block.ccd.fluid_transmitter',('在两端容器之间_直接搬运流体_，无需旋转动力。','_Transfers fluid_ between containers on opposite sides without rotational power.'),[
    (('连接容器时','When Attached to Containers'),('沿机身_箭头方向_出液。多个传输器可直线串接，共用一次输送量。','Transfers towards the housing _arrows_. Straight chains share one transfer allowance.')),
    (redstone,('_暂停_输送。侧面过滤槽决定可通过的流体。','_Pauses_ transfer. Side filters select which fluids may pass.'))],
    [(('使用扳手右击','R-Click with a Wrench'),('_调换_取液端与出液端。','_Swaps_ the input and output.'))])
describe('block.ccd.fluid_access_port',('把容器的_流体连接面_向外扩展，让更多设备访问同一份存液。','Extends a container\'s _fluid connection faces_, giving more devices access to its contents.'),[
    (('贴在流体容器上时','When Attached to a Fluid Container'),('背面连接容器，其余五面可接管道或设备。端口_不存液_，也不主动输送。','The rear attaches to the container; the other five faces accept pipes or devices. Stores _no fluid_ and does not pump.')),
    (('连接另一个扩展端口时','When Attached to Another Access Port'),('无法转接。请直接连接容器或桥接器的可用侧面。','Cannot extend another port. Attach to a container or an accessible side of a Fluid Bridge.'))])
describe('block.ccd.fluid_bridge',('把两端容器接到同一个_流体入口_，从侧面统一取放流体。','Combines two containers behind a single _fluid access point_ on its sides.'),[
    (('连接容器时','When Attached to Containers'),('两端分别连接容器，四侧供设备访问。不会自行_平衡液位_。','Attach containers at both ends and devices at the four sides. Does not _balance fluid levels_.')),
    (('设置端面过滤时','When an End Filter Is Set'),('该端只显示和传输_匹配的流体_。空过滤允许所有流体。','Only _matching fluid_ is shown and transferred through that end. An empty filter allows all fluids.'))],
    [(('对端面中心设置过滤','Set a Filter at an End Face'),('分别选择两端允许通过的_流体_。','Selects the allowed _fluid_ for each end independently.'))])
describe('block.ccd.redstone_fluid_valve',('用_红石信号_开关流体管道，无需旋转动力。','Opens or closes a fluid pipe using _redstone_, without rotational power.'),[
    (redstone,('_开启_通路；信号消失时关闭。管路仍需泵提供输送压力。','_Opens_ the passage; closes without a signal. A pump is still required to move fluid.'))])
describe('block.ccd.three_way_valve',('在两条支路之间_切换_流体通路，可用于切换来源或目的地。','_Switches_ between two branches to select a fluid source or destination.'),[
    (('没有红石信号时','Without Redstone Power'),('银色_公共口_接通对面的_直通口_。','Connects the silver _common port_ to the opposite _straight port_.')),
    (redstone,('公共口改接_侧口_。绿色标记当前支路，实际流向由泵决定。','Connects the common port to the _side port_. Green marks the selected branch; pumps determine flow direction.'))],
    [(('使用扳手右击','R-Click with a Wrench'),('调整支路朝向或旋转阀体。','Adjusts the branch orientation or rotates the valve.'))])
for smart in [False,True]:
    key='block.ccd.'+('smart_faucet' if smart else 'faucet')
    conditions=[(('正下方有容器或炼药锅时','With a Container or Cauldron Directly Below'),('从_背后容器_取液并向下灌入。目标无法接收时停止。','Draws from the _container behind_ and pours downwards. Stops when the target cannot accept fluid.')),
                (redstone,('_暂停_出液，保留手动开关状态。','_Pauses_ flow without changing the manual setting.'))]
    if smart: conditions.insert(1,(('传送带或置物台位于下方两格时','With a Belt or Depot Two Blocks Below'),('中间留空，使用_分液池注液配方_处理物品。','Leave the intervening block clear. Processes items using _Spout filling recipes_.')))
    controls=[(('右击机身','R-Click the Housing'),('_开启_或_关闭_龙头。','_Opens_ or _closes_ the faucet.'))]
    if smart: controls.append((('对正面过滤槽设置过滤','Set the Front Filter'),('只允许_匹配流体_出液或加工。','Allows only _matching fluid_ for pouring and filling.')))
    describe(key,('从容器向下_倾倒流体_，无需旋转动力。'+('可过滤流体并为物品注液。' if smart else '龙头自身不储液。'),
        '_Pours fluid_ downwards from a container without rotational power. '+('Filters fluids and fills items.' if smart else 'Stores no fluid.')),conditions,controls)
describe('block.ccd.fluid_packager',('把流体装进_铜包裹_，也可将收到的铜包裹卸入容器。','Packs fluid into _Copper Packages_ and unloads arriving packages into a container.'),[
    (('背面连接容器时','With a Container Behind'),('每包最多_4000 mB_。自动打包尽量装满，物流订单按_请求量_分包。','Packs up to _4000 mB_ at a time. Redstone packs available fluid; logistics splits the _requested amount_ into packages.')),
    (('收到铜包裹时','When a Copper Package Arrives'),('容器需容纳整包流体；空间不足或不接收该流体时等待。','Waits until the container can accept the entire fluid payload.'))],
    [(('手持装液容器右击','R-Click with a Filled Container'),('采样设置_流体过滤_，不消耗样品；只约束红石打包。','Samples a _fluid filter_ without consuming the sample. Applies to redstone packaging.')),
     (('空手潜行右击','Sneak + R-Click with an Empty Hand'),('_清除_流体过滤。','_Clears_ the fluid filter.'))])
describe('item.ccd.fluid_package',('装载_一种流体_的密封包裹，可由蛙港和锁链输送机运输。','A sealed package carrying _one fluid_, transported by Frogports and Chain Conveyors.'),[
    (('装有流体时','When Filled'),('送入_流体打包机_卸液，不能手动拆出流体。空款只用于查看外观。','Unload with a _Fluid Packager_; fluid cannot be unpacked by hand. Empty variants are display samples.'))],
    [(('背包中拿起剪贴板，右击包裹','Carry a Clipboard in the Inventory and R-Click the Package'),('_复制_地址到剪贴板当前页。','_Copies_ its address to the current clipboard page.')),
     (('背包中拿起剪贴板，左击包裹','Carry a Clipboard in the Inventory and L-Click the Package'),('_粘贴_唯一未勾选的 #地址；单独的 # 清除地址。','_Pastes_ the only unchecked #address; a lone # clears the address.'))])
describe('item.ccd.composite_package',('将_物品_和_多种流体_装在一起，由理包机整理同一订单后生成。','Carries _items_ and _multiple fluids_ together, assembled by a Repackager from one complete order.'),[
    (('用于运输时','During Transport'),('沿用包裹地址与订单。空款只用于查看外观。','Uses package addresses and orders. Empty variants are display samples.'))],
    [(('潜行右击','Sneak + R-Click'),('_打开_包裹：物品归还背包，流体分装为铜包裹。空间不足时掉落。','_Opens_ the package: items return to the inventory and fluids become Copper Packages. Overflow is dropped.'))])
describe('item.ccd.fluid_stock',('供物流仪表选择_流体_的标记。数量以_1 mB_为单位。','A marker for selecting _fluids_ in logistics. Each count represents _1 mB_.'),[
    (('用于订单时','When Used in Orders'),('只代表请求内容，实际流体由_铜包裹_运送。','Represents a request; actual fluid travels in _Copper Packages_.'))])
describe('block.ccd.fluid_gauge',('查看物流网络的_流体存量_，并按目标数量请求补货。','Monitors _fluid stock_ in a logistics network and requests replenishment to a target amount.'),[
    (('装在收货端打包机上时','When Attached to a Receiving Packager'),('维持背后容器的_目标存量_，已发出的补货计入在途数量。','Maintains _target stock_ in the container behind. Outstanding deliveries count towards the target.'))],
    [(('放置前右击存量转信器','R-Click a Stock Link before Placing'),('_调频_到该物流网络。','_Tunes_ to its logistics network.')),
     (('手持流体样品短按右键','R-Click Briefly with a Fluid Sample'),('选择要监控或请求的_流体_。','Selects the _fluid_ to monitor or request.')),
     (('按住右键／打开设置界面','Hold R-Click / Open the Settings'),('调整目标数量；输入框支持_精确 mB_，0 表示只监控。','Adjusts the target; the input accepts _exact mB_. Set 0 to monitor only.'))])
describe('block.ccd.fluid_arm',('用旋转动力在选定的容器之间_搬运流体_，每次最多_1000 mB_。','Uses rotation to _move fluid_ between selected containers, up to _1000 mB_ per trip.'),[
    (('选择交互点时','When Selecting Targets'),('默认选择_流体舱口正面_。服务端可允许直接选择容器。','Select the _front of Fluid Hatches_ by default. Server settings can allow direct container access.')),
    (redstone,('_暂停取液_，已携带的流体继续送出；无处注液时保留。','_Pauses pickup_. Carried fluid is still delivered and retained if outputs are blocked.'))],
    [(('放置前右击交互面','R-Click a Target before Placing'),('依次设为_取液_、_注液_或取消；潜行右击移除。','Cycles between _input_, _output_ and deselected. Sneak-right-click removes a target.')),
     (('右击已放置的移液臂','R-Click an Existing Arm with the Arm Item'),('应用所选目标；潜行右击清空目标。','Applies selected targets; sneak-right-click clears them.'))])
for smart in [False,True]:
    describe('block.ccd.'+('smart_fluid_hatch' if smart else 'fluid_hatch'),
        ('为背后容器提供_手持取放_和_移液臂_入口。'+('可过滤进出的流体。' if smart else '不提供管道转接。'),
         'Provides _handheld_ and _Pipetting Arm_ access to the container behind. '+('Filters incoming and outgoing fluid.' if smart else 'Does not relay pipe connections.')),
        [(('安装在容器侧面时','When Attached to a Container'),('从外露_正面_操作，仍遵守所连接一面的限制。','Interact with the exposed _front_. The attached container face retains its restrictions.'))]+(
            [(('未设置过滤时','Without a Filter'),('_停止取放_。设置后仅允许匹配流体通过。','_Blocks transfer_. Set a filter to allow matching fluids.'))] if smart else []),
        [(('手持桶、瓶或流体容器右击','R-Click with a Bucket, Bottle or Fluid Container'),('优先_倒入_；无法倒入时尝试_取出_。','Attempts to _empty_ first, then _fill_ if emptying is not possible.'))])
for horizontal in [False,True]:
    describe('block.ccd.'+('horizontal_smart_fluid_tank' if horizontal else 'smart_fluid_tank'),
        ('多种流体_分别保存_，共用一组储罐的容量。'+('沿水平方向连接。' if horizontal else '沿竖直方向连接。'),
         'Stores _multiple fluids separately_ within shared capacity. '+('Connects horizontally.' if horizontal else 'Connects vertically.')),
        [(('搭建完整结构时','When Building a Complete Structure'),('截面可为_1×1、2×2 或 3×3_正方形，延长后合并容量。','A _1×1, 2×2 or 3×3_ square cross-section combines capacity along its length.')),
         (('使用管道抽液时','When Extracting through Pipes'),('未过滤时先抽取护目镜列表中_靠前的流体_；使用过滤选择所需流体。','Without a filter, extracts the _first listed fluid_. Use filtering to select another.'))],
        [(('使用扳手右击','R-Click with a Wrench'),('切换_观察窗_。拆除后超出剩余容量的流体会损失。','Toggles _windows_. Breaking the structure loses fluid beyond its remaining capacity.'))])
for supply in [False,True]:
    describe('block.ccd.'+('copper_water_supply' if supply else 'copper_water_basin'),
        ('提供_无限水源_。'+('自动向六面相邻容器补水。' if supply else '可手动装水或由管道抽取。'),
         'Provides _infinite water_. '+('Automatically fills containers on all six adjacent faces.' if supply else 'Fill containers by hand or extract through pipes.')),
        [(('输入流体时','When Receiving Fluid'),('倒入的液体会被_销毁_，内部始终保持清水。','Incoming fluid is _discarded_; the water supply remains unchanged.'))]+(
          [(redstone,('只_暂停自动补水_，手动与管道取水仍可用。','_Pauses automatic supply_ only; manual and piped extraction remain available.'))] if supply else []),
        [(('手持容器右击','R-Click with a Container'),('为可接收水的容器_装水_，不会替换其他流体。','_Fills_ containers that accept water without replacing another fluid.')),
         (('手持装液容器潜行右击','Sneak + R-Click with a Filled Container'),('_倒掉_其中的流体。','_Discards_ its fluid contents.'))])
for smart in [False,True]:
    describe('block.ccd.'+('smart_collection_grate' if smart else 'copper_collection_grate'),
        ('收集上方连通液体、雨水和细雪，向_正下方_排液。'+('可过滤并用红石暂停。' if smart else ''),
         'Collects connected fluid above, rain and powder snow, then drains _downwards_. '+('Adds filtering and redstone control.' if smart else '')),
        [(('上方接触流体时','When Fluid Touches the Top'),('逐步收取_连通液池_；内部存满时停止。可接收滴水石滴液。','Gradually drains the _connected pool_, stopping when full. Also catches dripstone fluids.'))]+(
         [(redstone,('暂停自动集液和下排；外部管道与手持取放仍可用。','Pauses automatic collection and downward output; pipes and handheld access remain available.')),
          (('设置过滤时','When Filtering'),('只接收匹配流体，已有存液仍可排出。空过滤接受所有流体。','Accepts matching fluid; existing contents remain drainable. Empty filters accept all fluids.'))] if smart else []))
describe('block.ccd.fluid_atomizer',('将流体喷入_气流_，进行批量加工或施加药水效果。储液_4000 mB_。','Sprays fluid into an _air current_ for bulk processing or potion effects. Holds _4000 mB_.'),[
    (('接入动力和有效媒介时','With Rotation and a Valid Medium'),('水用于水洗，熔岩用于熔炼，流沙用于喷砂。其他加工取决于已安装附属。','Water washes, lava blasts and quicksand sands. Further processing depends on installed addons.')),
    (('装有药水流体时','When Filled with Potion Fluid'),('周期性对气流中的生物施加_药效_。','Periodically applies _potion effects_ to creatures in the current.')),
    (redstone,('_停止雾化_，保留普通吹吸气流。','_Stops spraying_ while retaining ordinary airflow.'))],
    [(('手持流体容器右击任意面','R-Click Any Face with a Fluid Container'),('优先倒入，不能倒入时尝试取出。管道从_四个侧面_连接。','Empties first, then attempts to fill. Pipes connect to the _four side faces_.'))])
describe('item.ccd.copper_bucket',('可携带_4000 mB_的通用铜桶，一次保存_一种流体_。','A general-purpose bucket holding _4000 mB_ of _one fluid_.'),[
    (('从世界取放时','When Used in the World'),('每次取放_1000 mB_，只收取源液体；创造模式放液不扣存量。','Picks up or places _1000 mB_ at a time. Collects source fluid only; creative placement preserves contents.'))],
    [(('对容器右击','R-Click a Container'),('从点击面_取液_，尽量装满。','_Fills_ from the clicked face, up to capacity.')),
     (('潜行右击','Sneak + R-Click'),('向容器_倒液_，或强制进入世界放液模式。','_Empties_ into a container or forces world placement.'))])
describe('item.ccd.honeycomb_mold',('将蜂蜜压成_蜜脾_的可重复使用模具。','A reusable mold for pressing honey into _honeycomb_.'),[
    (('与蜂蜜一起放入工作盆时','In a Basin with Honey'),('动力冲压机每次使用_250 mB_蜂蜜，完成后_返还模具_。','A Mechanical Press uses _250 mB_ of honey per operation and _returns the mold_.'))])
describe('item.minecraft.powder_snow_bucket',('细雪也可作为_流体_储存、泵送和打包，每桶_1000 mB_。','Powder snow can be _stored, pumped and packaged as fluid_, with _1000 mB_ per bucket.'),[
    (('装入雾化器时','When Supplied to an Atomizer'),('已安装支持冷冻加工的附属时，可作为_冷冻媒介_。','Acts as a _freezing medium_ when a compatible processing addon is installed.'))])
describe('item.ccd.quicksand_bucket',('由沙子搅拌得到的_喷砂媒介_，可桶装、泵送和运输。','A _sanding medium_ made by mixing sand. Can be bucketed, pumped and transported.'),[
    (('鼓风机吹过流沙或装入雾化器时','When Blown through or Atomized'),('批量处理_砂纸打磨配方_，不消耗砂纸。','Processes _Sandpaper Polishing recipes_ in bulk without sandpaper.'))],
    [(('对 Connected 空触媒右击','R-Click a Connected Empty Catalyst'),('使用_1000 mB_流沙转换为喷砂触媒；需启用对应功能。','Uses _1000 mB_ to form a sanding catalyst when that feature is enabled.'))])
media=[('seething_reagent','超热','seething','Create: Dreams & Desires'),('enriching_reagent','富集','enriching','Create Nuclear'),
       ('soul_stripping_reagent','灵魂剥离','soul stripping','Create: Nether Industry'),('explosive_slurry','爆破','exploding','Create: More Catalysts'),
       ('resonant_solution','共鸣','resonance','Create: More Catalysts'),('sculk_suspension','幽匿','sculking','Create: More Catalysts'),('purifying_solution','净化','purifying','Create: More Catalysts')]
for id,zh,en,mod in media:
    describe('item.ccd.'+id+'_bucket',(f'雾化器的_{zh}媒介_，每桶 _1000 mB_。',f'An Atomizer medium for _{en}_, with _1000 mB_ per bucket.'),[
        (('安装对应附属时','With the Required Addon'),(f'由 _{mod}_ 提供加工。未安装时隐藏获取入口，已有存液保留。',f'Processing is provided by _{mod}_. Acquisition is hidden without it; saved contents are retained.'))])

# Short live-state messages: no underscore formatting outside ItemDescription.
messages={
'ccd.package.invalid':('包裹流体数据无效','Invalid package fluid data'),
'ccd.package.address':('收件地址：%s','Address: %s'),
'ccd.package.use_packager':('使用流体打包机卸下铜包裹中的流体','Unload this Copper Package with a Fluid Packager'),
'ccd.composite.invalid':('包裹内容数据无效','Invalid package contents'),
'ccd.status.ready':('等待打包','Ready to package'), 'ccd.status.output':('等待取走包裹','Package awaiting collection'),
'ccd.status.no_container':('背面未连接流体容器','No fluid container behind'), 'ccd.status.no_fluid':('没有可打包的匹配流体','No matching fluid to package'),
'ccd.status.recovery':('正在退回未接收的流体','Returning unaccepted fluid'),
'ccd.smart_tank.structure':('截面宽 %s 格，长度 %s 格，共 %s 格','Cross-section: %s wide; length: %s; blocks: %s'),
'ccd.smart_tank.unavailable':('储罐结构暂不可用','Tank structure unavailable'),
'ccd.smart_tank.total':('存液：%s / %s','Contents: %s / %s'), 'ccd.smart_tank.types':('种类：%s / %s','Types: %s / %s'),
'ccd.smart_fluid_hatch.unconfigured':('未设置过滤，禁止取放','No filter — transfer blocked'),
'ccd.smart_fluid_hatch.filtered':('按过滤条件取放','Filtered access'),
'ccd.fluid_hatch.proxy':('已连接背后容器','Container connected behind'),
'ccd.fluid_hatch.no_container':('背后没有可用的流体容器','No accessible fluid container behind'),
'ccd.fluid_hatch.rejected':('容器无法完成本次取放','Container cannot complete this transfer'),
'ccd.fluid_access.sources':('连接容器：%s','Connected containers: %s'),
'ccd.fluid_access.no_storage':('与所连接容器共用存液','Shares the connected containers’ fluid'),
'ccd.fluid_arm.hatch_required':('请选择流体舱口正面','Select a Fluid Hatch front'),
'ccd.fluid_arm.input':('已选择取液点','Fluid input selected'), 'ccd.fluid_arm.output':('已选择注液点','Fluid output selected'),
'ccd.fluid_arm.removed':('已取消该交互点','Target deselected'), 'ccd.fluid_arm.limit':('最多选择 %s 个交互点','Maximum targets: %s'),
'ccd.fluid_arm.cleared':('已清空取液与注液点','Fluid targets cleared'),
'ccd.fluid_arm.no_selection':('放置前先选择取液与注液点','Select fluid inputs and outputs before placing'),
'ccd.fluid_arm.no_valid_points':('所选交互点均不可用','No selected targets are available'),
'ccd.fluid_arm.configured':('已设置 %s 个取液点、%s 个注液点；跳过 %s 个无效点','Configured %s inputs, %s outputs; skipped %s invalid targets'),
'ccd.fluid_arm.cargo':('携带：%s，%s','Carrying: %s, %s'), 'ccd.fluid_arm.status':('状态：%s','Status: %s'),
'ccd.fluid_arm.status.stopped':('没有旋转动力','No rotation'), 'ccd.fluid_arm.status.redstone':('红石暂停取液','Pickup paused by redstone'),
'ccd.fluid_arm.status.collecting':('前往取液','Moving to input'), 'ccd.fluid_arm.status.delivering':('前往注液','Moving to output'),
'ccd.fluid_arm.status.waiting_input':('等待可取流体','Waiting for available fluid'), 'ccd.fluid_arm.status.waiting_output':('等待输出接收','Waiting for output space'),
'ccd.fluid_arm.points':('取液：%s　注液：%s','Inputs: %s  Outputs: %s'),
'ccd.smart_faucet.items':('物品加工位置：下方两格','Item processing: two blocks below'),
'ccd.faucet.open':('已开启','Open'), 'ccd.faucet.closed':('已关闭','Closed'), 'ccd.faucet.paused':('红石暂停出液','Flow paused by redstone'),
'ccd.three_way.straight':('通路：公共口 ↔ 直通口','Route: common ↔ straight'), 'ccd.three_way.side':('通路：公共口 ↔ 侧口','Route: common ↔ side'),
'ccd.valve.open':('通路开启','Passage open'), 'ccd.valve.closed':('通路关闭','Passage closed'),
'ccd.transmitter.ready':('允许输送','Transfer enabled'), 'ccd.transmitter.paused':('红石暂停输送','Transfer paused by redstone'),
'ccd.transmitter.rate':('每 %2$s 刻最多 %1$s','Up to %1$s every %2$s ticks'),
'ccd.booster_pump.range':('单侧距离：%s 格','Range per side: %s blocks'), 'ccd.booster_pump.pressure':('泵送压力：原生动力泵的 %s 倍','Pressure: %s× Mechanical Pump'),
'ccd.package.address.copied':('已复制地址：%s','Copied address: %s'), 'ccd.package.address.copied_empty':('已复制清除地址标记','Copied an address-clear marker'),
'ccd.package.address.pasted':('收件地址：%s','Address: %s'), 'ccd.package.address.cleared':('已清除收件地址','Address cleared'),
'ccd.package.address.read_only':('只读剪贴板不能写入地址','Cannot write to a read-only Clipboard'),
'ccd.package.address.missing':('当前页没有未勾选的地址','No unchecked address on this page'),
'ccd.package.address.ambiguous':('请仅保留一个未勾选的地址','Leave exactly one address unchecked'),
'ccd.packager.filter_set':('打包过滤：%s','Packaging filter: %s'), 'ccd.packager.filter_cleared':('已取消打包过滤','Packaging filter cleared'),
'ccd.packager.recovery_amount':('待退回：%s，%s','To return: %s, %s'), 'ccd.fluid_stock.name':('流体标记（%s）','Fluid Marker (%s)'),
'ccd.gauge.amount_tip':('按住右键调整目标存量','Hold right-click to adjust target stock'),
'ccd.gauge.target_amount':('目标存量','Target Stock'), 'ccd.gauge.apply':('设定','Set'), 'ccd.gauge.exact_mb':('存量（mB）','Stock (mB)'),
'ccd.gauge.target_amount.tooltip':('输入 0–100000 mB。1000 mB 为一桶；0 仅监控。','Enter 0–100000 mB. 1000 mB is one bucket; 0 monitors only.'),
'ccd.gauge.apply.tooltip':('设定补货目标','Set the replenishment target'),
'ccd.water_basin.infinite':('无限清水','Infinite water'), 'ccd.water_basin.disposal':('输入流体将被销毁','Incoming fluid is discarded'),
'ccd.water_supply.active':('自动补水','Supplying water'), 'ccd.water_supply.paused':('红石暂停补水','Supply paused by redstone'),
'ccd.water_supply.disabled':('自动补水已禁用','Automatic supply disabled'), 'ccd.water_supply.rate':('每个容器：%s/刻','Per container: %s/tick'),
'ccd.water_supply.unlimited':('单次尽量补满','Fills available space per operation'),
'ccd.collection_grate.infinite':('液源可无限抽取','Infinite fluid source'),
'ccd.smart_collection_grate.active':('自动集液','Collecting fluid'), 'ccd.smart_collection_grate.paused':('红石暂停集液与下排','Collection and output paused by redstone'),
'ccd.smart_collection_grate.unfiltered':('接收所有流体','Accepting all fluids'), 'ccd.smart_collection_grate.filtered':('按过滤条件收集','Filtered collection'),
'ccd.atomizer.active':('正在喷雾','Spraying'), 'ccd.atomizer.idle':('未喷雾','Not spraying')}
for key,pair in messages.items(): put(key,*pair)
for suffix,zh,en in [('pump','泵送','Pumping'),('transfer','输送','Transfer'),('valve','通路','Passage'),('faucet','出液','Pouring'),
 ('water_source','水源','Water Source'),('water_supply','供水','Water Supply'),('packager','流体打包','Fluid Packaging'),
 ('fluid_arm','流体搬运','Fluid Transport'),('smart_tank','储罐存液','Stored Fluids'),('connection','容器连接','Container Connection'),
 ('collection','集液','Collection'),('atomizer','雾化','Atomization')]: put('ccd.gui.goggles.'+suffix,zh+'：',en+':')
# Keep public names and config paths stable; rewrite config explanations in parallel.
configs=[
('boosterPump.stressMultiplier','应力倍率','Stress Multiplier','相同转速下相对于原生动力泵的应力消耗倍数。','Stress cost relative to a Mechanical Pump at the same speed.'),
('boosterPump.pressureMultiplier','压力倍率','Pressure Multiplier','相同转速下相对于原生动力泵的泵送压力倍数，影响流量。','Pumping pressure relative to a Mechanical Pump at the same speed; affects flow rate.'),
('boosterPump.rangeMultiplier','距离倍率','Range Multiplier','原生动力泵配置距离的倍数，分别应用于泵的两侧。','Multiplier of the configured Mechanical Pump range, applied to each side.'),
('fluidArm.allowDirectContainerInteraction','允许直接选择容器','Allow Direct Container Targets','关闭时，移液臂必须通过流体舱口取放；开启后可直接选择容器的可用面。','When disabled, arms require Fluid Hatches. When enabled, they can target accessible container faces directly.'),
('smartTank.maxFluidTypes','每组流体种类上限','Fluid Types per Structure','竖向和横向储罐每组允许的流体种类数。不同附加性质分别计数；降低上限不清除已有内容。','Maximum fluid types per vertical or horizontal tank structure. Different components count separately; lowering the limit preserves existing contents.'),
('waterSupply.rate','每容器供水速率','Supply Rate per Container','单位 mB/刻，按周期集中供水。-1 表示单次尽量补满，0 关闭自动补水；不影响手动取水。','Measured in mB/tick per container, supplied in batches. -1 fills available space per operation; 0 disables automatic supply. Manual extraction is unaffected.'),
('atomizer.normalConsumption','加工媒介耗量','Processing Medium Consumption','单位 mB/秒，仅在有效喷雾时消耗；自定义雾化配方可单独设置。','Measured in mB/second of active spray. Custom atomizing recipes may override this value.'),
('atomizer.potionConsumption','药液耗量','Potion Consumption','单位 mB/秒，控制药水喷雾耗量；自定义配方可单独设置。','Measured in mB/second of potion spray. Custom recipes may override this value.')]
for suffix,zh,en,zt,et in configs:
    put('ccd.configuration.'+suffix,zh,en)
    put('ccd.configuration.'+suffix+'.tooltip',zt+' 重新进入世界后生效。',et+' Reopen the world to apply changes.')
for suffix in ['boosterPump','fluidArm','smartTank','waterSupply','atomizer']:
    put('ccd.configuration.'+suffix+'.tooltip','调整该设备的服务端参数，重新进入世界后生效。','Server-side settings for this device. Reopen the world to apply changes.')
for locale,entries in data.items():
    for key,value in entries.items():
        if '.tooltip.' in key and not key.startswith('ccd.configuration.'):
            assert value.count('_')%2==0,(key,value)
    (LANG/(locale+'.json')).write_text(json.dumps(entries,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
assert set(data['zh_cn'])==set(data['en_us'])
print('Rewritten bilingual tooltips, controls, feedback, goggles and config text:',len(data['en_us']),'keys per locale')
