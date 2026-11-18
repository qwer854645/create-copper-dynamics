package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import dev.ccd.CopperDynamics;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public final class HorizontalSmartTankBlock extends SmartFluidTankBlock {
    public static final EnumProperty<Direction.Axis> AXIS=BlockStateProperties.HORIZONTAL_AXIS;
    public static final MapCodec<HorizontalSmartTankBlock> CODEC=simpleCodec(HorizontalSmartTankBlock::new);
    public HorizontalSmartTankBlock(Properties properties) { super(properties); registerDefaultState(defaultBlockState().setValue(AXIS,Direction.Axis.X)); }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(AXIS); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(AXIS,context.getHorizontalDirection().getAxis()); }
    @Override public BlockState rotate(BlockState state,Rotation rotation) { return rotation==Rotation.CLOCKWISE_90 || rotation==Rotation.COUNTERCLOCKWISE_90 ? state.setValue(AXIS,state.getValue(AXIS)==Direction.Axis.X?Direction.Axis.Z:Direction.Axis.X):state; }
    @Override public BlockState mirror(BlockState state,Mirror mirror) { return state; }
    @Override public net.minecraft.world.InteractionResult onWrenched(BlockState state,net.minecraft.world.item.context.UseOnContext context) {
        var tank=context.getLevel().getBlockEntity(context.getClickedPos());
        if(tank instanceof HorizontalSmartTankBlockEntity horizontal) {
            if(!context.getLevel().isClientSide()) {
                var root=horizontal.getControllerBE();
                if(root!=null) root.setWindows(!root.hasWindow());
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }
    @Override public BlockEntityType<? extends SmartFluidTankBlockEntity> getBlockEntityType() { return CopperDynamics.HORIZONTAL_SMART_TANK_BE.get(); }
}
