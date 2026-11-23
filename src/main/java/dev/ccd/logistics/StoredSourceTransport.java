package dev.ccd.logistics;

import com.simibubi.create.content.fluids.*;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.ICapabilityProvider;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Internal source mouth feeds the native flow/network lifecycle, never pulls a neighbour. */
public final class StoredSourceTransport extends FluidTransportBehaviour {
    private final java.util.function.Supplier<Direction> outlet;
    private final java.util.function.Supplier<IFluidHandler> storage;
    private final java.util.function.Supplier<Float> pressure;
    private Direction configured;
    private float lastPressure=Float.NaN;
    public StoredSourceTransport(SmartBlockEntity owner,java.util.function.Supplier<Direction> outlet,java.util.function.Supplier<IFluidHandler> storage,java.util.function.Supplier<Float> pressure){super(owner);this.outlet=outlet;this.storage=storage;this.pressure=pressure;}
    @Override public boolean canHaveFlowToward(BlockState state,Direction side){return side==outlet.get();}
    private void configure(){
        var out=outlet.get();if(configured==out&&interfaces!=null)return;
        configured=out;lastPressure=Float.NaN;interfaces=new java.util.EnumMap<>(Direction.class);
        interfaces.put(out,new PipeConnection(out));
        var inside=out.getOpposite();
        interfaces.put(inside,new PipeConnection(inside){
            @Override public boolean determineSource(Level level,BlockPos pos){
                ((dev.ccd.mixin.PipeConnectionSourceAccessor)this).ccd$source(java.util.Optional.of(new FlowSource(new BlockFace(pos,inside)){
                    public boolean isEndpoint(){return true;}
                    @Override public FluidStack provideFluid(java.util.function.Predicate<FluidStack> filter){var fluid=storage.get().drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.SIMULATE);return filter.test(fluid)?fluid:FluidStack.EMPTY;}
                    @Override public ICapabilityProvider<IFluidHandler> provideHandler(){return ICapabilityProvider.of(storage);}
                }));return true;
            }
        });
    }
    @Override public void wipePressure(){configured=null;interfaces=null;super.wipePressure();configured=null;}
    public boolean directNative(){
        var level=blockEntity.getLevel();var pos=blockEntity.getBlockPos().relative(outlet.get());
        return level!=null&&level.hasChunkAt(pos)&&!(level.getBlockEntity(pos) instanceof dev.ccd.content.OmniPumpBlockEntity)
            &&com.simibubi.create.content.fluids.FluidPropagator.getPipe(level,pos)==null;
    }
    @Override public void tick(){
        if(!dev.ccd.CCDFeatures.enabled(blockEntity.getBlockState()))return;
        if(dev.ccd.compat.pnp.PnPCompat.active()&&!directNative())return;
        configure();float speed=pressure.get();
        if(speed!=lastPressure)for(var entry:interfaces.entrySet()){
            entry.getValue().wipePressure();entry.getValue().addPressure(entry.getKey()!=configured,speed);
        }
        lastPressure=speed;
        super.tick();
    }
}
