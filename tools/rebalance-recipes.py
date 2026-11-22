"""Authoritative survival recipe set. Does not regenerate artwork or processing outputs."""
from pathlib import Path
import json

root=Path(__file__).resolve().parents[1]/'src/main/resources/data/ccd/recipe'
def save(name,data):
    (root/(name+'.json')).write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def ingredient(id):return {'tag':id[1:]} if id.startswith('#') else {'item':id}
def shaped(id,pattern,key,count=1):
    save(id,{'type':'minecraft:crafting_shaped','category':'misc','pattern':pattern,'key':{k:ingredient(v) for k,v in key.items()},'result':{'id':'ccd:'+id,'count':count}})
def upgrade(id,*items):
    save(id,{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[ingredient(i) for i in items],'result':{'id':'ccd:'+id,'count':1}})
shaped('copper_bucket',['C C','C C',' C '],{'C':'create:copper_sheet'})
shaped('brass_bucket',['B B','BCB',' B '],{'B':'create:brass_sheet','C':'ccd:copper_bucket'})
shaped('faucet',[' CP',' L '],{'C':'create:copper_sheet','P':'create:fluid_pipe','L':'minecraft:lever'},2)
shaped('copper_water_basin',['C C','CTC',' C '],{'C':'create:copper_sheet','T':'create:fluid_tank'})
shaped('copper_collection_grate',['CBC','PHP','CDC'],{'C':'create:copper_sheet','B':'minecraft:iron_bars','P':'create:fluid_pipe','H':'create:hose_pulley','D':'create:item_drain'})
shaped('fluid_transmitter',[' C ','PHP',' C '],{'C':'create:copper_sheet','P':'create:fluid_pipe','H':'minecraft:hopper'})
shaped('fluid_access_port',[' P ','PCP',' P '],{'P':'create:fluid_pipe','C':'create:copper_casing'},2)
upgrade('fluid_hatch','create:item_hatch','create:fluid_pipe','create:copper_sheet')
upgrade('smart_faucet','ccd:faucet','create:brass_sheet','create:electron_tube','create:filter')
upgrade('smart_fluid_hatch','ccd:fluid_hatch','create:brass_sheet','create:electron_tube','create:filter')
upgrade('smart_collection_grate','ccd:copper_collection_grate','create:brass_casing','create:electron_tube','create:filter')
shaped('smart_fluid_tank',[' B ','FTF',' E '],{'B':'create:brass_casing','F':'create:filter','T':'create:fluid_tank','E':'create:electron_tube'})
upgrade('redstone_fluid_valve','create:fluid_valve','minecraft:repeater','create:electron_tube')
upgrade('three_way_valve','ccd:redstone_fluid_valve','create:fluid_pipe','create:fluid_pipe','create:copper_casing')
upgrade('flow_meter','create:speedometer','create:fluid_pipe','create:copper_sheet')
shaped('booster_pump',[' G ','CPC',' G '],{'G':'create:large_cogwheel','C':'create:copper_casing','P':'create:mechanical_pump'})
upgrade('bidirectional_fluid_pump','create:mechanical_pump','create:gearbox','create:copper_casing')
upgrade('redstone_fluid_pump','ccd:bidirectional_fluid_pump','minecraft:repeater','create:electron_tube')
upgrade('variable_flow_pump','ccd:bidirectional_fluid_pump','create:precision_mechanism','create:electron_tube','minecraft:comparator')
upgrade('portable_fluid_pump','create:mechanical_pump','create:hand_crank','create:belt_connector','create:copper_sheet')
upgrade('buffer_pump','create:mechanical_pump','create:fluid_tank','create:large_cogwheel','create:copper_sheet')
shaped('omni_pump',[' P ','GBG',' M '],{'P':'create:fluid_pipe','G':'create:gearbox','B':'ccd:buffer_pump','M':'create:precision_mechanism'})
shaped('fluid_atomizer',[' S ','CFC',' MT'],{'S':'create:spout','C':'create:copper_sheet','F':'create:encased_fan','M':'create:precision_mechanism','T':'create:fluid_tank'})
upgrade('fluid_arm','create:mechanical_arm','create:fluid_pipe','create:fluid_pipe','create:spout')
shaped('fluid_packager',[' C ','PST',' C '],{'C':'create:copper_sheet','P':'create:packager','S':'create:spout','T':'create:fluid_tank'})
upgrade('fluid_repackager','create:repackager','create:fluid_tank','create:electron_tube','create:fluid_pipe')
upgrade('fluid_gauge','create:factory_gauge','create:fluid_pipe','create:electron_tube')
shaped('fluid_bridge',[' E ','PMP',' B '],{'E':'create:electron_tube','P':'ccd:fluid_access_port','M':'create:precision_mechanism','B':'create:brass_casing'})
upgrade('fluid_filter','create:filter','create:fluid_pipe')
upgrade('fluid_attribute_filter','create:attribute_filter','create:fluid_pipe')
# Expensive infinite-water convenience, still a survival device rather than a creative tank.
save('copper_water_supply',{'type':'create:mechanical_crafting','accept_mirrored':False,'pattern':[' CBC ','BPTPB','TWSWT','BPTPB',' CBC '],
 'key':{k:ingredient(v) for k,v in {'C':'create:copper_sheet','B':'create:brass_casing','P':'create:precision_mechanism','T':'create:electron_tube','W':'create:mechanical_pump','S':'ccd:copper_water_basin'}.items()},'result':{'id':'ccd:copper_water_supply','count':1}})
