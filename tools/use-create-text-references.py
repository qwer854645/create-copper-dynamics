"""Prefer shipped Create wording; only adapt subjects and CCD-specific differences."""
import json
from pathlib import Path
from zipfile import ZipFile

ROOT=Path(__file__).resolve().parents[1]
jar=next((Path.home()/'.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280').rglob('*-slim.jar'))
references={
 'smart_tanks.header':'fluid_tank_sizes.header',
 'smart_tanks.text_1':'fluid_tank_sizes.text_1',
 'smart_tanks.text_5':'fluid_tank_sizes.text_4',
 'fluid_packaging.text_1':'packager.text_1',
 'fluid_packaging.text_2':'packager.text_2',
 'fluid_packaging.text_3':'packager.text_3',
 'fluid_packaging.text_4':'packager.text_4',
 'booster_pumping.header':'mechanical_pump_flow.header',
 'booster_pumping.text_1':'mechanical_pump_flow.text_1',
 'booster_pumping.text_2':'mechanical_pump_flow.text_2',
 'booster_pumping.text_3':'mechanical_pump_flow.text_6',
 'fluid_hatches.text_4':'mechanical_arm.text_1',
}
with ZipFile(jar) as archive:
 for locale in ('zh_cn','en_us'):
    original=json.loads(archive.read(f'assets/create/lang/{locale}.json'))
    path=ROOT/f'src/main/resources/assets/ccd/lang/{locale}.json'
    data=json.loads(path.read_text(encoding='utf-8'))
    for target,source in references.items():
        value=original['create.ponder.'+source]
        if locale=='zh_cn':
            for before,after in [('流体储罐','智能储罐'),('动力泵','增压泵'),('动力臂','动力移液臂'),('打包机','流体打包机'),('库存中的物品','容器中的流体'),('内容物','流体'),('目标库存','目标容器'),('库存中','容器中')]:
                value=value.replace(before,after)
        else:
            for before,after in [('Mechanical Pumps','Booster Pumps'),('Mechanical Pump','Booster Pump'),('Fluid tank','Smart Fluid Tank'),('Fluid Tanks','Smart Fluid Tanks'),('Mechanical Arm','Mechanical Fluid Arm'),('Packagers','Fluid Packagers'),('Packager','Fluid Packager'),('items of the inventory','fluids of the container')]:
                value=value.replace(before,after)
            value=value.replace('Fluid Fluid Packager','Fluid Packager')
            value=value.replace('packagers','fluid packagers').replace('the inventory they should target','the fluid container they should target')
            value=value.replace('items from the inventory','fluids from the container').replace('the contents into the inventory','the fluids into the container')
        data['ccd.ponder.'+target]=value
    edits={
      'ccd.ponder.booster_pumping.text_3':('可以使用扳手来反转方向','A Wrench can be used to reverse the direction'),
      'ccd.ponder.smart_tanks.text_2':('储罐的横截面可为 1×1、2×2 或 3×3','Their cross-section can be 1×1, 2×2 or 3×3 blocks'),
      'ccd.ponder.smart_tanks.text_3':('横向智能储罐可以沿水平方向多格连接','Horizontal Smart Fluid Tanks can be extended horizontally'),
      'ccd.ponder.smart_tanks.text_4':('同一组储罐可以同时储存多种流体','A single tank structure can store multiple fluids'),
      'ccd.ponder.fluid_hatches.text_3':('智能流体舱口可以限制通过它的流体种类','Smart Fluid Hatches can restrict which fluids may pass'),
      'ccd.ponder.fluid_atomizing.text_3':('气流经过雾化器时，可以进行相应的批量加工','Air currents passing through the Atomizer can process items in bulk'),
      'ccd.ponder.fluid_access.text_4':('设置过滤器，可为两端容器分配不同的流体','Use filters to assign different fluids to each container'),
    }
    summaries={
      'smart_fluid_tank':('可多格连接的_流体容器_，能同时储存_多种流体_。','A _Fluid Container_ that can be expanded and stores _Multiple Fluids_.'),
      'horizontal_smart_fluid_tank':('可水平连接的_智能储罐_。','A _Smart Fluid Tank_ that expands horizontally.'),
      'fluid_hatch':('将手持容器中的流体_存入_其所附着的容器中，也可_取出_流体。','_Empties_ your held fluid container into the attached container, or _Fills_ it.'),
      'smart_fluid_hatch':('限制通过舱口的_流体种类_。','Restricts the _Fluids_ that can pass through the hatch.'),
      'fluid_packager':('将容器中的流体_打包_，或将包裹_拆解_到容器中。','_Packages_ fluids from a container, or _Unpacks_ them into it.'),
      'fluid_arm':('在选定的输入端和输出端之间_搬运流体_。','_Moves Fluids_ between selected inputs and outputs.'),
      'booster_pump':('为管道网络提供更强的_泵送压力_和更远的_输送距离_。','Provides greater _Pressure_ and _Range_ for fluid pipe networks.'),
      'fluid_gauge':('监控流体_存量_，并请求_补货_。','Monitors fluid _Stock_ and requests _Restocking_.'),
      'fluid_atomizer':('使用流体进行_批量加工_。','Uses fluids for _Bulk Processing_.'),
    }
    for name,values in summaries.items(): edits[f'block.ccd.{name}.tooltip.summary']=values
    # Keep secondary details out of the basic purpose/controls.
    for name in ('smart_fluid_tank','horizontal_smart_fluid_tank'):
        edits[f'block.ccd.{name}.tooltip.action1']=('打开或关闭_观察窗_。','Toggles the _Windows_.')
    edits['block.ccd.fluid_arm.tooltip.control1']=('放置前右击容器时','When R-Clicked on a Container Before Placement')
    edits['block.ccd.fluid_arm.tooltip.action1']=('将其指定为_输入端_或_输出端_；潜行右击移除。','Assigns it as an _Input_ or _Output_. Sneak-click to remove it.')
    for key,values in edits.items(): data[key]=values[0 if locale=='zh_cn' else 1]
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

# Keep English Ponder literals identical to the current language entries.
import re
english=json.loads((ROOT/'src/main/resources/assets/ccd/lang/en_us.json').read_text(encoding='utf-8'))
source=ROOT/'src/main/java/dev/ccd/client/ponder/CCDPonderScenes.java'
scene=None; index=0; lines=[]
for line in source.read_text(encoding='utf-8').splitlines(keepends=True):
    match=re.search(r'begin\(builder,"([^"]+)","([^"]+)"\)',line)
    if match:
        scene=match.group(1);index=0
        line=line.replace(match.group(0),f'begin(builder,"{scene}",{json.dumps(english[f"ccd.ponder.{scene}.header"],ensure_ascii=False)})')
    if scene and re.search(r'text\(s,util,.*?,"[^"]+"\)',line):
        index+=1
        line=re.sub(r'(text\(s,util,.*?,)"[^"]+"(\))',lambda m:m[1]+json.dumps(english[f'ccd.ponder.{scene}.text_{index}'],ensure_ascii=False)+m[2],line)
    lines.append(line)
source.write_text(''.join(lines),encoding='utf-8')
