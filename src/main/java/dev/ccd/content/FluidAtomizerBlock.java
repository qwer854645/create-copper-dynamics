package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlock;
import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.Tags;
import com.simibubi.create.foundation.fluid.FluidHelper;

public final class FluidAtomizerBlock extends EncasedFanBlock {
    public static final MapCodec<FluidAtomizerBlock> CODEC = simpleCodec(FluidAtomizerBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public FluidAtomizerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }
    @Override public BlockState getRotatedBlockState(BlockState state,net.minecraft.core.Direction face){
        var facing=state.getValue(FACING);
        var axis=face.getAxis()==facing.getAxis()
            ?(facing.getAxis()==net.minecraft.core.Direction.Axis.Y?net.minecraft.core.Direction.Axis.X:net.minecraft.core.Direction.Axis.Y)
            :face.getAxis();
        return state.setValue(FACING,facing.getClockWise(axis));
    }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block other, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, other, from, moving);
        updateSignal(level, pos);
    }
    static void updateSignal(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FluidAtomizerBlock)) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) level.setBlockAndUpdate(pos, state.setValue(POWERED, powered));
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        if(level.getBlockEntity(pos) instanceof FluidAtomizerBlockEntity atomizer) {
            var filter=atomizer.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour.TYPE);
            if(filter!=null&&filter.getSlotPositioning().testHit(level,pos,state,hit.getLocation().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(pos))))
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(Tags.Items.TOOLS_WRENCH) || !FluidHatchBlock.isFluidContainer(level, stack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof FluidAtomizerBlockEntity atomizer)) return ItemInteractionResult.FAIL;
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        // Handheld access works on every face; machine capabilities remain side-only.
        if (!FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, atomizer))
            FluidHelper.tryFillItemFromBE(level, player, hand, stack, atomizer);
        return ItemInteractionResult.SUCCESS;
    }
    @Override public BlockEntityType<? extends FluidAtomizerBlockEntity> getBlockEntityType() {
        return CopperDynamics.FLUID_ATOMIZER_BE.get();
    }
}
