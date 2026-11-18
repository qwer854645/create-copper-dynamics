"""Editorial pass based on Create 6.0.10's shipped zh_cn/en_us language files.

Keeps IDs, format arguments and gameplay behaviour unchanged. Run on current
language files, not the historical tooltip generators.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LANG = ROOT / 'src/main/resources/assets/ccd/lang'
data = {locale: json.loads((LANG / (locale + '.json')).read_text(encoding='utf-8'))
        for locale in ('zh_cn', 'en_us')}


def put(key, zh, en):
    assert key in data['zh_cn'] and key in data['en_us'], key
    data['zh_cn'][key], data['en_us'][key] = zh, en


def tip(name, section, zh, en):
    put(('item.' if name in ('copper_bucket', 'fluid_stock', 'fluid_package', 'composite_package') else 'block.')
        + 'ccd.' + name + '.tooltip.' + section, zh, en)


# Short purposes first; operating conditions carry the detailed instructions.
summaries = {
    'booster_pump': ('以更大的_应力消耗_，提高管道的_泵送压力_和_输送距离_。', 'Provides greater _pressure_ and _range_ for fluid pipes at a higher _Stress Impact_.'),
    'fluid_transmitter': ('在容器之间_输送流体_，无需旋转动力。', '_Transfers fluids_ between containers without rotational power.'),
    'redstone_fluid_valve': ('使用_红石信号_控制流体管道的通断。', 'Controls fluid pipe connections using a _Redstone Signal_.'),
    'three_way_valve': ('使用_红石信号_在两条流体通路之间切换。', 'Switches between two fluid connections using a _Redstone Signal_.'),
    'faucet': ('将容器中的流体_倒入下方_，无需旋转动力。', '_Pours fluids_ from a container into the space below, without rotational power.'),
    'smart_faucet': ('将选定的流体_倒入下方_，也可用于_物品注液_。', '_Pours selected fluids_ into containers or uses them for _Filling Items_ below.'),
    'fluid_packager': ('将流体装入_铜包裹_，或将包裹中的流体存入容器。', 'Packs fluids into _Copper Packages_ or unpacks them into a container.'),
    'fluid_gauge': ('监控物流网络中的_流体存量_，并自动请求补货。', 'Monitors _Fluid Stock_ in a logistics network and requests replenishment.'),
    'fluid_arm': ('使用旋转动力，在选定位置之间_搬运流体_。', 'Uses rotational power to _Move Fluids_ between selected locations.'),
    'fluid_hatch': ('为容器提供便捷的_流体存取口_，可供手持容器和_动力移液臂_使用。', 'Provides _Fluid Access_ for handheld containers and _Mechanical Fluid Arms_.'),
    'smart_fluid_hatch': ('为容器提供带有_过滤器_的流体存取口。', 'Provides _Filtered Fluid Access_ to a container.'),
    'smart_fluid_tank': ('可同时储存_多种流体_，并能沿竖直方向_堆叠扩容_。', 'Stores _Multiple Fluids_ and can be _Stacked Vertically_ for greater capacity.'),
    'horizontal_smart_fluid_tank': ('可同时储存_多种流体_，并能沿水平方向_连接扩容_。', 'Stores _Multiple Fluids_ and can be _Extended Horizontally_ for greater capacity.'),
    'copper_water_basin': ('为手持容器和流体管道提供_无限水源_。', 'Provides an _Infinite Water Source_ for handheld containers and fluid pipes.'),
    'copper_water_supply': ('向相邻容器_自动供水_的无限水源。', 'An infinite water source that _Supplies Adjacent Containers_ automatically.'),
    'copper_collection_grate': ('收集上方的流体，并将其_排入下方容器_。', 'Collects fluids from above and _Drains into Containers Below_.'),
    'smart_collection_grate': ('收集选定的流体，并将其_排入下方容器_。可使用红石控制。', 'Collects selected fluids and _Drains into Containers Below_. Can be controlled with Redstone.'),
    'fluid_access_port': ('扩展容器的_流体存取面_，让更多设备共用其中的流体。', 'Extends a container’s _Fluid Access_ to additional devices.'),
    'fluid_bridge': ('将两端容器接到同一个_流体存取口_。', 'Combines two containers into a single _Fluid Access Point_.'),
    'fluid_atomizer': ('将储存的流体雾化，用于_气流加工_。', 'Atomizes stored fluids for _Bulk Processing_ with air currents.'),
    'copper_bucket': ('可携带_四桶_同种流体的大容量铜桶。', 'A large bucket that holds _Four Buckets_ of a single fluid.'),
    'fluid_stock': ('在物流订单中代表_流体_，每份对应_1 mB_。', 'Represents _Fluids_ in logistics orders. Each unit represents _1 mB_.'),
    'fluid_package': ('在物流系统中_运输流体_的密封包裹。', 'A sealed package for _Transporting Fluids_ through logistics systems.'),
    'composite_package': ('在同一包裹中运输_物品和流体_。', 'Transports _Items and Fluids_ in the same package.'),
}
for name, values in summaries.items():
    tip(name, 'summary', *values)

tip('booster_pump', 'behaviour1', '两端连接流体管道，四侧可接入传动轴。_旋转方向_不影响流向。', 'Connect Fluid Pipes at either end and Shafts at the sides. _Rotation Direction_ does not affect the flow.')
tip('booster_pump', 'action1', '_反转_流体输送方向。', '_Reverses_ the direction of flow.')
tip('fluid_arm', 'behaviour1', '默认通过_流体舱口正面_取液和注液，每次最多搬运_1000 mB_。', 'Uses the _Front of Fluid Hatches_ by default. Moves up to _1000 mB_ at a time.')
tip('fluid_access_port', 'behaviour2', '_断开_容器连接。未通电时，比较器转发容器的信号。', '_Disconnects_ the container. While unpowered, a Comparator repeats the container’s signal.')
tip('fluid_bridge', 'behaviour2', '_断开_两端连接。未通电时，比较器输出两端信号之和的一半。', '_Disconnects_ both containers. While unpowered, a Comparator outputs half the sum of their signals.')
tip('fluid_bridge', 'behaviour1', '该端只允许_匹配的流体_进出。另一端未设置过滤时，这些流体仍只通过已设置过滤的一端。', 'Only _Matching Fluids_ can enter or leave that end. They are excluded from the opposite end if its filter is empty.')
tip('fluid_transmitter', 'behaviour1', '沿_箭头方向_将流体送入另一端容器。直线串接时，整条链共用一次输送量。', 'Transfers fluids towards the _Arrow_ into the opposite container. A straight chain shares one transfer allowance.')
for name in ('smart_fluid_tank', 'horizontal_smart_fluid_tank'):
    tip(name, 'behaviour1', '截面可为_1×1、2×2 或 3×3_。完整结构中的储罐共用容量，不同流体分别储存。', 'The cross-section can be _1×1, 2×2 or 3×3_. Tanks in a complete structure share capacity while keeping fluids separate.')
    tip(name, 'behaviour2', '未设置过滤时，先抽取列表中_靠前的流体_。可使用过滤器选择所需流体。', 'Without a filter, the _First Available Fluid_ is extracted. Use a filter to select a specific fluid.')
for name in ('fluid_hatch', 'smart_fluid_hatch'):
    tip(name, 'behaviour1', '从外露的_正面_存取流体。管道不能通过舱口连接容器。', 'Access fluids from the exposed _Front_. Fluid Pipes cannot connect through the hatch.')
    tip(name, 'action1', '将手持容器中的流体_存入_。无法存入时，尝试从容器中_取出_流体。', '_Empties_ the held container. If it cannot be emptied, attempts to _Fill_ it instead.')

# Use Create's concise condition headings consistently.
for locale, entries in data.items():
    for key, value in list(entries.items()):
        if '.tooltip.control' in key:
            if locale == 'zh_cn':
                value = value.replace('使用扳手右击', '使用扳手右击时').replace('右击机身', '右击时')
                value = value.replace('右击时时', '右击时')
            entries[key] = value

labels = {
    'ccd.fluid_arm.status.stopped': ('未接入动力', 'No Rotational Power'),
    'ccd.fluid_arm.status.redstone': ('已暂停取液', 'Collection Paused'),
    'ccd.fluid_arm.status.collecting': ('正在取液', 'Collecting Fluid'),
    'ccd.fluid_arm.status.delivering': ('正在注液', 'Delivering Fluid'),
    'ccd.fluid_arm.status.waiting_input': ('等待流体', 'Awaiting Fluid'),
    'ccd.fluid_arm.status.waiting_output': ('等待可用输出', 'Awaiting Output'),
    'ccd.smart_fluid_hatch.unconfigured': ('未设置过滤器', 'No Filter Set'),
    'ccd.smart_fluid_hatch.filtered': ('已设置流体过滤器', 'Fluid Filter Set'),
    'ccd.fluid_hatch.proxy': ('已连接容器', 'Container Connected'),
    'ccd.fluid_hatch.no_container': ('未连接流体容器', 'No Fluid Container Connected'),
    'ccd.fluid_hatch.rejected': ('无法存取该流体', 'Cannot Transfer This Fluid'),
    'ccd.faucet.open': ('开启', 'Open'),
    'ccd.faucet.closed': ('关闭', 'Closed'),
    'ccd.faucet.paused': ('已暂停出液', 'Flow Paused'),
    'ccd.transmitter.ready': ('可输送', 'Ready'),
    'ccd.transmitter.paused': ('已暂停输送', 'Transfer Paused'),
    'ccd.status.ready': ('等待打包', 'Ready to Package'),
    'ccd.status.output': ('等待取走包裹', 'Awaiting Package Collection'),
    'ccd.status.no_container': ('未连接流体容器', 'No Fluid Container Connected'),
    'ccd.status.no_fluid': ('没有匹配的流体', 'No Matching Fluid'),
    'ccd.status.recovery': ('正在退回流体', 'Returning Fluid'),
    'ccd.water_supply.active': ('正在供水', 'Supplying Water'),
    'ccd.water_supply.paused': ('已暂停供水', 'Water Supply Paused'),
    'ccd.water_supply.disabled': ('自动供水已禁用', 'Automatic Water Supply Disabled'),
    'ccd.smart_collection_grate.active': ('正在集液', 'Collecting Fluid'),
    'ccd.smart_collection_grate.paused': ('已暂停集液与排液', 'Collection and Drain Paused'),
    'ccd.atomizer.active': ('正在雾化', 'Atomizing'),
    'ccd.atomizer.idle': ('未在雾化', 'Idle'),
    'ccd.gui.goggles.pump': ('泵送信息：', 'Pump Info:'),
    'ccd.gui.goggles.transfer': ('输送信息：', 'Transfer Info:'),
    'ccd.gui.goggles.valve': ('阀门状态：', 'Valve Status:'),
    'ccd.gui.goggles.faucet': ('龙头状态：', 'Faucet Status:'),
    'ccd.gui.goggles.water_source': ('水源信息：', 'Water Source Info:'),
    'ccd.gui.goggles.water_supply': ('供水状态：', 'Water Supply Status:'),
    'ccd.gui.goggles.packager': ('打包机状态：', 'Packager Status:'),
    'ccd.gui.goggles.fluid_arm': ('移液臂状态：', 'Fluid Arm Status:'),
    'ccd.gui.goggles.connection': ('容器连接信息：', 'Container Connection Info:'),
    'ccd.gui.goggles.collection': ('集液状态：', 'Collection Status:'),
    'ccd.gui.goggles.atomizer': ('雾化器状态：', 'Atomizer Status:'),
}
for key, values in labels.items():
    put(key, *values)

# Ponder explains one visible step at a time, using the same device names.
ponder = {
    'fluid_packaging': [
        ('打包与拆包流体', 'Packaging and Unpacking Fluids'),
        ('流体打包机的背面需要连接流体容器', 'The rear of a Fluid Packager connects to a fluid container'),
        ('红石信号使打包机将最多 4000 mB 流体装入铜包裹', 'A Redstone Signal packs up to 4000 mB into a Copper Package'),
        ('铜包裹的地址和运输方式与普通包裹相同', 'Copper Packages use the same addresses and transport as ordinary Packages'),
        ('收到铜包裹后，打包机会将流体存入背后的容器', 'Arriving Copper Packages are emptied into the attached container'),
        ('使用装有流体的容器右击，可设置红石打包的过滤条件', 'R-Click with a filled container to filter Redstone-triggered packaging'),
        ('理包机可将同一订单的物品和流体合并为黄铜包裹', 'A Repackager combines items and fluids from the same order into Brass Packages')],
    'fluid_hatches': [
        ('使用流体舱口存取流体', 'Accessing Fluids using Fluid Hatches'),
        ('流体舱口可用于存取其背后容器中的流体', 'Fluid Hatches provide access to fluids in the container behind them'),
        ('手持流体容器右击舱口正面，即可存入或取出流体', 'R-Click the front with a fluid container to empty or fill it'),
        ('智能流体舱口只允许过滤器选定的流体通过', 'Smart Fluid Hatches only allow fluids selected by their filter'),
        ('放置动力移液臂前，先选择舱口正面作为取液点和注液点', 'Before placing a Mechanical Fluid Arm, select hatch fronts as inputs and outputs'),
        ('接入旋转动力后，移液臂会在选定位置之间搬运流体', 'Supply rotational power to move fluids between the selected locations')],
    'fluid_access': [
        ('扩展流体容器的连接', 'Extending Fluid Container Connections'),
        ('流体扩展端口的所有面均可访问所连接的容器', 'All faces of a Fluid Access Port share access to its attached container'),
        ('扩展端口和流体桥接器不能相互连接以扩展库存', 'Fluid Access Ports and Fluid Bridges cannot extend each other'),
        ('流体桥接器可通过同一接口访问两端的容器', 'A Fluid Bridge provides access to both end containers through one interface'),
        ('一端设置过滤后，匹配流体不会通过另一端的空过滤器', 'Fluids selected by one end are excluded from the opposite end if its filter is empty')],
    'smart_tanks': [
        ('智能储罐的组合与存储', 'Storage using Smart Fluid Tanks'),
        ('智能储罐可以堆叠，提供更多的存储量', 'Smart Fluid Tanks can be stacked for greater storage capacity'),
        ('储罐截面可组成 1×1、2×2 或 3×3 的正方形', 'Their square cross-section can be 1×1, 2×2 or 3×3 blocks'),
        ('横向智能储罐沿放置时选择的水平方向延伸', 'Horizontal Smart Fluid Tanks extend along their placement axis'),
        ('不同流体分别储存，共用整组储罐的容量', 'Different fluids remain separate while sharing the structure’s capacity'),
        ('使用扳手可以打开或关闭观察窗', 'A Wrench can be used to toggle the windows')],
    'fluid_faucets': [
        ('使用龙头倾倒流体', 'Pouring Fluids using Faucets'),
        ('龙头需要安装在流体容器的侧面', 'Faucets attach to the side of a fluid container'),
        ('开启龙头，即可向正下方的容器注入流体', 'Open the Faucet to fill a container directly below'),
        ('智能龙头还可为下方两格的传送带或置物台上的物品注液', 'Smart Faucets can also fill items on Belts or Depots two blocks below')],
    'fluid_atomizing': [
        ('使用流体雾化器进行气流加工', 'Bulk Processing using Fluid Atomizers'),
        ('从雾化器背面接入旋转动力，侧面可连接流体管道', 'Supply rotation at the rear and connect Fluid Pipes at the sides'),
        ('手持流体容器右击，也可存入或取出流体', 'Handheld fluid containers can also be emptied or filled by R-Clicking'),
        ('雾化器中的流体决定气流的加工效果', 'The stored fluid determines the processing effect of the air current'),
        ('红石信号会停止雾化，但气流仍会继续', 'A Redstone Signal stops atomizing without stopping the air current')],
    'fluid_switching': [
        ('切换流体管道的通路', 'Switching Fluid Pipe Connections'),
        ('银色接口是两条通路的公共接口', 'The silver connector is shared by both connections'),
        ('未接收红石信号时，公共口与对面的接口接通', 'Without a Redstone Signal, it connects to the opposite end'),
        ('接收红石信号后，公共口改为与侧面接口接通', 'A Redstone Signal switches it to the side connection'),
        ('流体的实际输送方向仍由动力泵决定', 'Mechanical Pumps still determine the direction of flow')],
    'fluid_collection': [
        ('收集流体与自动供水', 'Collecting Fluids and Supplying Water'),
        ('集液栅可收集接触其顶面的流体', 'Collection Grates collect fluids touching their top face'),
        ('收集到的流体会排入正下方的容器', 'Collected fluids drain into a container directly below'),
        ('智能集液栅可筛选流体，并使用红石信号暂停收集与排液', 'Smart Collection Grates can filter fluids; Redstone pauses collection and draining'),
        ('潮涌箱会自动向六面相邻的容器供水', 'Water Supply Boxes automatically supply water to adjacent containers on all six sides'),
        ('铜水槽可供手持容器和管道取水，但不会自动向外供水', 'Copper Water Basins supply handheld containers and pipes, but do not fill adjacent containers automatically')],
    'fluid_transfer': [
        ('使用流体传输器输送流体', 'Fluid Transportation using Fluid Transmitters'),
        ('流体传输器可在两端容器之间输送流体，无需旋转动力', 'Fluid Transmitters move fluids between end containers without rotational power'),
        ('机身箭头指示流体的输送方向', 'The arrow indicates the direction of flow'),
        ('使用扳手可以反转输送方向', 'A Wrench can be used to reverse the direction'),
        ('红石信号会暂停流体输送', 'A Redstone Signal pauses the transfer')],
    'booster_pumping': [
        ('使用增压泵传输流体', 'Fluid Transportation using Booster Pumps'),
        ('增压泵控制两端管道网络中的流体输送', 'Booster Pumps govern the flow of their attached pipe networks'),
        ('泵上的方向标记指示流体流向', 'The direction marker indicates the direction of flow'),
        ('使用扳手可以反转方向，旋转方向不影响流向', 'Use a Wrench to reverse the flow; input rotation direction does not affect it'),
        ('更强的泵送压力和更远的输送距离需要消耗更多应力', 'Greater pumping pressure and range come with a higher Stress Impact')],
}
for scene, lines in ponder.items():
    for index, values in enumerate(lines):
        put(f'ccd.ponder.{scene}.' + ('header' if index == 0 else f'text_{index}'), *values)

# Ponder also uses English literals as its fallback/generated language source.
# Keep those literals in lockstep with the translated storyboards.
import re
source = ROOT / 'src/main/java/dev/ccd/client/ponder/CCDPonderScenes.java'
scene = None
index = 0
output = []
for line in source.read_text(encoding='utf-8').splitlines(keepends=True):
    start = re.search(r'begin\(builder,"([^"]+)","([^"]+)"\)', line)
    if start:
        scene, index = start.group(1), 0
        line = line.replace(start.group(0), f'begin(builder,"{scene}",{json.dumps(ponder[scene][0][1], ensure_ascii=False)})')
    if scene and re.search(r'text\(s,util,.*?,"[^"]+"\)', line):
        index += 1
        line = re.sub(r'(text\(s,util,.*?,)"[^"]+"(\))',
                      lambda m: m.group(1) + json.dumps(ponder[scene][index][1], ensure_ascii=False) + m.group(2), line)
    output.append(line)
source.write_text(''.join(output), encoding='utf-8')

for locale, entries in data.items():
    (LANG / (locale + '.json')).write_text(json.dumps(entries, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('Revised bilingual tooltips, goggle labels and all 10 Ponder storyboards.')
