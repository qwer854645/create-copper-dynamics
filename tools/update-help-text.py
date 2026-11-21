"""Curated bilingual Create-style help. UTF-8 source avoids shell encoding damage."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd/lang'
rows={
'block.ccd.portable_fluid_pump':[
('summary','通过_手动操作_或_旋转力_输送流体。','Moves fluids using _manual operation_ or _Rotational Power_.'),
('control1','右键点击','When R-Clicked'),('action1','手动驱动泵；_潜行_时反向操作。','Operates the pump manually. _Sneak_ to reverse manual rotation.'),
('control2','使用扳手右键点击','When R-Clicked with a Wrench'),('action2','转动泵的管口方向；潜行使用扳手可_拆卸_。','Rotates the pipe connections. Sneak with a Wrench to _pick up_ the pump.')],
'block.ccd.buffer_pump':[
('summary','暂存外部注入的流体，并用_旋转力_向出液口输送。','Buffers externally supplied fluids and delivers them using _Rotational Power_.'),
('condition1','接入旋转力时','When supplied with Rotational Power'),('behaviour1','向出液口输送缓存中的流体，不会主动从其他容器抽液。','Delivers buffered fluid through its outlet without extracting from other containers.'),
('control1','手持装液容器右键点击','When R-Clicked with a filled container'),('action1','将流体装入_2000 mB_缓存。','Fills the _2000 mB_ buffer.')],
'block.ccd.creative_delivery_fluid_tank':[
('summary','持续输出选定流体的_创造模式_储罐。','A _Creative_ tank that continuously supplies the selected fluid.'),
('control1','手持装液容器右键点击','When R-Clicked with a filled container'),('action1','选择要供应的流体；顶部四侧控件用于切换_输出模式_。','Selects the supplied fluid. The four upper side controls set the _Output Mode_.'),
('control2','使用扳手右键点击','When R-Clicked with a Wrench'),('action2','调整_出液口_方向，侧面槽位随储罐转动。','Rotates the _outlet_. The side slots rotate with the tank.'),
('condition1','允许向世界输出时','When world output is enabled'),('behaviour1','可通过开放管口排液；默认只向_容器_供液。','May discharge through open pipe ends. By default, only _containers_ receive fluid.')],
'item.ccd.fluid_filter':[
('summary','为流体设备指定_允许_或_拒绝_通过的流体。','Selects fluids to _Allow_ or _Deny_ in fluid-handling components.'),
('control1','手持时右键点击','When R-Clicked in hand'),('action1','打开过滤器，设置最多_18_个流体样本。','Opens the filter to configure up to _18_ fluid samples.'),
('control2','拖入装液容器或浏览器流体条目','When dragging in a fluid container or browser fluid'),('action2','记录流体样本，不消耗物品；右键槽位可_清除_样本。','Copies a fluid sample without consuming items. R-Click a slot to _clear_ it.'),
('condition1','启用精确匹配时','When exact matching is enabled'),('behaviour1','同时比较流体种类与_数据组件_。','Matches both the fluid type and its _Data Components_.')],
'item.ccd.fluid_attribute_filter':[
('summary','根据流体的_属性_进行筛选。','Filters fluids by their _Attributes_.'),
('control1','手持时右键点击','When R-Clicked in hand'),('action1','打开过滤器，将流体放入_参考槽_，滚动选择属性。','Opens the filter. Place a fluid in the _reference slot_ and scroll to select an attribute.'),
('control2','点击添加或反向添加','When selecting Add or Add Inverted'),('action2','保存选定属性；反向添加要求流体_不具有_该属性。','Saves the selected attribute. Inverted attributes require the fluid to _not match_.'),
('condition1','选择筛选方式时','When choosing a matching mode'),('behaviour1','可要求满足_任一_或_全部_属性，也可拒绝匹配的流体。','Allows fluids matching _Any_ or _All_ attributes, or denies matching fluids.')],
'block.ccd.redstone_fluid_pump':[
('condition1','接收红石信号时','When powered by Redstone'),('behaviour1','_反转_流体输送方向，传动轴继续转动。','_Reverses_ fluid flow while the shafts continue rotating.')],
'block.ccd.reciprocating_fluid_pump':[
('control1','将齿轮接入顶部或底部','When meshing a Cogwheel above or below'),('action1','为泵提供_旋转力_，两个管口不接受齿轮传动。','Supplies _Rotational Power_. The pipe mouths cannot mesh with Cogwheels.'),
('condition1','改变红石信号强度时','When Redstone strength changes'),('behaviour1','调整目标_流量_与_输送距离_。','Adjusts the target _Flow Rate_ and _Range_.')],
'block.ccd.turning_booster_pump':[
('condition1','接入旋转力时','When supplied with Rotational Power'),('behaviour1','在两个相邻管口之间输送流体，轴接口位于管口所在平面内。','Moves fluid between adjacent mouths, with shaft ports in the same plane.')],
'block.ccd.vertical_turning_booster_pump':[
('condition1','接入旋转力时','When supplied with Rotational Power'),('behaviour1','在两个相邻管口之间输送流体，贯通轴_垂直于管口平面_。','Moves fluid between adjacent mouths. The shaft runs _perpendicular to their plane_.')],
'block.ccd.bidirectional_fluid_pump':[
('condition1','反转旋转方向时','When rotation is reversed'),('behaviour1','同时反转_流体方向_。','Also reverses the _Direction of Flow_.')],
'block.ccd.flow_meter':[
('condition1','流体通过时','When fluid passes through'),('behaviour1','指针随实际流量变化；停止输送时归零。','The needle responds to transferred fluid and returns to zero when flow stops.'),
('control1','佩戴工程师护目镜观察','When viewed with Engineer’s Goggles'),('action1','显示每游戏刻通过的流体量，单位为_mB/t_。','Displays the transferred volume per tick in _mB/t_.'),
('control2','使用扳手右键点击','When R-Clicked with a Wrench'),('action2','调整_表盘_朝向。','Changes the _dial_ orientation.')],
'block.ccd.omni_pump':[
('control1','使用扳手右键点击一个面','When R-Clicking a face with a Wrench'),('action1','依次切换为_输入_、_输出_、_应力口_和_关闭_，其他面保持原样。','Cycles _Input_, _Output_, _Shaft_ and _Closed_ without changing other faces.'),
('condition1','接入旋转力时','When supplied with Rotational Power'),('behaviour1','从输入面收液并向输出面供液，所有输出共用输送能力。','Draws fluid through inputs and supplies outputs. All outputs share the transfer rate.')],
'item.ccd.fluid_arm_cargo':[
('summary','动力移液臂使用的_流体载荷_，用于保存运输中的流体。','The Mechanical Fluid Arm’s _fluid cargo_, holding fluid in transit.')],
'item.ccd.seething_reagent_bucket':[
('control1','向烈焰人燃烧室供液','When supplied to a Blaze Burner'),('action1','每_1000 mB_提供_160 秒_超级燃烧，也可直接使用桶装燃料。','Each _1000 mB_ provides _160 seconds_ of Superheating. Buckets can also be fed directly.'),
('condition1','安装超热气流加工附属时','When a seething fan-processing addon is installed'),('behaviour1','装入流体雾化器可使气流获得_超热_加工效果。','Can also provide _Seething_ processing in a Fluid Atomizer.')]
}
scenes={
'fluid_ordering':('监控与订购流体',['流体工厂仪表监控流体库存，并通过物流网络请求补足数量。','使用已调频的库存链接器选择网络，再用装液容器选择流体。','目标数量以 mB 设置，设为零时仅监控库存。','流体识别标记表示订单中的数量，实际流体通过铜包裹运输。']),
'pump_variants':('选择泵的连接方式',['转向增压泵在两个相邻管口之间输送流体。','垂直转向增压泵的传动轴垂直于两个管口所在的平面。','红石信号会反转红石双向动力泵的输送方向。','双向动力泵随旋转方向反转流体方向。','调频动力泵根据信号强度调整流量和距离，齿轮从顶部或底部接入。','右键点击便携式流体泵即可手动操作，潜行时反向操作。']),
'buffered_delivery':('输送缓存中的流体',['缓存泵可储存由管道或手持容器注入的 2000 mB 流体。','旋转力将缓存中的流体送至出液口，不会主动从其他容器抽液。','手持装液容器右键选择流体，输送式创造储罐可无限供应该流体。','使用扳手调整出液口，顶部侧面控件可切换容器供液与向世界输出。']),
'omni_pumping':('设置万向泵的接口',['万向泵放下时，六个面均为关闭状态。','用扳手右键点击一个面，可依次切换输入、输出、应力口和关闭。','向应力口接入旋转力，即可从输入面收液并向输出面供液。','所有输出共用输送能力，反转旋转方向不会交换接口模式。']),
'flow_metering':('测量管道流量',['将流量计串接在两根流体管道之间。','指针随实际输送量变化，停止流动时归零。','佩戴工程师护目镜可查看流速，单位为 mB/t。','使用扳手调整表盘朝向。']),
'redstone_valving':('控制流体通路',['红石流体阀门未通电时允许流体通过。','红石信号会关闭通路，信号消失后重新开启。','仍需使用动力泵驱动管道中的流体。']),
'fluid_filtering':('筛选流体',['手持流体过滤器右键点击，可设置流体样本。','从装液容器复制样本，或从配方浏览器拖入流体条目，不会消耗物品。','流体属性过滤器从参考流体中选择属性，点击添加或反向添加保存规则。','选择满足任一、全部属性或拒绝匹配，再将过滤器放入设备的筛选槽。']),
'metal_buckets':('使用大容量桶',['铜桶和黄铜桶可储存多桶同种流体。','右键点击容器取液，潜行右键点击倒液。','在世界中每次取放一桶流体，黄铜桶的容量比铜桶更大。']),
'fluid_media':('使用加工流体',['流体雾化器消耗储存的流体，为气流提供加工效果。','可选加工流体需要安装对应附属，才能提供相应的气流加工类型。','超级熔岩也可供给烈焰人燃烧室，每 1000 mB 提供 160 秒超级燃烧。'])
}
import re
summaries={
'booster_pump':('使用_旋转力_驱动管道流体，提供更远的_输送距离_。','Uses _Rotational Power_ to move fluids through pipes over a greater _Range_.'),
'fluid_transmitter':('无需旋转力，在两端容器之间_输送流体_。','_Transfers fluids_ between end containers without Rotational Power.'),
'fluid_packager':('将容器中的流体_打包_，或将包裹中的流体_拆出_。','_Packs_ fluids from a container and _unpacks_ fluid packages into it.'),
'fluid_repackager':('将同一订单中的_物品与流体_合并为黄铜包裹。','Combines _items and fluids_ from the same order into Brass Packages.'),
'fluid_arm':('在选定的输入端和输出端之间_搬运流体_。','_Transfers fluids_ between selected input and output locations.'),
'fluid_hatch':('为手持容器提供背后储罐的_流体存取_接口。','Provides _fluid access_ to the container behind it for handheld containers.'),
'smart_fluid_hatch':('可_筛选流体_的流体舱口。','A Fluid Hatch that can _filter fluids_.'),
'smart_fluid_tank':('可组合扩容，并在同一结构内储存_多种流体_。','Combines into larger structures that can store _multiple fluids_.'),
'horizontal_smart_fluid_tank':('沿_水平方向_组合扩容，可储存多种流体。','Combines _horizontally_ into larger structures storing multiple fluids.'),
'faucet':('将容器中的流体_倒入下方容器_。','Pours fluid from a container into _containers below_.'),
'smart_faucet':('可筛选流体的龙头，也能为下方的_物品注液_。','A filtering Faucet that can also _fill items_ below.'),
'fluid_atomizer':('消耗流体，为气流提供_批量加工_效果。','Consumes fluids to provide _Bulk Processing_ effects to air currents.'),
'copper_collection_grate':('收集接触顶部的流体，排入_正下方的容器_。','Collects fluid touching its top and drains into a _container directly below_.'),
'smart_collection_grate':('可_筛选流体_并接受红石控制的集液栅。','A Collection Grate with _fluid filtering_ and Redstone control.'),
'fluid_access_port':('扩展所连接容器的_流体接口_。','Extends access to an attached container’s _fluid interface_.'),
'fluid_bridge':('通过同一个接口存取_两端容器_中的流体。','Provides fluid access to _both end containers_ through one interface.'),
'redstone_fluid_valve':('用_红石信号_开关流体管路。','Opens and closes a fluid connection using a _Redstone Signal_.'),
'three_way_valve':('用_红石信号_切换公共管口所连接的支路。','Uses a _Redstone Signal_ to switch the branch connected to its common port.'),
'redstone_fluid_pump':('使用旋转力输送流体，用_红石信号_反转流向。','Uses Rotational Power to move fluid and a _Redstone Signal_ to reverse flow.'),
'bidirectional_fluid_pump':('流体方向随_旋转方向_改变的动力泵。','A Mechanical Pump whose flow follows its _Direction of Rotation_.'),
'reciprocating_fluid_pump':('使用旋转力输送流体，以_红石信号强度_调整流量和距离。','Uses Rotational Power to move fluid, with _Redstone strength_ adjusting flow and range.')
}
source=Path(__file__).resolve().parents[1]/'src/main/java/dev/ccd/client/ponder/CCDPonderScenes.java'
scene_source=source.read_text(encoding='utf-8')
# Final concise descriptions replace their whole group, removing obsolete numbered rows.
concise={
'block.ccd.portable_fluid_pump':[
('summary','通过_手动操作_或_旋转力_输送流体。','Moves fluids using _manual operation_ or _Rotational Power_.'),
('control1','右键点击时','When R-Clicked'),('action1','手动驱动泵，潜行时_反向_操作。','Operates the pump manually. Sneak to _reverse_ rotation.')],
'block.ccd.buffer_pump':[
('summary','使用_旋转力_输出缓存中的流体。','Uses _Rotational Power_ to deliver buffered fluid.'),
('control1','手持装液容器右键点击时','When R-Clicked with a filled container'),('action1','填充_2000 mB_缓存，也可通过管道注液。','Fills the _2000 mB_ buffer. Pipes can also supply fluid.')],
'block.ccd.turning_booster_pump':[
('summary','使用_旋转力_在相邻管口之间输送流体。','Uses _Rotational Power_ to move fluid between adjacent pipe connections.'),
('condition1','连接传动轴时','When connecting Shafts'),('behaviour1','两个应力口与管口位于_同一平面_。','The two shaft connections lie _in the plane_ of the pipe connections.')],
'block.ccd.vertical_turning_booster_pump':[
('summary','使用_旋转力_在相邻管口之间输送流体。','Uses _Rotational Power_ to move fluid between adjacent pipe connections.'),
('condition1','连接传动轴时','When connecting Shafts'),('behaviour1','贯通轴_垂直于_管口所在平面。','The shaft runs _perpendicular_ to the plane of the pipe connections.')],
'block.ccd.redstone_fluid_pump':[
('summary','使用_旋转力_输送流体。','Uses _Rotational Power_ to move fluid.'),
('condition1','接收红石信号时','When powered by Redstone'),('behaviour1','_反转_流向。','_Reverses_ the direction of flow.')],
'block.ccd.bidirectional_fluid_pump':[
('summary','使用_旋转力_输送流体。','Uses _Rotational Power_ to move fluid.'),
('condition1','反转旋转方向时','When rotation is reversed'),('behaviour1','_反转_流向。','_Reverses_ the direction of flow.')],
'block.ccd.reciprocating_fluid_pump':[
('summary','使用_旋转力_输送流体，流量和距离由_红石信号强度_控制。','Uses _Rotational Power_ to move fluid. _Redstone strength_ controls flow rate and range.'),
('control1','在顶部或底部连接齿轮时','When meshing a Cogwheel above or below'),('action1','为泵提供_旋转力_。','Supplies _Rotational Power_.'),
('condition1','接收红石信号时','When receiving a Redstone Signal'),('behaviour1','侧面指针显示_信号强度_。','The side needles indicate _signal strength_.')],
'block.ccd.creative_delivery_fluid_tank':[
('summary','无限供应选定的_流体_。','Provides an unlimited supply of the selected _fluid_.'),
('control1','手持装液容器右键点击时','When R-Clicked with a filled container'),('action1','选择供应的_流体_。','Selects the supplied _fluid_.'),
('control2','滚动顶部侧面控件时','When scrolling an upper side control'),('action2','切换_仅向容器输送_或_允许向世界排液_。','Switches between _container delivery_ and _world output_.'),
('control3','使用扳手右键点击时','When R-Clicked with a Wrench'),('action3','调整_出液口_朝向。','Rotates the _outlet_.')],
'block.ccd.omni_pump':[
('summary','使用_旋转力_在自定义输入、输出面之间输送流体。','Uses _Rotational Power_ to move fluid between configurable inputs and outputs.'),
('control1','使用扳手右键点击时','When R-Clicked with a Wrench'),('action1','切换所点击面的_输入_、_输出_、_应力口_和_关闭_模式。','Cycles the clicked face through _Input_, _Output_, _Shaft_ and _Closed_.'),
('condition1','设置多个输出面时','When multiple outputs are configured'),('behaviour1','所有输出共用_输送流量_。','All outputs share the _transfer rate_.')],
'block.ccd.flow_meter':[
('summary','通过指针显示管道中的_流量_。','Indicates pipe _flow rate_ with a needle.'),
('condition1','佩戴工程师护目镜时','When wearing Engineer’s Goggles'),('behaviour1','显示当前流量，单位为_mB/t_。','Displays the current flow rate in _mB/t_.'),
('control1','使用扳手右键点击时','When R-Clicked with a Wrench'),('action1','调整_表盘_朝向。','Rotates the _dial_.')],
'item.ccd.fluid_filter':[
('summary','指定_允许_或_拒绝_通过的流体。','Selects fluids to _Allow_ or _Deny_.'),
('control1','手持右键点击时','When R-Clicked in hand'),('action1','打开设置。拖入流体样本，右键点击槽位_清除_样本。','Opens the settings. Drag in fluid samples; R-Click a slot to _clear_ it.'),
('condition1','启用精确匹配时','When exact matching is enabled'),('behaviour1','同时比较流体种类和_数据组件_。','Matches fluid type and _Data Components_.')],
'item.ccd.fluid_attribute_filter':[
('summary','根据_属性_允许或拒绝流体通过。','Allows or denies fluids by their _Attributes_.'),
('control1','手持右键点击时','When R-Clicked in hand'),('action1','打开设置。放入参考流体，滚动选择属性并_添加_。','Opens the settings. Set a reference fluid, scroll to an attribute and _Add_ it.'),
('condition1','反向添加属性时','When adding an inverted attribute'),('behaviour1','要求流体_不匹配_该属性。','Requires the fluid to _not match_ that attribute.')],
'item.ccd.seething_reagent_bucket':[
('summary','供烈焰人燃烧室进行_超级燃烧_的流体燃料。','A fluid fuel for _Superheating_ Blaze Burners.'),
('condition1','供给烈焰人燃烧室时','When supplied to a Blaze Burner'),('behaviour1','每_1000 mB_提供_160 秒_超级燃烧。','Each _1000 mB_ provides _160 seconds_ of Superheating.'),
('condition2','安装对应加工附属时','When the matching processing addon is installed'),('behaviour2','装入流体雾化器可提供_超热_加工。','Provides _Seething_ processing in a Fluid Atomizer.')]
}
for locale in ['zh_cn','en_us']:
    path=root/(locale+'.json');d=json.loads(path.read_text(encoding='utf-8'))
    for prefix,entries in rows.items():
        for suffix,zh,en in entries:d[prefix+'.tooltip.'+suffix]=zh if locale=='zh_cn' else en
    for id,(zh,en) in summaries.items():d['block.ccd.'+id+'.tooltip.summary']=zh if locale=='zh_cn' else en
    for id,(title,lines) in scenes.items():
        match=re.search(r'begin\(builder,"'+id+r'","([^"]+)"\)(.*?)(?=\n    public static|\n    private static|\Z)',scene_source,re.S)
        english=re.findall(r'text\(s,util,[^,]+,"([^"]+)"\)',match.group(2))
        assert len(english)==len(lines),(id,len(english),len(lines))
        d['ccd.ponder.'+id+'.header']=title if locale=='zh_cn' else match.group(1)
        for i,line in enumerate(lines):d[f'ccd.ponder.{id}.text_{i+1}']=line if locale=='zh_cn' else english[i]
    # No fixed capacity claim: capacity is adjustable in the server configuration.
    d['item.ccd.copper_bucket.tooltip.summary']='可多次取放流体的_大容量_铜桶。' if locale=='zh_cn' else 'A _high-capacity_ Copper Bucket for repeated filling and emptying.'
    d['item.ccd.brass_bucket.tooltip.summary']='比铜桶容量更大的_黄铜桶_。' if locale=='zh_cn' else 'A _Brass Bucket_ with more capacity than the Copper Bucket.'
    for medium in ['enriching_reagent','soul_stripping_reagent','explosive_slurry','resonant_solution','sculk_suspension','purifying_solution']:
        d['item.ccd.'+medium+'_bucket.tooltip.condition1']='安装对应加工附属并装入雾化器时' if locale=='zh_cn' else 'When used in an Atomizer with the matching processing addon'
    for prefix,entries in concise.items():
        for key in list(d):
            if key.startswith(prefix+'.tooltip.'):del d[key]
        for suffix,zh,en in entries:d[prefix+'.tooltip.'+suffix]=zh if locale=='zh_cn' else en
    # Shared wording for equivalent components; keep distinct behavior where it matters.
    for material in ['copper','brass']:
        prefix=f'item.ccd.{material}_bucket.tooltip.'
        d[prefix+'control1']='对容器右键点击时' if locale=='zh_cn' else 'When R-Clicked on a container'
        d[prefix+'action1']='从容器中_取液_。' if locale=='zh_cn' else '_Draws fluid_ from the container.'
        d[prefix+'control2']='潜行右键点击时' if locale=='zh_cn' else 'When Sneak-R-Clicked'
        d[prefix+'action2']='向容器或世界中_倒液_。' if locale=='zh_cn' else '_Pours fluid_ into a container or the world.'
        d[prefix+'condition1']='在世界中取放流体时' if locale=='zh_cn' else 'When transferring fluid in the world'
        d[prefix+'behaviour1']='每次取放_1000 mB_，只能收取流体源。' if locale=='zh_cn' else 'Transfers _1000 mB_ at a time and collects source blocks only.'
    for id in ['smart_fluid_tank','horizontal_smart_fluid_tank']:
        d[f'block.ccd.{id}.tooltip.condition1']='通过管道取液时' if locale=='zh_cn' else 'When extracting through Fluid Pipes'
        d[f'block.ccd.{id}.tooltip.behaviour1']='优先输出列表中_靠前的流体_，可用过滤器指定流体。' if locale=='zh_cn' else 'Extracts the _first available fluid_. A filter can select a specific fluid.'
    for id in ['faucet','smart_faucet']:
        d[f'block.ccd.{id}.tooltip.behaviour1']='_暂停_出液。' if locale=='zh_cn' else '_Pauses_ fluid output.'
    for key in list(d):
        if '.tooltip.control' in key and locale=='zh_cn':
            d[key]=d[key].replace('右击','右键点击').replace('左击','左键点击')
        if '.tooltip.action' in key and locale=='zh_cn':
            d[key]=d[key].replace('右击','右键点击').replace('左击','左键点击')
    d['ccd.creative_delivery.active']='无限供应' if locale=='zh_cn' else 'Unlimited supply'
    d['ccd.creative_delivery.mode']='输出模式' if locale=='zh_cn' else 'Output Mode'
    d['ccd.filter.inverted']='反向属性' if locale=='zh_cn' else 'Inverted attribute'
    d['ccd.filter.attribute_help']='在设置界面中选择并添加属性' if locale=='zh_cn' else 'Select and add attributes in the filter settings'
    path.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
