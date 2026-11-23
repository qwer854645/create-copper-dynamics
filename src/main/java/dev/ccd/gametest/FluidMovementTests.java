package dev.ccd.gametest;

import dev.ccd.compat.ProcessingMedia;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import net.minecraft.world.phys.Vec3;

@GameTestHolder("ccd") @PrefixGameTestTemplate(false)
public class FluidMovementTests {
    @GameTest(template="empty") public static void superLavaMovesInsteadOfSwallowingInput(GameTestHelper h){
        var entity=h.spawn(net.minecraft.world.entity.EntityType.COW,2,2,2);
        entity.setNoAi(true);entity.setNoGravity(true);
        var medium=ProcessingMedia.ALL.getFirst();
        var before=entity.position();
        entity.setDeltaMovement(Vec3.ZERO);
        boolean handled=medium.type.get().move(medium.source.get().defaultFluidState(),entity,new Vec3(0,0,1),0);
        h.assertTrue(handled&&entity.position().distanceTo(before)>.001,"Super lava swallowed directional input without moving");
        entity.setDeltaMovement(0,.2,0);double y=entity.getY();
        medium.type.get().move(medium.source.get().defaultFluidState(),entity,Vec3.ZERO,.08);
        h.assertTrue(entity.getY()>y,"Super lava prevented upward escape movement");
        h.assertTrue(entity.getDeltaMovement().y>0,"Upward motion was clamped away");
        for(var other:ProcessingMedia.ALL)if(other!=medium)
            h.assertTrue(!other.type.get().move(other.source.get().defaultFluidState(),entity,Vec3.ZERO,.08),"Other medium intercepts movement: "+other.id);
        entity.discard();h.succeed();
    }
}
