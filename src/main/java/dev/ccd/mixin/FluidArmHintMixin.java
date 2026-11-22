package dev.ccd.mixin;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.foundation.item.TooltipHelper;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ArmBlockEntity.class)
abstract class FluidArmHintMixin {
    @Redirect(method="addToTooltip",at=@At(value="INVOKE",target="Lcom/simibubi/create/foundation/item/TooltipHelper;addHint(Ljava/util/List;Ljava/lang/String;[Ljava/lang/Object;)V"))
    private void ccd$fluidHint(java.util.List<Component> tooltip,String key,Object[] args){
        if((Object)this instanceof dev.ccd.content.FluidArmBlockEntity){
            tooltip.add(Component.translatable("ccd.hint.fluid_arm_no_targets.title").withStyle(net.minecraft.ChatFormatting.GOLD));
            tooltip.add(Component.translatable("ccd.hint.fluid_arm_no_targets").withStyle(net.minecraft.ChatFormatting.GRAY));
        }else TooltipHelper.addHint(tooltip,key,args);
    }
}
