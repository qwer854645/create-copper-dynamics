package dev.ccd.content;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Every physical face is configured independently: closed, inlet, outlet, shaft. */
public final class OmniPumpBlock extends KineticBlock implements IBE<OmniPumpBlockEntity>, IWrenchable {
    public static final int CLOSED=0,INPUT=1,OUTPUT=2,SHAFT=3;
    public static final java.util.Map<Direction,IntegerProperty> PORTS=new java.util.EnumMap<>(Direction.class);
    static {for(var side:Direction.values())PORTS.put(side,IntegerProperty.create(side.getName(),0,3));}
    public OmniPumpBlock(Properties properties){super(properties);var state=defaultBlockState();for(var p:PORTS.values())state=state.setValue(p,CLOSED);registerDefaultState(state);}
    @Override protected com.mojang.serialization.MapCodec<? extends KineticBlock> codec(){return simpleCodec(OmniPumpBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PORTS.values().toArray(IntegerProperty[]::new));}
    public static int mode(BlockState state,Direction side){return state.getValue(PORTS.get(side));}
    @Override public void neighborChanged(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,Block other,BlockPos neighbor,boolean moving){
        super.neighborChanged(state,level,pos,other,neighbor,moving);
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof OmniPumpBlockEntity pump)pump.invalidatePressure();
    }
    @Override public Direction.Axis getRotationAxis(BlockState state){for(var side:Direction.values())if(mode(state,side)==SHAFT)return side.getAxis();return Direction.Axis.Y;}
    @Override public boolean hasShaftTowards(LevelReader world,BlockPos pos,BlockState state,Direction side){return mode(state,side)==SHAFT;}
    @Override public InteractionResult onWrenched(BlockState state,UseOnContext context){
        if(!context.getLevel().isClientSide()){
            var level=context.getLevel();var pos=context.getClickedPos();var side=context.getClickedFace();
            if(level.getBlockEntity(pos) instanceof OmniPumpBlockEntity pump)pump.detachKinetics();
            var next=state.cycle(PORTS.get(side));level.setBlockAndUpdate(pos,next);level.invalidateCapabilities(pos);
            if(level.getBlockEntity(pos) instanceof OmniPumpBlockEntity pump){pump.attachKinetics();pump.invalidatePressure();pump.notifyUpdate();}
            context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.translatable("ccd.omni_pump.mode."+mode(next,side)),true);
        }
        return InteractionResult.SUCCESS;
    }
    private BlockState mapped(BlockState state,java.util.function.UnaryOperator<Direction> mapping){var result=state;for(var side:Direction.values())result=result.setValue(PORTS.get(mapping.apply(side)),mode(state,side));return result;}
    @Override public BlockState rotate(BlockState state,net.minecraft.world.level.block.Rotation rotation){return mapped(state,rotation::rotate);}
    @Override public BlockState mirror(BlockState state,net.minecraft.world.level.block.Mirror mirror){return mapped(state,mirror::mirror);}
    @Override public Class<OmniPumpBlockEntity> getBlockEntityClass(){return OmniPumpBlockEntity.class;}
    @Override public BlockEntityType<? extends OmniPumpBlockEntity> getBlockEntityType(){return CopperDynamics.OMNI_PUMP_BE.get();}
}
