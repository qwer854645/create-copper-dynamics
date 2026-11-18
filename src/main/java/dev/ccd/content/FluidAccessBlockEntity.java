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
            @Override protected Vec3 getSouthLocation() { return new Vec3(.5,.5,18.01/16d); }
            @Override public float getScale() { return .3f; }
            @Override protected boolean isSideActive(BlockState state,Direction side) { return side.getAxis()==state.getValue(FluidAccessBlock.FACING).getAxis(); }
        },(direction,filter)->filter.forFluids().withCallback(stack->{ if(level!=null) level.invalidateCapabilities(worldPosition); }),
            direction->direction.getAxis()==getBlockState().getValue(FluidAccessBlock.FACING).getAxis()));
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip,boolean sneaking) {
        var fluids = new java.util.ArrayList<FluidStack>();
        for (int i=0;i<handler.getTanks();i++) {
            var fluid=handler.getFluidInTank(i);
            if (fluid.isEmpty()) continue;
            var existing=fluids.stream().filter(f->FluidStack.isSameFluidSameComponents(f,fluid)).findFirst();
            if(existing.isPresent()) existing.get().setAmount((int)Math.min(Integer.MAX_VALUE,(long)existing.get().getAmount()+fluid.getAmount()));
            else fluids.add(fluid.copy());
        }
        // Filtered multi-fluid views can expose shared virtual slots. Do not sum
        // their capacities into an invented physical storage size.
        dev.ccd.CCDTooltip.heading(tooltip,"create.gui.goggles.fluid_container");
        for(var fluid:fluids) {
            com.simibubi.create.foundation.utility.CreateLang.fluidName(fluid).style(net.minecraft.ChatFormatting.GRAY).forGoggles(tooltip,1);
            com.simibubi.create.foundation.utility.CreateLang.builder().add(dev.ccd.CCDTooltip.amount(fluid.getAmount())).forGoggles(tooltip,1);
        }
        dev.ccd.CCDTooltip.line(tooltip,"ccd.fluid_access.sources",dev.ccd.CCDTooltip.number(sources().stream().map(FluidAccess.Source::identity).distinct().count()));
        return true;
    }
}
