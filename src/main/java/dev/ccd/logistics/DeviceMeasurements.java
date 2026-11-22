package dev.ccd.logistics;

/** Opt-in server instrumentation; sync requests are NOT actual network packets. */
public final class DeviceMeasurements {
    private DeviceMeasurements(){}
    public static final boolean ENABLED=Boolean.getBoolean("ccd.deviceMeasurements");
    public static final class Sample { public long ticks,nanos,syncRequests; }
    private static final java.util.Map<String,Sample> samples=new java.util.TreeMap<>();
    private static final java.util.Set<net.minecraft.world.level.block.entity.BlockEntity> targets=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    public static synchronized void watch(java.util.Collection<? extends net.minecraft.world.level.block.entity.BlockEntity> entities){samples.clear();targets.clear();targets.addAll(entities);}
    public static synchronized boolean watching(net.minecraft.world.level.block.entity.BlockEntity be){return targets.contains(be);}
    public static synchronized void stop(){targets.clear();}
    public static synchronized void tick(String type,long nanos){var s=samples.computeIfAbsent(type,k->new Sample());s.ticks++;s.nanos+=nanos;}
    public static synchronized void sync(String type){samples.computeIfAbsent(type,k->new Sample()).syncRequests++;}
    public static synchronized void clear(){samples.clear();}
    public static synchronized String report(){
        return samples.entrySet().stream().map(e->e.getKey()+": ticks="+e.getValue().ticks+", cpu_ns="+e.getValue().nanos+", sync_requests="+e.getValue().syncRequests).collect(java.util.stream.Collectors.joining("; "));
    }
}
