"""Maintain paired Create-style player help without changing processing rules."""
from pathlib import Path
import json,re
root=Path(__file__).resolve().parents[1]
updates={
'block.ccd.fluid_packager.tooltip.condition1':('送入黄铜包裹且后方连接物品容器时','When Receiving a Brass Package with an Item Container Behind'),
'block.ccd.fluid_packager.tooltip.behaviour1':('将_物品存入容器_，流体分装为每包最多_4000 mB_的铜包裹输出，并保留收件地址。','Stores _items in the container_ and outputs fluids in _Copper Packages_ of up to _4000 mB_, retaining the address.'),
'block.ccd.fluid_packager.tooltip.condition2':('送入包裹且后方仅连接流体容器时','When Receiving a Package with Only a Fluid Container Behind'),
'block.ccd.fluid_packager.tooltip.behaviour2':('将包裹中的_流体注入容器_；空间不足时等待。','Empties the package’s _fluid into the container_; waits if there is not enough space.'),
'block.ccd.fluid_packager.tooltip.control2':('手持装液容器右键点击时','When R-Clicked with a Filled Container'),
'block.ccd.fluid_packager.tooltip.action2':('选择红石打包的_流体_。','Selects the _fluid_ for Redstone-triggered packaging.'),
'block.ccd.fluid_arm.tooltip.control1':('手持动力移液臂右键点击已放置的移液臂时','When R-Clicked with a Fluid Arm'),
'block.ccd.fluid_arm.tooltip.action1':('应用_预选目标_；潜行右键点击可清空目标。','Applies the _selected targets_; Sneak-R-Click to clear them.'),
'block.ccd.fluid_arm.tooltip.control2':('空手右键点击携带流体的移液臂时','When R-Clicked with an Empty Hand While Carrying Fluid'),
'block.ccd.fluid_arm.tooltip.action2':('将携带的流体以_铜包裹_交还。','Returns the carried fluid as a _Copper Package_.'),
'block.ccd.fluid_hatch.tooltip.summary':('为手持容器和_动力移液臂_提供背后容器的流体存取接口。','Provides fluid access to the container behind it for handheld containers and _Fluid Arms_.'),
'block.ccd.tank_blaze_burner.tooltip.summary':('内置_4000 mB 储罐_的烈焰人燃烧室，也可使用_固体燃料_。','A Blaze Burner with a _4000 mB tank_. Also accepts _solid fuels_.'),
'block.ccd.tank_blaze_burner.tooltip.behaviour1':('储存一种液体燃料，通常每_1000 mB_按对应桶燃料供燃。Liquid Fuel 燃料使用其设定的耗量和燃烧时间。','Stores one liquid fuel, normally burning _1000 mB_ as its bucket fuel. Liquid Fuel recipes use their own consumption and burn time.'),
'block.ccd.omni_pump.tooltip.behaviour2':('输出受阻时_停止抽液_，泵内不储液。','_Stops extraction_ when outputs are blocked. Stores no fluid.'),
'block.ccd.variable_flow_pump.tooltip.control1':('在机身露出的齿轮面连接齿轮时','When Meshed with the Exposed Cogwheel'),
'ccd.ponder.pump_variants.text_6':('调频动力泵根据信号强度调整流量和距离，齿轮连接机身露出的齿轮面','Variable Flow Pumps use Redstone strength to adjust flow and range; mesh Cogwheels with the exposed gear'),
'ccd.status.ready':('就绪','Ready'),
'ccd.status.recovery':('等待交付剩余货物','Waiting to Deliver Remaining Cargo'),
'ccd.packager.recovery_amount':('待交付：%s，%s','Awaiting Delivery: %s, %s'),
'ccd.packager.recovery_items':('待交付物品：%s × %s','Awaiting Delivery: %s × %s'),
'item.ccd.composite_package.tooltip.condition2':('送入后方连接物品容器的流体打包机时','When Sent to a Fluid Packager with an Item Container Behind'),
'item.ccd.composite_package.tooltip.behaviour2':('物品存入容器，流体拆为保留地址的_铜包裹_输出。','Stores items and outputs addressed _Copper Packages_ containing the fluids.'),
'item.ccd.smoking_fluid_bucket.tooltip.summary':('用于_批量烟熏_的流体。','A fluid for _Bulk Smoking_.'),
'item.ccd.smoking_fluid_bucket.tooltip.condition1':('气流经过熏烤液或装入流体雾化器时','When Air Passes Through the Fluid or It Is Supplied to an Atomizer'),
'item.ccd.smoking_fluid_bucket.tooltip.behaviour1':('使用_烟熏配方_批量加工物品。','Processes items in bulk using _Smoking Recipes_.'),
'ccd.filter.value.hot':('高温','Hot'),'ccd.filter.value.normal':('常温','Normal'),
'ccd.filter.value.gas':('气体','Gas'),'ccd.filter.value.liquid':('液体','Liquid'),
'ccd.filter.value.lit':('发光','Luminous'),'ccd.filter.value.dark':('不发光','Non-luminous'),
'item.ccd.fluid_filter.tooltip.behaviour1':('同时匹配流体种类和_附加数据_，例如药水效果。','Also matches _additional data_, such as potion effects.'),
'ccd.filter.exact':('匹配附加数据','Match Additional Data'),
'ccd.ponder.fluid_hatches.text_4':('智能舱口仅允许匹配过滤条件的流体通过','Smart Hatches allow only fluids matching their filter'),
'ccd.ponder.fluid_hatches.text_5':('动力移液臂也可通过舱口访问背后的容器','Fluid Arms can also access the container through its Hatch'),
'ccd.ponder.fluid_packaging.text_3':('铜包裹可通过传送带和包裹物流运输','Copper Packages can travel on Belts and package logistics'),
'ccd.ponder.fluid_packaging.text_6':('送入黄铜包裹时，物品存入后方物品容器，流体分装为铜包裹输出','Brass Packages deposit their items into the rear item container and output their fluids in Copper Packages'),
'ccd.ponder.fluid_repackaging.text_4':('将同一订单的所有包裹汇集到理包机后方的物品容器','Collect all packages of the same order in the item container behind the Repackager'),
'ccd.ponder.fluid_arm_setup.text_3':('放置移液臂并接入旋转力，在选定位置间搬运流体','Place the configured Arm and supply rotational power to transfer fluid between the selected locations'),
'ccd.ponder.tank_burner.text_2':('通常每 1000 mB 按桶燃料供燃；Liquid Fuel 燃料使用其自身耗量和燃烧时间','Normally burns 1000 mB as bucket fuel; Liquid Fuel recipes use their own consumption and burn time'),
}
for index,locale in enumerate(['zh_cn','en_us']):
    p=root/f'src/main/resources/assets/ccd/lang/{locale}.json'
    data=json.loads(p.read_text(encoding='utf-8-sig'))
    for prefix,old,new in [('block.ccd.fluid_arm',3,2),('block.ccd.smart_collection_grate',2,1)]:
        for kind in ['condition','behaviour']:
            key=f'{prefix}.tooltip.{kind}{old}'
            if key in data:data[f'{prefix}.tooltip.{kind}{new}']=data.pop(key)
    for key,values in updates.items():data[key]=values[index]
    if index==0:
        for key,value in data.items():
            data[key]=value.replace('流体重打包机','流体理包机').replace('重打包机','理包机').replace('库存链接器','存量转信器').replace('流体机械臂','动力移液臂').replace('旋转动力','旋转力')
        data['ccd.ponder.fluid_hatches.text_4']=updates['ccd.ponder.fluid_hatches.text_4'][0]
    for key in list(data):
        if key.startswith('ccd.configuration.') and key.endswith('.tooltip'):
            data[key]=data[key].replace(' 重新进入世界后生效。','').replace(' Reopen the world to apply changes.','')
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
