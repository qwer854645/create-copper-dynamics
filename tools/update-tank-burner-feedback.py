from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd/lang'
for loc in ['zh_cn','en_us']:
    p=root/f'{loc}.json';d=json.loads(p.read_text(encoding='utf-8'));zh=loc=='zh_cn'
    d.update({'ccd.tank_burner.heating':'燃烧室状态：' if zh else 'Burner Status:',
        'ccd.tank_burner.heat':'热量：%s' if zh else 'Heat: %s',
        'ccd.tank_burner.remaining':'剩余燃烧时间：%s 秒' if zh else 'Remaining burn time: %s s'})
    for key,a,b in [('none','无烈焰人','Empty'),('smouldering','待燃','Smouldering'),('fading','余热','Fading'),('kindled','燃烧','Heated'),('seething','超级燃烧','Superheated')]:
        d['ccd.tank_burner.heat.'+key]=a if zh else b
    p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
