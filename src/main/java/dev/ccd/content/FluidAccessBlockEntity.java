package dev.ccd.content;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.SidedFilteringBehaviour;
import dev.ccd.logistics.FluidAccess;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class FluidAccessBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public SidedFilteringBehaviour filters;
    private final IFluidHandler handler = FluidAccess.handler(this::sources);
    public FluidAccessBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); }
    public List<FluidAccess.Source> sources() {
        return level==null || isRemoved() || !level.hasChunkAt(worldPosition) || level.getBlockEntity(worldPosition)!=this
            ? List.of() : FluidAccess.resolve(level,worldPosition,null);
    }
    public IFluidHandler handler(Direction side) {
        if (!accessible(side)) return null;
        return FluidAccess.handler(() -> accessible(side) ? sources() : List.of());
    }
    private boolean accessible(Direction side) {
        Direction facing=getBlockState().getValue(FluidAccessBlock.FACING);
        return side==null || !(((FluidAccessBlock)getBlockState().getBlock()).isBridge()
            ? side.getAxis()==facing.getAxis() : side==facing.getOpposite());
    }
    public boolean accepts(Direction end, FluidStack fluid) {
        return filters==null || filters.get(end)==null || filters.get(end).test(fluid);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if (!((FluidAccessBlock)getBlockState().getBlock()).isBridge()) return;
        behaviours.add(filters=new SidedFilteringBehaviour(this,new ValueBoxTransform.Sided() {
            @Override protected Vec3 getSouthLocation() { return new Vec3(.5,13/16d,18.55/16d); }
            @Override public float getScale() { return .3f; }
            @Override protected boolean isSideActive(BlockState state,Direction side) { return side.getAxis()==state.getValue(FluidAccessBlock.FACING).getAxis(); }
        },(direction,filter)->filter.forFluids().withCallback(stack->{ if(level!=null) level.invalidateCapabilities(worldPosition); }),
            direction->direction.getAxis()==getBlockState().getValue(FluidAccessBlock.FACING).getAxis()));
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip,boolean sneaking) {
        containedFluidTooltip(tooltip,sneaking,handler);
        tooltip.add(Component.translatable("ccd.fluid_access.sources",sources().stream().map(FluidAccess.Source::identity).distinct().count()));
        tooltip.add(Component.translatable("ccd.fluid_access.no_storage"));
        return true;
    }
}
