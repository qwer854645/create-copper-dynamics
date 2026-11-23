package dev.ccd.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/** Fixed six-neighbour contact checks, shared by source and flowing liquid states. */
public final class MossSuspensionBlock extends LiquidBlock {
    public MossSuspensionBlock(FlowingFluid fluid,Properties properties){super(fluid,properties);}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        super.onPlace(state,level,pos,old,moving);
        if(!level.isClientSide())level.scheduleTick(pos,this,1);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){
        super.neighborChanged(state,level,pos,block,from,moving);
        if(!level.isClientSide())level.scheduleTick(pos,this,1);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!dev.ccd.CCDFeatures.enabled(state))return;
        for(var side:Direction.values())MossificationEffect.spreadAt(level,pos.relative(side));
    }
}
