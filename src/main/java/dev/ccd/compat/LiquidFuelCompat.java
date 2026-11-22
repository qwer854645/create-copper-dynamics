package dev.ccd.compat;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;

/** Optional released Liquid Fuel 3.x data API; no hard dependency on its classes. */
public final class LiquidFuelCompat {
    private LiquidFuelCompat(){}
    public record Fuel(int amount,int ticks,boolean superheated){}
    public static Fuel find(Fluid fluid){
        if(!ModList.get().isLoaded("createliquidfuel"))return null;
        try{
            var registry=Class.forName("com.forsteri.createliquidfuel.core.LiquidFuels");
            var value=registry.getMethod("get",Fluid.class).invoke(null,fluid);
            if(value==null)return null;
            var type=value.getClass();
            int amount=(Integer)type.getMethod("amountConsumedPerTick").invoke(value);
            int ticks=(Integer)type.getMethod("burnTime").invoke(value);
            boolean hot=(Boolean)type.getMethod("superHeat").invoke(value);
            return amount>0&&ticks>0?new Fuel(amount,ticks,hot):null;
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Unsupported Create Liquid Fuel API",e);}
    }
}
