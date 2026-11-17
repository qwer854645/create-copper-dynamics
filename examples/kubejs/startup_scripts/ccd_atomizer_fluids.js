// Optional example. Copy into kubejs/startup_scripts/ and restart to register this new fluid.
StartupEvents.registry('fluid', event => {
  event.create('brine').displayName('Brine')
})
