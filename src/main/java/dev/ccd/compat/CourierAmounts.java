package dev.ccd.compat;

import dev.ccd.content.FluidStockItem;
import net.minecraft.world.item.ItemStack;

/** Unit conversion for portable tickers: token counts always remain millibuckets. */
public final class CourierAmounts {
    private CourierAmounts() {}
    public static int adjust(int current, boolean add, boolean shift, boolean control, int minimum, int available, int steps) {
        long step=control?1:shift?100:1000;
        long amount=(long)current+(add?1:-1)*step*Math.max(1L,steps);
        return (int)Math.max(Math.max(0,minimum),Math.min(Math.max(0,available),Math.min(Integer.MAX_VALUE,amount)));
    }
    /** Optional Factory Abstractions wrapper; no hard link when it is absent. */
    public static ItemStack itemKey(Object wrapper) {
        try {
            var api=Class.forName("ru.zznty.create_factory_abstractions.generic.support.BigGenericStack");
            var stack=api.getMethod("asStack").invoke(wrapper);
            return ((com.simibubi.create.content.logistics.BigItemStack)stack).stack;
        } catch (ReflectiveOperationException | LinkageError ignored) { return ItemStack.EMPTY; }
    }
    public static int portableChange(Object wrapper,int requested,boolean shift,boolean control,java.util.List<? extends java.util.List<?>> displayed) {
        if(!(itemKey(wrapper).getItem() instanceof FluidStockItem)) return requested;
        try {
            var api=Class.forName("ru.zznty.create_factory_abstractions.generic.support.BigGenericStack");
            var current=((com.simibubi.create.content.logistics.BigItemStack)api.getMethod("asStack").invoke(wrapper)).count;
            long delta=(long)requested-current;
            long available=0;
            ItemStack identity=itemKey(wrapper);
            for(var row:displayed) for(var entry:row) {
                var sample=(com.simibubi.create.content.logistics.BigItemStack)api.getMethod("asStack").invoke(entry);
                if(ItemStack.isSameItemSameComponents(identity,sample.stack)) available+=Math.max(0,sample.count);
            }
            // Native item branch: plain +/-1, Ctrl +/-10, Shift +/-stackSize.
            int nativeStep=control?10:1;
            long steps=Math.max(1,Math.abs(delta)/nativeStep);
            long value=(long)current+Long.signum(delta)*(control?1:shift?100:1000)*steps;
            return delta==0?requested:(int)Math.clamp(value,0,Math.min(Integer.MAX_VALUE,available));
        } catch(ReflectiveOperationException | LinkageError ignored) { return requested; }
    }
}
