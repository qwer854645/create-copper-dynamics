package dev.ccd.logistics;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Pressure-only adapter. The real block is a capability endpoint, never a transparent pipe. */
public final class NativeEndpointPump extends PumpBlockEntity {
    public static final ThreadLocal<Boolean> UPDATING=ThreadLocal.withInitial(()->false);
    public static void changed(Level level,BlockPos pos){
        boolean old=UPDATING.get();UPDATING.set(true);
        try{com.simibubi.create.content.fluids.FluidPropagator.propagateChangedPipe(level,pos,level.getBlockState(pos));}
        finally{UPDATING.set(old);}
    }
    private Direction port;
    private boolean pulling;
    private float pressure;
    public int range(){
        var block=getBlockState().getBlock();
        if(block instanceof dev.ccd.content.BufferPumpBlock)return dev.ccd.CCDConfig.bufferPumpRange(com.simibubi.create.content.fluids.FluidPropagator.getPumpRange());
        if(block instanceof dev.ccd.content.CreativeDeliveryTankBlock)return 256;
        return dev.ccd.CCDConfig.boosterRange(com.simibubi.create.content.fluids.FluidPropagator.getPumpRange());
    }
    public NativeEndpointPump(Level level,BlockPos pos,BlockState state){
        super(level.getBlockEntity(pos).getType(),pos,state);setLevel(level);
    }
    @Override public float getSpeed(){return pressure;}
    @Override protected Direction getFront(){return port;}
    @Override protected boolean isFront(Direction side){return side==port;}
    @Override public boolean isPullingOnSide(boolean front){return pulling;}
    public void distribute(Direction side,boolean input,float pressure){
        this.port=side;this.pulling=input;this.pressure=pressure;
        if(pressure>0){
            boolean old=UPDATING.get();UPDATING.set(true);
            try{super.distributePressureTo(side);}finally{UPDATING.set(old);}
        }
    }
}
