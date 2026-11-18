"""Build small original baseplates and bilingual scene text from storyboard order."""
import json,re
from pathlib import Path
import nbtlib
from nbtlib import Compound,List,Int,String
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'src/main/resources'
source=(ROOT/'src/main/java/dev/ccd/client/ponder/CCDPonderScenes.java').read_text(encoding='utf-8')
translations={
'Supply rotation after placing the arm to move fluid between the selected points':'放置后接入动力，在所选输入和输出之间搬运流体',
'Collecting and supplying fluids':'收集与供应流体',
'Collection Grates collect fluid touching their top face':'集液栅收集接触其顶面的流体',
'Collected fluid drains into a container directly below':'收集的流体会排入正下方的容器',
'Smart Collection Grates add a fluid filter and redstone control':'智能集液栅可设置流体过滤器，并使用红石控制',
'Conduit Boxes supply water to containers touching any of their six faces':'潮涌箱向六面相邻的容器供应水',
'Copper Water Basins provide water for containers and pipes without automatic supply':'铜水槽可为容器和管道提供水，但不会主动补水',
'Transferring between containers':'在容器之间输送流体',
'Fluid Transmitters move fluid between adjacent containers without rotational power':'流体传输器无需旋转动力，即可在相邻容器之间输送流体',
'The housing arrows point towards the receiving container':'机身箭头指向接收流体的容器',
'A Wrench reverses the transfer direction':'使用扳手可反转输送方向',
'A redstone signal pauses transfer':'红石信号使输送暂停',
'Transporting fluids with Booster Pumps':'使用增压泵输送流体',
'Booster Pumps drive fluid through attached pipe networks':'增压泵使流体沿连接的管网输送',
'Their direction marks indicate the flow towards the output':'方向标记指向流体输出的一端',
'Use a Wrench to reverse the pumping direction':'使用扳手可反转泵送方向',
'Greater pressure and range come with a higher Stress Impact':'更高的压力和更远的输送距离需要更大的应力影响',
'Packaging and unpacking fluids':'流体的打包与解包',
'Attach the rear of a Fluid Packager to a fluid container':'将流体打包机背面连接到流体容器',
'A redstone signal packs up to 4000 mB into a Copper Package':'红石信号使打包机将最多 4000 mB 流体装入铜包裹',
'Packages use the same addresses and transport as ordinary packages':'铜包裹使用与普通包裹相同的地址和运输方式',
'Arriving Copper Packages empty into the container behind':'收到的铜包裹会将流体存入背后的容器',
'Use a filled container to set the filter for redstone packaging':'使用装有流体的容器设置红石打包的过滤条件',
'A Repackager combines items and fluids from the same order into Brass Packages':'理包机可将同一订单的物品和流体合并为黄铜包裹',
'Accessing fluids through hatches':'通过流体舱口存取流体',
'Fluid Hatches provide access to the container behind':'流体舱口用于访问其背后的流体容器',
'Right-click the exposed front with a fluid container to insert or extract fluid':'手持流体容器右击外露正面，即可存入或取出流体',
'Smart Fluid Hatches only allow the selected fluid to pass':'智能流体舱口只允许所选流体进出',
'Before placing an arm, select hatch fronts as fluid inputs and outputs':'放置动力移液臂前，选择舱口正面作为输入和输出',
'Extending and combining fluid access':'扩展与合并流体存取',
'An Access Port exposes the attached container on its other five faces':'流体扩展端口的其余五面可访问所连接的容器',
'Access Ports cannot extend other Access Ports':'流体扩展端口不能连接另一个扩展端口',
'A Fluid Bridge gives side connections access to both end containers':'从流体桥接器侧面可同时访问两端的容器',
'Each end can filter which fluids enter or leave its container':'两端可以分别过滤其容器进出的流体',
'Combining Smart Fluid Tanks':'组合智能储罐',
'Smart Fluid Tanks combine into structures with shared capacity':'智能储罐可组合为共享容量的多方块结构',
'The square cross-section can be one, two or three blocks wide':'正方形截面可为一格、两格或三格宽',
'Horizontal Smart Fluid Tanks extend along their placement axis':'横向智能储罐沿放置朝向延伸',
"Different fluids remain separate while sharing the structure's capacity":'不同流体分别保存，并共用整组储罐的容量',
'Right-click with a Wrench to toggle the windows':'使用扳手右击可切换观察窗',
'Pouring and filling with faucets':'使用龙头倾倒流体',
'Attach a faucet to the side of a fluid container':'将龙头连接到流体容器侧面',
'Open the faucet to fill the container directly below':'开启龙头，为正下方的容器注液',
'Smart Faucets fill items on a Belt or Depot two blocks below':'智能龙头可以为下方两格的传送带或置物台上的物品注液',
'Processing with fluid spray':'使用流体雾化器加工',
'Supply rotation at the rear and fluid through the sides':'从背面接入动力，从侧面输入流体',
'Fluid containers can also fill or empty the Atomizer by hand':'也可使用手持流体容器存入或取出流体',
'The stored fluid gives the air current its processing effect':'储存的流体使气流获得对应的加工效果',
'Redstone stops the spray without stopping the air current':'红石信号只停止雾化，不会停止气流',
'Switching fluid routes':'切换流体通路',
'The silver port is shared by both routes':'银色接口为两条通路共用的接口',
'Without redstone, fluid can pass through the straight route':'没有红石信号时，公共口与直通口接通',
'Redstone switches the common port to the side route':'接收红石信号后，公共口改为接通侧口',
'Pumps still determine the direction of flow':'流体的输送方向仍由动力泵决定',
}
for locale in ['zh_cn','en_us']:
    path=R/f'assets/ccd/lang/{locale}.json'; lang=json.loads(path.read_text(encoding='utf-8'))
    for key in list(lang):
        if key.startswith('ccd.ponder.'): del lang[key]
    for match in re.finditer(r'var s=begin\(builder,"([^"]+)","([^"]+)"\);(.*?)(?=\n    public static void|\n\})',source,re.S):
        id,title,body=match.groups()
        for suffix,text in [('header',title)]+[(f'text_{i}',text) for i,text in enumerate(re.findall(r'text\(s,util,.*?,"([^"]+)"\)',body),1)]:
            lang[f'ccd.ponder.{id}.{suffix}']=translations[text] if locale=='zh_cn' else text
    path.write_text(json.dumps(lang,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for name in ['packaging','hatches','access','tanks','faucets','atomizing','valves','collection','transfer','pumping']:
    blocks=[Compound({'pos':List[Int]([x,y,z]),'state':Int(0 if y==0 else 1)}) for x in range(5) for z in range(5) for y in range(5)]
    root=Compound({'DataVersion':Int(3955),'size':List[Int]([5,5,5]),
        'palette':List[Compound]([Compound({'Name':String('minecraft:andesite')}),Compound({'Name':String('minecraft:air')})]),
        'blocks':List[Compound](blocks),'entities':List[Compound]([])})
    path=R/f'assets/ccd/ponder/{name}.nbt'; path.parent.mkdir(parents=True,exist_ok=True)
    nbtlib.File(root,gzipped=True).save(path)
print('Generated 10 original scene templates and bilingual Ponder text')
