package dev.ccd.content;

import java.util.List;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/** Native pipe flow metadata only: no owned liquid, tank capability or independent transfer tick. */
public final class ThreeWayValveBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public ThreeWayValveBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.THREE_WAY_VALVE_BE.get(), pos, state);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(new FluidTransportBehaviour(this) {
            @Override public boolean canHaveFlowToward(BlockState state, Direction side) {
                return ThreeWayValveBlock.open(state, side);
            }
            @Override public boolean canPullFluidFrom(FluidStack fluid, BlockState state, Direction side) {
                return ThreeWayValveBlock.open(state, side) && super.canPullFluidFrom(fluid, state, side);
            }
        });
        registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
    }
    @Override public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide()) {
            ThreeWayValveBlock.updateSignal(level, worldPosition);
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        CCDTooltip.heading(tooltip, "ccd.gui.goggles.valve");
        CCDTooltip.line(tooltip, ChatFormatting.AQUA, getBlockState().getValue(ThreeWayValveBlock.POWERED)
            ? "ccd.three_way.side" : "ccd.three_way.straight");
        return true;
    }
}
