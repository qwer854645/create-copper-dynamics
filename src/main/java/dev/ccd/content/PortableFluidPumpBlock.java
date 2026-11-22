package dev.ccd.content;

import dev.ccd.CopperDynamics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionResult;

public final class PortableFluidPumpBlock extends VariableFlowPumpBlock {
    public PortableFluidPumpBlock(Properties properties) { super(properties.dynamicShape()); }
    @Override public boolean isSmallCog(){return false;}
    @Override public boolean hasShaftTowards(net.minecraft.world.level.LevelReader level,BlockPos pos,BlockState state,net.minecraft.core.Direction side){
        return side.getAxis()==getRotationAxis(state);
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){
        float stroke=level.getBlockEntity(pos) instanceof PortableFluidPumpBlockEntity pump?pump.stroke:0;
        var local=strokeShape(stroke);
        net.minecraft.world.phys.shapes.VoxelShape result=net.minecraft.world.phys.shapes.Shapes.empty();
        for(var box:local.toAabbs()){
            double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
            double[] max={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
            for(double x:new double[]{box.minX,box.maxX})for(double y:new double[]{box.minY,box.maxY})for(double z:new double[]{box.minZ,box.maxZ}){
                double[] v={.5,.5,.5};double[] source={x-.5,y-.5,z-.5};
                var axes=new net.minecraft.core.Direction[]{PumpOrientation.side(state,ROLL,net.minecraft.core.Direction.EAST),PumpOrientation.side(state,ROLL,net.minecraft.core.Direction.UP),state.getValue(FACING).getOpposite()};
                for(int i=0;i<3;i++){v[0]+=source[i]*axes[i].getStepX();v[1]+=source[i]*axes[i].getStepY();v[2]+=source[i]*axes[i].getStepZ();}
                for(int i=0;i<3;i++){min[i]=Math.min(min[i],v[i]);max[i]=Math.max(max[i],v[i]);}
            }
            result=net.minecraft.world.phys.shapes.Shapes.or(result,net.minecraft.world.phys.shapes.Shapes.box(min[0],min[1],min[2],max[0],max[1],max[2]));
        }
        return result;
    }
    public static net.minecraft.world.phys.shapes.VoxelShape strokeShape(float stroke){
        double top=15-2.2*net.minecraft.util.Mth.clamp(stroke,0,1);
        return net.minecraft.world.phys.shapes.Shapes.or(box(1,0,1,15,2,15),box(2,2,2,14,top,14));
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return getShape(state,level,pos,context);}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getInteractionShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos){return getShape(state,level,pos,net.minecraft.world.phys.shapes.CollisionContext.empty());}
    @Override public BlockState getRotatedBlockState(BlockState state,net.minecraft.core.Direction clickedFace) {
        return PumpOrientation.rotate(state,ROLL,clickedFace);
    }
    @Override public net.minecraft.world.level.block.entity.BlockEntityType<? extends com.simibubi.create.content.fluids.pump.PumpBlockEntity> getBlockEntityType() {
        return CopperDynamics.PORTABLE_PUMP_BE.get();
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        return operate(level,pos,player,player.getMainHandItem())?InteractionResult.sidedSuccess(level.isClientSide()):InteractionResult.PASS;
    }
    private boolean operate(Level level,BlockPos pos,Player player,net.minecraft.world.item.ItemStack held) {
        if(player.isSpectator()||!player.mayBuild())return false;
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof PortableFluidPumpBlockEntity pump) {
            pump.turn(player.isShiftKeyDown());
            // Match the crank's Extendo Grip exemption and configured hunger cost.
            if(!held.is(com.simibubi.create.AllItems.EXTENDO_GRIP.get()))
                player.causeFoodExhaustion(32*com.simibubi.create.infrastructure.config.AllConfigs.server().kinetics.crankHungerMultiplier.getF());
            if(player.getFoodData().getFoodLevel()==0)
                com.simibubi.create.foundation.advancement.AllAdvancements.HAND_CRANK.awardTo(player);
        }
        return true;
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,net.minecraft.world.InteractionHand hand,BlockHitResult hit) {
        if(stack.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_WRENCH)) {
            var context=new net.minecraft.world.item.context.UseOnContext(player,hand,hit);
            var result=player.isShiftKeyDown()?onSneakWrenched(state,context):onWrenched(state,context);
            return result.consumesAction()?net.minecraft.world.ItemInteractionResult.SUCCESS:net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return operate(level,pos,player,stack)?net.minecraft.world.ItemInteractionResult.SUCCESS
            :net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
