package dev.ccd.gametest;

import dev.ccd.CopperDynamics;
import dev.ccd.content.HorizontalSmartTankBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ccd")
@PrefixGameTestTemplate(false)
public class HorizontalTankPlacementTests {
    @GameTest(template="empty")
    public static void fillsHorizontalEndLayers(GameTestHelper h) {
        check(h, Direction.EAST, false);
    }
    @GameTest(template="empty")
    public static void fillsNegativeHorizontalEndLayers(GameTestHelper h) {
        check(h, Direction.NORTH, false);
    }
    @GameTest(template="empty")
    public static void sneakingPlacesOnlyOneTank(GameTestHelper h) {
        check(h, Direction.SOUTH, true);
    }
    private static void check(GameTestHelper h, Direction face, boolean sneak) {
        var axis = face.getAxis();
        var origin = new BlockPos(2, 2, 2);
        var state = CopperDynamics.HORIZONTAL_SMART_TANK.get().defaultBlockState().setValue(HorizontalSmartTankBlock.AXIS, axis);
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++)
            h.setBlock(origin.offset(axis == Direction.Axis.X ? new BlockPos(0, x, z) : new BlockPos(x, z, 0)), state);
        h.runAfterDelay(6, () -> {
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
            player.getInventory().clearContent();
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setShiftKeyDown(sneak);
            // Look perpendicular to the tank to verify axis inheritance as well.
            player.setYRot(axis == Direction.Axis.X ? 0 : 90);
            var stack = new ItemStack(CopperDynamics.HORIZONTAL_SMART_TANK_ITEM.get(), 8);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var absolute = h.absolutePos(origin);
            var hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)), face, absolute, false);
            var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
            var result = CopperDynamics.HORIZONTAL_SMART_TANK_ITEM.get().place(context);
            h.assertTrue(result.consumesAction(), "Tank placement failed");
            int count = 0;
            for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) {
                var target = origin.relative(face).offset(axis == Direction.Axis.X ? new BlockPos(0, x, z) : new BlockPos(x, z, 0));
                var actual = h.getBlockState(target);
                if (actual.is(CopperDynamics.HORIZONTAL_SMART_TANK.get())) {
                    count++;
                    h.assertTrue(actual.getValue(HorizontalSmartTankBlock.AXIS) == axis, "Placed tank lost controller axis");
                }
            }
            h.assertTrue(count == (sneak ? 1 : 4), "Wrong number of tanks in added layer: " + count);
            h.assertTrue(stack.getCount() == (sneak ? 7 : 4), "Incorrect item consumption");
            player.setShiftKeyDown(false);
            player.getInventory().clearContent();
            h.succeed();
        });
    }
}
