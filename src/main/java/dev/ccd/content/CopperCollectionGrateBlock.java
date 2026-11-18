package dev.ccd.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.behaviour.spouting.CauldronSpoutingBehavior;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.fluid.FluidHelper;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;

/** Native cauldron weather and scheduled dripstone reception, backed by a Create fluid inventory. */
public class CopperCollectionGrateBlock extends CauldronBlock implements IBE<CopperCollectionGrateBlockEntity>, IWrenchable {
    // CauldronBlock's codec return type is invariant; the factory still creates this grate subtype.
    public static final MapCodec<CauldronBlock> CODEC = simpleCodec(CopperCollectionGrateBlock::new);
    protected static final VoxelShape SHAPE = Shapes.or(
        box(3, 0, 3, 13, 2, 13), box(4, 0, 4, 12, 4, 12),
        ring(2, 14, 6, 10, 2, 3), ring(2, 14, 3, 13, 3, 5),
        ring(1, 15, 3, 13, 5, 7), ring(0, 16, 2, 14, 7, 8),
        box(3, 6, 3, 4, 7, 13), box(6, 6, 3, 7, 7, 13),
        box(9, 6, 3, 10, 7, 13), box(12, 6, 3, 13, 7, 13));
    private static VoxelShape ring(int outerMin, int outerMax, int innerMin, int innerMax, int bottom, int top) {
        return Shapes.or(box(outerMin, bottom, outerMin, outerMax, top, innerMin),
            box(outerMin, bottom, innerMax, outerMax, top, outerMax),
            box(outerMin, bottom, innerMin, innerMin, top, innerMax),
            box(innerMax, bottom, innerMin, outerMax, top, innerMax));
    }
    public CopperCollectionGrateBlock(Properties properties) { super(properties); }
    @Override public MapCodec<CauldronBlock> codec() { return CODEC; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) { return SHAPE; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CopperCollectionGrateBlockEntity grate
            ? (int) Math.ceil(grate.inventory().getFluidAmount() * 15.0 / CopperCollectionGrateBlockEntity.CAPACITY) : 0;
    }
    @Override public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        if (level.isClientSide() || !shouldHandlePrecipitation(level, precipitation)) return;
        if (precipitation == Biome.Precipitation.RAIN) collectDrip(level, pos, Fluids.WATER);
        else if (precipitation == Biome.Precipitation.SNOW && level.getBlockEntity(pos) instanceof CopperCollectionGrateBlockEntity grate)
            grate.collectEnvironmental(CCDFluids.POWDER_SNOW.get(), 1000 / 3);
    }
    @Override protected boolean canReceiveStalactiteDrip(Fluid fluid) { return fluid == Fluids.WATER || fluid == Fluids.LAVA; }
    @Override protected void receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid) {
        if (!level.isClientSide()) collectDrip(level, pos, fluid);
    }
    private void collectDrip(Level level, BlockPos pos, Fluid fluid) {
        var info = CauldronSpoutingBehavior.CAULDRON_INFO.get(fluid);
        if (info != null && level.getBlockEntity(pos) instanceof CopperCollectionGrateBlockEntity grate)
            grate.collectEnvironmental(fluid, info.amount());
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Tags.Items.TOOLS_WRENCH) || !FluidHatchBlock.isFluidContainer(level, stack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof CopperCollectionGrateBlockEntity grate)) return ItemInteractionResult.FAIL;
        var before = grate.inventory().getFluid().copy();
        boolean inserted = FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, grate);
        if (inserted || FluidHelper.tryFillItemFromBE(level, player, hand, stack, grate)) {
            if (!level.isClientSide()) level.playSound(null, pos, inserted ? FluidHelper.getEmptySound(grate.inventory().getFluid())
                : FluidHelper.getFillSound(before), SoundSource.BLOCKS, .5f, 1f);
        }
        return ItemInteractionResult.SUCCESS;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        IBE.onRemove(state, level, pos, next);
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public Class<CopperCollectionGrateBlockEntity> getBlockEntityClass() { return CopperCollectionGrateBlockEntity.class; }
    @Override public BlockEntityType<? extends CopperCollectionGrateBlockEntity> getBlockEntityType() {
        return CopperDynamics.COPPER_COLLECTION_GRATE_BE.get();
    }
}
