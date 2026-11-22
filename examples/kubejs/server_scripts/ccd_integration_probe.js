// Temporary integration fixture; remove after testing. Uses only existing fluids.
CCDAtomizer.itemHandler('ccd_probe:item', ctx => CCDAtomizer.output(Item.of('minecraft:clay_ball', ctx.getInput().getCount())))
CCDAtomizer.entityHandler('ccd_probe:entity', ctx => CCDAtomizer.effect('ccd:mossification', 80, 0))
CCDAtomizer.itemHandler('ccd_probe:error', ctx => { throw new Error('Intentional CCD integration probe error') })
CCDFluids.interaction('ccd_probe:pair', 'minecraft:lava', 'minecraft:water', 'minecraft:gold_block', null, null)
CCDFluids.blockInteraction('ccd_probe:block', 'minecraft:water', 'minecraft:glass', 'both', null, 'minecraft:diamond_block')
console.info('CCD_INTEGRATION_PROBE_REGISTERED')
