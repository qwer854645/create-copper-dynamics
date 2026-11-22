package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.atomizing.AtomizerProcessing;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class OutputFilterBoundaryTests {
    @GameTest(template="empty") public static void unknownProcessorsAreNotRunToPreviewOutputs(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CopperDynamics.FLUID_ATOMIZER.get().defaultBlockState());
        var owner=(dev.ccd.content.FluidAtomizerBlockEntity)h.getBlockEntity(pos);
        var filter=owner.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour.TYPE);
        filter.setFilter(new ItemStack(Items.DIAMOND));
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        var unknown=new com.simibubi.create.content.kinetics.fan.processing.FanProcessingType(){
            public int getPriority(){return 0;}
            public boolean isValidAt(net.minecraft.world.level.Level level,BlockPos p){return false;}
            public boolean canProcess(ItemStack stack,net.minecraft.world.level.Level level){return true;}
            public java.util.List<ItemStack> process(ItemStack stack,net.minecraft.world.level.Level level){calls.incrementAndGet();return java.util.List.of(new ItemStack(Items.DIAMOND));}
            public void affectEntity(net.minecraft.world.entity.Entity e,net.minecraft.world.level.Level l){}
            public void morphAirFlow(AirFlowParticleAccess p,net.minecraft.util.RandomSource r){}
            public void spawnProcessingParticles(net.minecraft.world.level.Level l,net.minecraft.world.phys.Vec3 p){}
        };
        var input=new com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack(new ItemStack(Items.DIRT));
        AtomizerProcessing.withSource(owner,()->{
            h.assertTrue(owner.hasOutputFilter()&&!AtomizerProcessing.supportsOutputFilter(unknown,input.stack,h.getLevel()),"Unknown type incorrectly approved");
            for(int i=0;i<200;i++)com.simibubi.create.content.kinetics.fan.processing.FanProcessing.applyProcessing(input,h.getLevel(),unknown);
        });
        h.assertTrue(calls.get()==0&&input.stack.is(Items.DIRT),"Unknown processor ran speculatively or consumed input: calls="+calls.get()+" stack="+input.stack);
        filter.setFilter(ItemStack.EMPTY);
        h.assertTrue(AtomizerProcessing.supportsOutputFilter(unknown,input.stack,h.getLevel()),"Empty filter blocked unknown processing");h.succeed();
    }
}
