package dev.ccd.mixin;

import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cosmetic title only; language data and all registry identities remain untouched. */
@Mixin(ClientLanguage.class)
abstract class ChineseModTitleMixin {
    @Inject(method="getOrDefault",at=@At("HEAD"),cancellable=true)
    private void ccd$alternateTitle(String key,String fallback,CallbackInfoReturnable<String> cir){
        if(key.equals("itemGroup.ccd") && dev.ccd.CCDFeatures.rebirthName()
            && net.minecraft.client.Minecraft.getInstance().options.languageCode.startsWith("zh_"))
            cir.setReturnValue("流体的结合：重生（泵不住了）");
    }
}
