package dev.ccd.content;

import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

/** Native inlet filter; existing contents remain drainable after filter changes. */
public final class SmartCollectionGrateBlockEntity extends CopperCollectionGrateBlockEntity {
    private FilteringBehaviour filtering;
    public SmartCollectionGrateBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.SMART_COLLECTION_GRATE_BE.get(), pos, state);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(filtering = new FilteringBehaviour(this, new ValueBoxTransform() {
            @Override public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) { return new Vec3(.5, .51, .5); }
            @Override public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack pose) { pose.mulPose(Axis.XP.rotationDegrees(90)); }
            @Override public float getScale() { return .3f; }
        }).forFluids().withCallback(this::filterChanged));
    }
    @Override protected boolean automaticEnabled() { return !getBlockState().getValue(SmartCollectionGrateBlock.POWERED); }
    @Override protected boolean acceptsFluid(FluidStack fluid) { return filtering != null && filtering.test(fluid); }
    @Override public void initialize() {
        super.initialize();
        if (level != null) SmartCollectionGrateBlock.updateSignal(level, worldPosition);
    }
    private void filterChanged(ItemStack stack) {
        if (level == null || level.isClientSide()) return;
        invalidateCapabilities();
        // Wake native pipe endpoints which previously rejected this fluid, without clearing or migrating the buffer.
        for (Direction side : Direction.values()) {
            BlockPos neighbour = worldPosition.relative(side);
            if (!level.hasChunkAt(neighbour)) continue;
            if (level.getBlockEntity(neighbour) instanceof PumpBlockEntity pump) pump.updatePressureChange();
            else FluidPropagator.propagateChangedPipe(level, neighbour, level.getBlockState(neighbour));
        }
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        CCDTooltip.heading(tooltip, "ccd.gui.goggles.collection");
        CCDTooltip.line(tooltip, automaticEnabled() ? ChatFormatting.AQUA : ChatFormatting.YELLOW,
            automaticEnabled() ? "ccd.smart_collection_grate.active" : "ccd.smart_collection_grate.paused");
        CCDTooltip.line(tooltip, filtering.getFilter().isEmpty() ? "ccd.smart_collection_grate.unfiltered" : "ccd.smart_collection_grate.filtered");
        return true;
    }
}