# Stonecutting gives explicit choices instead of conflicting shapeless recipes.
for name,source,target in [
 ('turning_booster_pump','booster_pump','turning_booster_pump'),
 ('booster_pump_from_turning','turning_booster_pump','booster_pump'),
 ('vertical_turning_booster_pump_from_turning_booster_pump','turning_booster_pump','vertical_turning_booster_pump'),
 ('turning_booster_pump_from_vertical_turning_booster_pump','vertical_turning_booster_pump','turning_booster_pump'),
 ('horizontal_smart_fluid_tank_conversion','smart_fluid_tank','horizontal_smart_fluid_tank'),
 ('smart_fluid_tank_conversion','horizontal_smart_fluid_tank','smart_fluid_tank')]:
    save(name,{'type':'minecraft:stonecutting','ingredient':{'item':'ccd:'+source},'result':{'id':'ccd:'+target,'count':1}})
def fluid(id,amount):return {'type':'neoforge:single','fluid':id,'amount':amount}
def mix(id,ingredients,amount,time=200,heat=None):
    p=root/'mixing'/f'{id}.json';old=json.loads(p.read_text())
    data={'type':'create:mixing','ingredients':ingredients,'results':[{'id':'ccd:'+('powder_snow' if id.startswith('powder_snow_') else id),'amount':amount}],'processing_time':time}
    if 'neoforge:conditions' in old:data['neoforge:conditions']=old['neoforge:conditions']
    if heat:data['heat_requirement']=heat
    save('mixing/'+id,data)
def items(*ids):return [ingredient(id) for id in ids]
mix('quicksand',[fluid('minecraft:water',250),*items('#minecraft:sand','#minecraft:sand')],1000,100)
mix('slime',items('minecraft:slime_ball'),250,100)
mix('powder_snow_from_snow_block',items('minecraft:snow_block'),1000,100)
mix('powder_snow_from_snowballs',items(*(['minecraft:snowball']*4)),1000,100)
mix('haunting',[fluid('minecraft:water',500),*items('minecraft:soul_sand','minecraft:bone_meal')],500,200,'heated')
# Consumes a blaze cake, preventing superheated fuel from cheaply reproducing itself.
mix('seething_reagent',[fluid('minecraft:lava',1000),*items('create:blaze_cake','minecraft:magma_cream')],500,400,'superheated')
mix('enriching_reagent',[fluid('minecraft:water',500),*items('#c:dusts/uranium','minecraft:redstone')],500,300,'heated')
mix('soul_stripping_reagent',[fluid('ccd:haunting',500),*items('minecraft:soul_soil','minecraft:amethyst_shard')],500,300,'heated')
mix('explosive_slurry',[fluid('minecraft:water',500),*items('minecraft:gunpowder','minecraft:gunpowder','minecraft:redstone')],500,200)
mix('resonant_solution',[fluid('minecraft:water',500),*items('minecraft:amethyst_shard','minecraft:amethyst_shard','minecraft:quartz')],500,200)
mix('sculk_suspension',[fluid('minecraft:water',500),*items('minecraft:sculk','minecraft:sculk','minecraft:bone_meal')],500,200)
mix('purifying_solution',[fluid('minecraft:water',250),*items('minecraft:ghast_tear','minecraft:glowstone_dust','minecraft:glowstone_dust')],250,300,'heated')
mix('withering_fluid',[fluid('minecraft:water',500),*items('minecraft:wither_rose','minecraft:fermented_spider_eye')],500,200)
mix('conduit_energy_fluid',[fluid('minecraft:water',500),*items('minecraft:nautilus_shell','minecraft:prismarine_crystals','minecraft:prismarine_crystals')],500,300)
mix('moss_suspension',[fluid('minecraft:water',500),*items('minecraft:moss_block','minecraft:bone_meal')],500,200)
mix('tear_solution',[fluid('minecraft:water',500),*items('minecraft:crying_obsidian','minecraft:fermented_spider_eye')],500,200)
mix('corrupting_fluid',[fluid('ccd:withering_fluid',250),fluid('ccd:sculk_suspension',250),*items('minecraft:echo_shard','minecraft:fermented_spider_eye')],500,400,'heated')
print('Rebuilt all survival recipes and 1:1 conversions; preserved optional-mod conditions.')
