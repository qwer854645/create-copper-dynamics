// Copy into kubejs/server_scripts/. The optional startup example provides kubejs:brine.
// The plugin clears old callbacks before loading server scripts, so /reload replaces them cleanly.
CCDAtomizer.itemHandler('example:brine_clay', ctx => {
  // Called once for the whole completed input stack; return the intended total output count.
  return CCDAtomizer.output(Item.of('minecraft:clay_ball', ctx.getInput().getCount()))
})

CCDAtomizer.entityHandler('example:brine_effect', ctx => {
  return ctx.getHealth() < 10
    ? CCDAtomizer.effect('minecraft:regeneration', 60, 0)
    : CCDAtomizer.damage(0)
})

ServerEvents.recipes(event => {
  event.custom({
    type: 'ccd:atomizing',
    fluid: { fluid: 'kubejs:brine' },
    priority: 100,
    minimum_rpm: 16,
    consumption: { amount: 10, interval: 20 },
    processing_type: 'create:splashing', // Fallback for inputs not matched by the custom item rules.
    items: [
      {
        input: { item: 'minecraft:cobblestone' },
        results: [{ stack: { id: 'minecraft:clay_ball', count: 1 }, chance: 0.5 }],
        processing_time: 100
      },
      {
        input: { item: 'minecraft:dirt' },
        script: 'example:brine_clay',
        processing_time: 80
      }
    ],
    entity_script: 'example:brine_effect',
    entity_interval: 40,
    entity_types: ['minecraft:player']
  }).id('example:brine_atomizing')
})
