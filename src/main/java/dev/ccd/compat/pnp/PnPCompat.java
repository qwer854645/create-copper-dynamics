package dev.ccd.compat.pnp;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import dev.ccd.content.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

/** Keep optional engine classes behind the mod-presence check. */
public final class PnPCompat {
    public static final ThreadLocal<Boolean> CONTAINERS_ONLY=ThreadLocal.withInitial(()->false);
    private PnPCompat() {}
    public static boolean active() {
        return ModList.get().isLoaded("pipesnphysics")
            && de.devin.pipesnphysics.PipesNPhysicsConfig.ENABLE_ENGINE.get();
    }
    public static boolean endpoint(BlockEntity be) {
        return be instanceof BufferPumpBlockEntity
            || be instanceof CreativeDeliveryTankBlockEntity;
    }
    public static int stored(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos) {
        var store=de.devin.pipesnphysics.engine.store.PipeStore.at(level,pos);
        return store==null?0:store.amount();
    }
    public static boolean pump(BlockEntity be) {
        return be instanceof PumpBlockEntity && "ccd".equals(
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getNamespace())
            && !endpoint(be);
    }
    public static double strength(PumpBlockEntity pump) {
        if(!dev.ccd.CCDFeatures.enabled(pump.getBlockState()))return 0;
        if (pump instanceof VariableFlowPumpBlockEntity variable) return variable.flowPressure();
        return pump.isOverStressed() ? 0 : Math.abs(pump.getSpeed());
    }
    public static Direction push(PumpBlockEntity pump) {
        var state=pump.getBlockState();
        var front=state.getValue(PumpBlock.FACING);
        if (!pump.isPullingOnSide(true)) return front;
        return state.getBlock() instanceof TurningBoosterPumpBlock ? TurningBoosterPumpBlock.branch(state) : front.getOpposite();
    }
    public static Direction pull(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,Direction push) {
        var state=level.getBlockState(pos);
        if (!(state.getBlock() instanceof TurningBoosterPumpBlock)) return push.getOpposite();
        var front=state.getValue(PumpBlock.FACING);
        return push==front ? TurningBoosterPumpBlock.branch(state) : front;
    }
}
