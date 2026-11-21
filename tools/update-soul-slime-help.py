import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
for locale in ['zh_cn','en_us']:
    path=root/f'src/main/resources/assets/ccd/lang/{locale}.json'
    data=json.loads(path.read_text(encoding='utf-8'))
    for key,value in data.items():
        data[key]=value.replace('缠魂液','灵魂液') if locale=='zh_cn' else value.replace('Haunting Fluid','Soul Fluid')
    for fluid,zh,en in [
        ('slime','接触时施加_缓慢 II_，移动和跳跃受到黏滞阻力。','Applies _Slowness II_ and impedes movement and jumping on contact.'),
        ('haunting','接触时施加_缓慢_和_虚弱_。','Applies _Slowness_ and _Weakness_ on contact.')]:
        prefix=f'item.ccd.{fluid}_bucket.tooltip.'
        data[prefix+'condition2']='生物接触流体时' if locale=='zh_cn' else 'When entities touch the fluid'
        data[prefix+'behaviour2']=zh if locale=='zh_cn' else en
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
path=root/'src/main/java/dev/ccd/client/ponder/CCDPonderScenes.java'
text=path.read_text(encoding='utf-8').replace('Haunting Fluid','Soul Fluid')
path.write_text(text,encoding='utf-8')
