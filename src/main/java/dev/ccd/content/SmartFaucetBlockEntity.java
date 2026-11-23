package dev.ccd.content;

import java.lang.ref.WeakReference;
import java.util.List;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import dev.ccd.CopperDynamics;
import dev.ccd.CCDTooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.Nullable;

/** Native recipe and belt APIs, supplied directly from the rear container at completion. */
public final class SmartFaucetBlockEntity extends FaucetBlockEntity {
    private FilteringBehaviour filtering;
    private boolean outputMode;
    public boolean outputMode() { return outputMode; }
    public void setOutputMode(boolean output) {
        outputMode=output;resetProcessing();notifyUpdate();
    }
    private WeakReference<TransportedItemStack> processing = new WeakReference<>(null);
    private ItemStack inputIdentity = ItemStack.EMPTY;
    private long readyAt, lastSeen;
    private record FillPlan(IFluidHandler source, FluidStack offered, int required) {}
    private record Scan(@Nullable FillPlan plan, boolean waitForFluid) {}

    public SmartFaucetBlockEntity(BlockPos pos, BlockState state) {
        super(CopperDynamics.SMART_FAUCET_BE.get(), pos, state);
    }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        behaviours.add(filtering = new FilteringBehaviour(this, new ValueBoxTransform.Sided() {
            @Override protected Vec3 getSouthLocation() { return new Vec3(.5, .5, 11.25 / 16); }
            @Override public float getScale() { return .375f; }
            @Override protected boolean isSideActive(BlockState state, Direction side) {
                return side == state.getValue(FaucetBlock.FACING);
            }
        }) {
            @Override public boolean acceptsValueSettings() { return true; }
            @Override public boolean isCountVisible() { return false; }
            @Override public boolean isRecipeFilter() { return false; }
            @Override public com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard createBoard(net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit) {
                return new com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard(
                    Component.translatable("ccd.smart_faucet.filter_mode"),0,1,
                    List.of(Component.translatable("ccd.smart_faucet.filter_fluid"),Component.translatable("ccd.smart_faucet.filter_output")),
                    new com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter(v->Component.translatable(v.row()==0?"ccd.smart_faucet.filter_fluid":"ccd.smart_faucet.filter_output")));
            }
            @Override public ValueSettings getValueSettings() { return new ValueSettings(outputMode?1:0,0); }
            @Override public void setValueSettings(net.minecraft.world.entity.player.Player player,ValueSettings settings,boolean ctrlDown) {
                setOutputMode(settings.row()==1);
            }
            @Override public net.minecraft.network.chat.MutableComponent getLabel() {
                return Component.translatable(outputMode?"ccd.smart_faucet.filter_output":"ccd.smart_faucet.filter_fluid");
            }
        }.withCallback(stack->resetProcessing()));
        behaviours.add(new BeltProcessingBehaviour(this).whenItemEnters(this::receive).whileItemHeld(this::hold));
    }
    @Override protected boolean acceptsFluid(FluidStack fluid) { return outputMode || filtering.test(fluid); }
    @Override protected boolean allowContainerTransfer() { return !outputMode && processing.get() == null; }
    @Override public void tick() {
        if (!dev.ccd.CCDFeatures.enabled(getBlockState())) return;
        if (level != null && (!enabled() || level.getGameTime() - lastSeen > 1)) resetProcessing();
        super.tick();
    }
    private void resetProcessing() {
        processing.clear();
        inputIdentity = ItemStack.EMPTY;
        readyAt = 0;
    }
    private boolean canProcess(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler) {
        return level != null && !level.isClientSide() && !isRemoved() && !handler.blockEntity.isVirtual()
            && enabled() && !transferring && !BeltProcessingBehaviour.isBlocked(level, worldPosition.below(2))
            && FillingBySpout.canItemBeFilled(level, item.stack);
    }
    private ProcessingResult receive(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler) {
        if (!canProcess(item, handler)) return ProcessingResult.PASS;
        Scan scan = scan(item.stack);
        return scan.plan() != null || scan.waitForFluid() ? ProcessingResult.HOLD : ProcessingResult.PASS;
    }
    private Scan scan(ItemStack item) {
        IFluidHandler source = inputHandler();
        if (source == null) return new Scan(null, true);
        boolean sawFluid = false, needsMore = false;
        for (int tank = 0; tank < source.getTanks(); tank++) {
            FluidStack sample = source.getFluidInTank(tank).copy();
            if (sample.isEmpty() || !acceptsFluid(sample)) continue;
            FluidStack offered = FluidTransfers.planDrain(source, sample, LIMIT, false);
            if (offered.isEmpty()) continue;
            sawFluid = true;
            int required = FillingBySpout.getRequiredAmountForItem(level, item.copy(), offered.copy());
            if (required <= 0 || required > LIMIT) continue;
            if (required > offered.getAmount()) { needsMore = true; continue; }
            if (!acceptsFillingOutput(item,offered,required)) continue;
            return new Scan(new FillPlan(source, offered, required), false);
        }
        return new Scan(null, needsMore || !sawFluid);
    }
    private ProcessingResult hold(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler) {
        if (!canProcess(item, handler)) { resetProcessing(); return ProcessingResult.PASS; }
        lastSeen = level.getGameTime();
        Scan scan = scan(item.stack);
        if (scan.plan() == null) {
            resetProcessing();
            return scan.waitForFluid() ? ProcessingResult.HOLD : ProcessingResult.PASS;
        }
        if (processing.get() != item || !ItemStack.isSameItemSameComponents(inputIdentity, item.stack)) {
            processing = new WeakReference<>(item);
            inputIdentity = item.stack.copyWithCount(1);
            readyAt = lastSeen + SpoutBlockEntity.FILLING_TIME;
            AllSoundEvents.SPOUTING.playOnServer(level, worldPosition, .75f, 1);
            return ProcessingResult.HOLD;
        }
        if (lastSeen < readyAt) return ProcessingResult.HOLD;
        transferring = true;
        try {
            complete(item, handler, scan.plan());
        } finally {
            transferring = false;
            resetProcessing();
        }
        return ProcessingResult.HOLD;
    }
    private void complete(TransportedItemStack item, TransportedItemStackHandlerBehaviour handler, FillPlan plan) {
        // Evaluate the native operation on copies. No physical item changes before exact withdrawal succeeds.
        ItemStack original = item.stack.copy();
        ItemStack leftover = original.copy();
        FluidStack fluid = plan.offered().copy();
        if (!acceptsFillingOutput(original,plan.offered(),plan.required())) return;
        ItemStack result = FillingBySpout.fillItem(level, plan.required(), leftover, fluid);
        if (leftover.getCount() != item.stack.getCount() - 1
            || plan.offered().getAmount() - fluid.getAmount() != plan.required()) return;
        FluidStack request = plan.offered().copyWithAmount(plan.required());
        FluidStack confirmed = plan.source().drain(request.copy(), FluidAction.SIMULATE);
        if (!FluidStack.isSameFluidSameComponents(confirmed, request) || confirmed.getAmount() < request.getAmount()) return;
        FluidStack actual = plan.source().drain(request.copy(), FluidAction.EXECUTE);
        if (!FluidStack.matches(actual, request)) {
            if (!actual.isEmpty()) returnUndelivered(plan.source(), actual);
            return;
        }
        if (!enabled() || !acceptsFluid(actual) || isRemoved()
            || BeltProcessingBehaviour.isBlocked(level, worldPosition.below(2)) || handler.blockEntity.isRemoved()
            || level.getBlockEntity(handler.getPos()) != handler.blockEntity) {
            returnUndelivered(plan.source(), actual);
            return;
        }
        boolean[] committed = { false };
        handler.handleProcessingOnAllItems(current -> {
            if (committed[0] || current != item || !ItemStack.matches(current.stack, original)) return null;
            current.clearFanProcessingData();
            var output = current.copy();
            output.stack = result;
            var remaining = current.copy();
            remaining.stack = leftover;
            committed[0] = true;
            return TransportedResult.convertToAndLeaveHeld(result.isEmpty() ? List.of() : List.of(output),
                leftover.isEmpty() ? null : remaining);
        });
        if (committed[0]) splash(actual, 2);
        else returnUndelivered(plan.source(), actual);
    }
    /** Inspect declared recipe outputs without rolling random results during preflight. */
    public boolean acceptsFillingOutput(ItemStack item,FluidStack offered,int required) {
        if(!outputMode || filtering.getFilter().isEmpty())return true;
        var input=new net.minecraft.world.item.crafting.SingleRecipeInput(item.copy());
        var assembly=com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.getRecipe(level,input,
            com.simibubi.create.AllRecipeTypes.FILLING.getType(),com.simibubi.create.content.fluids.transfer.FillingRecipe.class,
            holder->holder.value().matches(input,level)&&holder.value().getRequiredFluid().test(offered));
        if(assembly.isPresent())return assembly.get().value().getRollableResults().stream()
            .anyMatch(output->output.getChance()>0&&filtering.test(output.getStack()));
        net.minecraft.world.item.crafting.RecipeType<com.simibubi.create.content.fluids.transfer.FillingRecipe> fillingType=com.simibubi.create.AllRecipeTypes.FILLING.getType();
        var recipe=level.getRecipeManager().getAllRecipesFor(fillingType).stream()
            .map(net.minecraft.world.item.crafting.RecipeHolder::value)
            .filter(value->value.matches(input,level)&&value.getRequiredFluid().test(offered)).findFirst();
        if(recipe.isPresent())return recipe.get().getRollableResults().stream()
            .anyMatch(output->output.getChance()>0&&filtering.test(output.getStack()));
        var copy=item.copy();var fluid=offered.copy();
        var result=com.simibubi.create.content.fluids.transfer.GenericItemFilling.fillItem(level,required,copy,fluid);
        return !result.isEmpty()&&filtering.test(result);
    }
    @Override protected void write(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean clientPacket) {
        super.write(tag,registries,clientPacket);tag.putBoolean("FilterOutput",outputMode);
    }
    @Override protected void read(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries,boolean clientPacket) {
        super.read(tag,registries,clientPacket);outputMode=tag.getBoolean("FilterOutput");resetProcessing();
    }
}
