"""Remove per-tick endpoint BFS; use native pressure propagation and capability endpoints."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/java/dev/ccd/content'
p=root/'BufferPumpBlockEntity.java';s=p.read_text();a=s.index('    private record Step');b=s.index('    @Override protected void write',a)
s=s[:a]+'''    private boolean pressureDirty=true;
    public void invalidatePressure(){pressureDirty=true;}
    @Override public void onSpeedChanged(float previous){super.onSpeedChanged(previous);invalidatePressure();}
    @Override public void tick(){
        super.tick();if(level==null||level.isClientSide()||!pressureDirty)return;
        pressureDirty=false;
        var port=getBlockState().getValue(BufferPumpBlock.FACING);
        var neighbor=worldPosition.relative(port);
        com.simibubi.create.content.fluids.FluidPropagator.propagateChangedPipe(level,neighbor,level.getBlockState(neighbor));
        new dev.ccd.logistics.NativeEndpointPump(level,worldPosition,getBlockState()).distribute(port,false,isOverStressed()?0:Math.abs(getSpeed()));
    }
'''+s[b:];s=s.replace('public void updatePipesOnSide(Direction side){}','public void updatePipesOnSide(Direction side){invalidatePressure();}').replace('public void updatePressureChange(){}','public void updatePressureChange(){invalidatePressure();}')
p.write_text(s)
p=root/'OmniPumpBlockEntity.java';s=p.read_text();a=s.index('    private record Step');b=s.index('    @Override protected void write',a)
s=s[:a]+'''    private boolean pressureDirty=true;
    public void invalidatePressure(){pressureDirty=true;}
    @Override public void onSpeedChanged(float previous){super.onSpeedChanged(previous);invalidatePressure();}
    @Override public void tick(){
        super.tick();if(level==null||level.isClientSide()||!pressureDirty)return;
        pressureDirty=false;
        var adapter=new dev.ccd.logistics.NativeEndpointPump(level,worldPosition,getBlockState());
        for(var side:Direction.values()){
            var next=worldPosition.relative(side);
            FluidPropagator.propagateChangedPipe(level,next,level.getBlockState(next));
            int mode=OmniPumpBlock.mode(getBlockState(),side);
            if(mode==OmniPumpBlock.INPUT||mode==OmniPumpBlock.OUTPUT)adapter.distribute(side,mode==OmniPumpBlock.INPUT,powered()?Math.abs(getSpeed()):0);
        }
    }
'''+s[b:];p.write_text(s)
p=root/'CreativeDeliveryTankBlockEntity.java';s=p.read_text();a=s.index('    private record Step');b=s.index('    @Override protected void write',a)
s=s[:a]+'''    private boolean pressureDirty=true;
    public void invalidatePressure(){pressureDirty=true;}
    @Override public void tick(){
        super.tick();if(level==null||level.isClientSide()||!pressureDirty)return;
        pressureDirty=false;
        var side=getBlockState().getValue(CreativeDeliveryTankBlock.FACING);var next=worldPosition.relative(side);
        FluidPropagator.propagateChangedPipe(level,next,level.getBlockState(next));
        if(!selected.isEmpty())new dev.ccd.logistics.NativeEndpointPump(level,worldPosition,getBlockState()).distribute(side,false,RATE*2f);
    }
'''+s[b:];s=s.replace('public void select(FluidStack fluid){','public void select(FluidStack fluid){invalidatePressure();');p.write_text(s)
