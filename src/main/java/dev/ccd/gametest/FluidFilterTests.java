package dev.ccd.gametest;

import com.simibubi.create.content.logistics.filter.FilterItemStack;
import dev.ccd.CopperDynamics;
import dev.ccd.content.FluidFilterRules;
import dev.ccd.content.FluidStockItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class FluidFilterTests {
    @GameTest(template="empty") public static void editorPreservesSlotsAndAtomicAttributes(GameTestHelper h){
        var data=new CompoundTag();var water=new FluidStack(Fluids.WATER,1);var lava=new FluidStack(Fluids.LAVA,1);
        var registries=h.getLevel().registryAccess();
        dev.ccd.content.FluidFilterEditor.apply(data,false,registries,0,17,water,0);
        dev.ccd.content.FluidFilterEditor.apply(data,false,registries,0,4,lava,0);
        dev.ccd.content.FluidFilterEditor.apply(data,false,registries,1,4,FluidStack.EMPTY,0);
        var entries=data.getList("entries",10);
        h.assertTrue(entries.size()==1&&entries.getCompound(0).getInt("slot")==17,"Removing a sample shifted another slot");
        h.assertTrue(FluidFilterRules.matches(data,false,h.getLevel(),water)&&!FluidFilterRules.matches(data,false,h.getLevel(),lava),"Sparse filter changed matching");
        dev.ccd.content.FluidFilterEditor.apply(data,false,registries,2,0,FluidStack.EMPTY,1);
        dev.ccd.content.FluidFilterEditor.apply(data,false,registries,2,0,FluidStack.EMPTY,1);
        h.assertTrue(data.getBoolean("blacklist"),"Repeated mode selection toggled state");
        data=new CompoundTag();
        h.assertTrue(dev.ccd.content.FluidFilterEditor.apply(data,true,registries,6,0,water,0),"Inverted attribute rejected");
        h.assertTrue(data.getList("entries",10).getCompound(0).getBoolean("invert"),"Inversion not saved atomically");
        h.assertTrue(!dev.ccd.content.FluidFilterEditor.apply(data,true,registries,0,1,water,0),"Duplicate attribute accepted");
        dev.ccd.content.FluidFilterEditor.apply(data,true,registries,5,0,FluidStack.EMPTY,0);
        h.assertTrue(data.getList("entries",10).isEmpty(),"Clear failed");h.succeed();
    }
    @GameTest(template="empty") public static void listFilterUsesNativeWrapperAndPersists(GameTestHelper h) {
        var water = new FluidStack(Fluids.WATER, 1000);
        var lava = new FluidStack(Fluids.LAVA, 1000);
        var settings = new CompoundTag(); var entries = new ListTag(); var entry = new CompoundTag();
        entry.put("fluid", water.copyWithAmount(1).saveOptional(h.getLevel().registryAccess())); entries.add(entry); settings.put("entries", entries);
        var filter = new ItemStack(CopperDynamics.FLUID_FILTER.get());
        filter.set(CopperDynamics.FLUID_FILTER_DATA.get(), CustomData.of(settings));
        var restored = ItemStack.parseOptional(h.getLevel().registryAccess(), (CompoundTag) filter.save(h.getLevel().registryAccess()));
        var wrapper = FilterItemStack.of(restored);
        h.assertTrue(wrapper.isFilterItem() && wrapper.test(h.getLevel(), water) && !wrapper.test(h.getLevel(), lava), "Fluid list wrapper or persistence failed");
        settings.putBoolean("blacklist", true);
        h.assertTrue(!FluidFilterRules.matches(settings, false, h.getLevel(), water) && FluidFilterRules.matches(settings, false, h.getLevel(), lava), "Deny list failed");
        var bucket = new ItemStack(Items.WATER_BUCKET);
        h.assertTrue(FluidStockItem.sample(bucket).getFluid().isSame(Fluids.WATER) && bucket.is(Items.WATER_BUCKET) && bucket.getCount() == 1, "Sampling consumed bucket");
        h.succeed();
    }
    @GameTest(template="empty") public static void attributeFilterSupportsAnyAllAndInversion(GameTestHelper h) {
        var water = new FluidStack(Fluids.WATER, 1000); var lava = new FluidStack(Fluids.LAVA, 1000);
        var settings = new CompoundTag(); var entries = new ListTag();
        entries.add(FluidFilterRules.rule("mod", "minecraft")); entries.add(FluidFilterRules.rule("temperature", "hot")); settings.put("entries", entries);
        h.assertTrue(FluidFilterRules.matches(settings, true, h.getLevel(), water), "Any failed");
        settings.putBoolean("all", true);
        h.assertTrue(!FluidFilterRules.matches(settings, true, h.getLevel(), water) && FluidFilterRules.matches(settings, true, h.getLevel(), lava), "All failed");
        entries.getCompound(1).putBoolean("invert", true);
        h.assertTrue(FluidFilterRules.matches(settings, true, h.getLevel(), water) && !FluidFilterRules.matches(settings, true, h.getLevel(), lava), "Inversion failed");
        var filter = new ItemStack(CopperDynamics.FLUID_ATTRIBUTE_FILTER.get()); filter.set(CopperDynamics.FLUID_FILTER_DATA.get(), CustomData.of(settings));
        h.assertTrue(FilterItemStack.of(filter).test(h.getLevel(), water), "Attribute item not recognized by native filter path");
        h.succeed();
    }
}
