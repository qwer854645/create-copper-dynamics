"""Consistent readout terminology; no instructions or whole-row number coloring."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ccd/lang'
rows={
'ccd.flow_meter.rate':('流量：%s mB/t','Flow Rate: %s mB/t'),
'ccd.reciprocating.flow':('目标流量：%s mB/t','Target Flow Rate: %s mB/t'),
'ccd.reciprocating.range':('单侧范围：%s 格','Range per Side: %s blocks'),
'ccd.reciprocating.signal':('信号强度：%s / 15','Signal Strength: %s / 15'),
'ccd.omni_pump.face':('%s：%s','%s: %s'),
'ccd.faucet.open':('开启','Open'),'ccd.faucet.closed':('关闭','Closed'),'ccd.faucet.paused':('已暂停','Paused'),
'ccd.valve.open':('开启','Open'),'ccd.valve.closed':('关闭','Closed'),
'ccd.transmitter.ready':('就绪','Ready'),'ccd.transmitter.paused':('已暂停','Paused'),
'ccd.water_supply.active':('供水中','Supplying Water'),'ccd.water_supply.paused':('已暂停','Paused'),
'ccd.smart_collection_grate.active':('收集中','Collecting'),'ccd.smart_collection_grate.paused':('已暂停','Paused'),
'ccd.atomizer.active':('雾化中','Atomizing'),'ccd.atomizer.idle':('空闲','Idle'),
'ccd.fluid_arm.status.stopped':('未接入旋转力','No Rotational Power'),
'ccd.fluid_arm.status.redstone':('已暂停取液','Intake Paused'),
'ccd.fluid_arm.status.collecting':('取液中','Collecting'),
'ccd.fluid_arm.status.delivering':('注液中','Delivering'),
'ccd.fluid_arm.status.waiting_input':('等待流体','Waiting for Fluid'),
'ccd.fluid_arm.status.waiting_output':('等待接收容器','Waiting for Output'),
}
for locale in ['zh_cn','en_us']:
    path=root/(locale+'.json');data=json.loads(path.read_text(encoding='utf-8'))
    for key,(zh,en) in rows.items():data[key]=zh if locale=='zh_cn' else en
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
