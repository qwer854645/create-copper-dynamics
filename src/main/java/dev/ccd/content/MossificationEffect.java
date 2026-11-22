package dev.ccd.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;

/** Contact seeds use the vanilla replaceable tag; decoration/spread uses vanilla bonemeal. */
public final class MossificationEffect extends MobEffect {
    public MossificationEffect(){super(MobEffectCategory.NEUTRAL,0x668b36);}
    @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){return true;}
    @Override public boolean applyEffectTick(LivingEntity entity,int amplifier){
        if(!(entity.level() instanceof ServerLevel level))return true;
        var bounds=entity.getBoundingBox().inflate(.01);
        for(var cursor:BlockPos.betweenClosed(BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ),BlockPos.containing(bounds.maxX,bounds.maxY,bounds.maxZ))){
            if(!level.hasChunkAt(cursor))continue;
            var state=level.getBlockState(cursor);
            if(!state.is(BlockTags.MOSS_REPLACEABLE))continue;
            var shape=state.getCollisionShape(level,cursor);
            boolean touching=shape.toAabbs().stream().anyMatch(box->box.move(cursor).intersects(bounds));
            if(!touching)continue;
            var pos=cursor.immutable();
            level.setBlockAndUpdate(pos,Blocks.MOSS_BLOCK.defaultBlockState());
            if(level.isEmptyBlock(pos.above())){
                ((BonemealableBlock)Blocks.MOSS_BLOCK).performBonemeal(level,level.random,pos,Blocks.MOSS_BLOCK.defaultBlockState());
                level.levelEvent(1505,pos,0);
            }
        }
        return true;
    }
}
