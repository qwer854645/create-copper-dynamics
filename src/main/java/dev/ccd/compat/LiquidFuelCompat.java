package dev.ccd.compat;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;

/** Optional released Liquid Fuel 3.x data API; no hard dependency on its classes. */
public final class LiquidFuelCompat {
    private LiquidFuelCompat(){}
    private static boolean unavailable;
    public record Fuel(int amount,int ticks,boolean superheated){}
    private static final class Methods {
        static final java.lang.reflect.Method GET,AMOUNT,TICKS,HOT;
        static {
            try {
                GET=Class.forName("com.forsteri.createliquidfuel.core.LiquidFuels").getMethod("get",Fluid.class);
                var type=GET.getReturnType();
                AMOUNT=type.getMethod("amountConsumedPerTick");
                TICKS=type.getMethod("burnTime");
                HOT=type.getMethod("superHeat");
            }catch(ReflectiveOperationException e){throw new IllegalStateException("Unsupported Create Liquid Fuel API",e);}
        }
    }
    public static Fuel find(Fluid fluid){
        if(unavailable||!ModList.get().isLoaded("createliquidfuel"))return null;
        try{
            var value=Methods.GET.invoke(null,fluid);
            if(value==null)return null;
            int amount=(Integer)Methods.AMOUNT.invoke(value);
            int ticks=(Integer)Methods.TICKS.invoke(value);
            boolean hot=(Boolean)Methods.HOT.invoke(value);
            return amount>0&&ticks>0?new Fuel(amount,ticks,hot):null;
        }catch(ReflectiveOperationException | LinkageError | RuntimeException e){
            unavailable=true;
            org.slf4j.LoggerFactory.getLogger("CCD-LiquidFuel").error("Disabling CCD Liquid Fuel adapter: unsupported API",e);
            return null;
        }
    }
}
