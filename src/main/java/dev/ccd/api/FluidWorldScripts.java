package dev.ccd.api;

import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/** Directional pair rules: source/flowing replace the first fluid; downward replaces the second. */
public final class FluidWorldScripts {
    private FluidWorldScripts(){}
    private record Rule(String id,net.minecraft.world.level.material.Fluid first,net.minecraft.world.level.material.Fluid second,BlockState source,BlockState flowing,BlockState downward){}
    private static final java.util.Map<String,Rule> RULES=new ConcurrentHashMap<>();
    private record BlockRule(String id,net.minecraft.world.level.material.Fluid fluid,
        net.minecraft.world.level.block.Block target,net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag,
        String mode,BlockState fluidResult,BlockState blockResult){
        boolean matches(BlockState state){return tag==null?state.is(target):state.is(tag);}
    }
    private static final java.util.Map<String,BlockRule> BLOCK_RULES=new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> APPLYING=ThreadLocal.withInitial(()->false);
    private static net.minecraft.world.level.material.Fluid fluid(String id){
        var key=ResourceLocation.parse(id);if(!BuiltInRegistries.FLUID.containsKey(key))throw new IllegalArgumentException("Unknown fluid: "+id);
        var fluid=BuiltInRegistries.FLUID.get(key);if(fluid==net.minecraft.world.level.material.Fluids.EMPTY)throw new IllegalArgumentException("Empty fluid cannot interact");return fluid;
    }
    private static BlockState block(String id){
        if(id==null||id.isEmpty())return null;
        var key=ResourceLocation.parse(id);if(!BuiltInRegistries.BLOCK.containsKey(key))throw new IllegalArgumentException("Unknown block: "+id);
        return BuiltInRegistries.BLOCK.get(key).defaultBlockState();
    }
    public static void interaction(String id,String first,String second,String sourceResult,String flowingResult,String downwardResult){
        var key=ResourceLocation.parse(id).toString();
        var a=fluid(first);var b=fluid(second);
        if(a.isSame(b))throw new IllegalArgumentException("Interaction requires different fluid families");
        var source=block(sourceResult);var flowing=block(flowingResult);var downward=block(downwardResult);
        if(source==null&&flowing==null&&downward==null){RULES.remove(key);return;}
        RULES.put(key,new Rule(key,a,b,source,flowing,downward));
    }
    /** mode: both, source or flowing. Null result keeps that position unchanged. */
    public static void blockInteraction(String id,String fluidId,String target,String mode,String fluidResult,String blockResult){
        var key=ResourceLocation.parse(id).toString();
        if(!java.util.Set.of("both","source","flowing").contains(mode))throw new IllegalArgumentException("Unknown fluid contact mode: "+mode);
        var first=fluid(fluidId);
        net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> tag=null;
        net.minecraft.world.level.block.Block targetBlock=null;
        if(target.startsWith("#"))tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,ResourceLocation.parse(target.substring(1)));
        else {var parsed=block(target);if(parsed==null)throw new IllegalArgumentException("A contact target is required");targetBlock=parsed.getBlock();}
        var a=block(fluidResult);var b=block(blockResult);
        if(a==null&&b==null){BLOCK_RULES.remove(key);return;}
        BLOCK_RULES.put(key,new BlockRule(key,first,targetBlock,tag,mode,a,b));
    }
    public static void remove(String id){var key=ResourceLocation.parse(id).toString();RULES.remove(key);BLOCK_RULES.remove(key);}
    public static void clear(){RULES.clear();BLOCK_RULES.clear();}
    public static boolean blockContact(net.minecraft.world.level.Level level,BlockPos pos){
        if(level.isClientSide()||BLOCK_RULES.isEmpty()||APPLYING.get())return false;
        var fluidState=level.getFluidState(pos);if(fluidState.isEmpty())return false;
        for(var rule:BLOCK_RULES.values().stream().sorted(java.util.Comparator.comparing(BlockRule::id)).toList()){
            if(!rule.fluid.isSame(fluidState.getType())||rule.mode.equals("source")&&!fluidState.isSource()
                ||rule.mode.equals("flowing")&&fluidState.isSource())continue;
            for(var side:Direction.values()){
                var target=pos.relative(side);if(!level.hasChunkAt(target)||!rule.matches(level.getBlockState(target)))continue;
                // Resolve event-adjusted results before either write. Suppress recursive rule
                // execution while neighbour updates observe a two-position transformation.
                APPLYING.set(true);
                try{
                    var a=rule.fluidResult==null?null:net.neoforged.neoforge.event.EventHooks.fireFluidPlaceBlockEvent(level,pos,pos,rule.fluidResult);
                    var b=rule.blockResult==null?null:net.neoforged.neoforge.event.EventHooks.fireFluidPlaceBlockEvent(level,target,pos,rule.blockResult);
                    boolean changed=false;
                    if(a!=null&&!a.equals(level.getBlockState(pos)))changed|=level.setBlock(pos,a,3);
                    if(b!=null&&!b.equals(level.getBlockState(target)))changed|=level.setBlock(target,b,3);
                    if(changed){level.levelEvent(1501,pos,0);return true;}
                }finally{APPLYING.set(false);}
            }
        }
        return false;
    }
    private static java.util.List<Rule> rules(){return RULES.values().stream().sorted(java.util.Comparator.comparing(Rule::id)).toList();}
    private static void replace(LevelAccessor level,BlockPos pos,BlockPos origin,BlockState result){
        level.setBlock(pos,net.neoforged.neoforge.event.EventHooks.fireFluidPlaceBlockEvent(level,pos,origin,result),3);
        level.levelEvent(1501,pos,0);
    }
    public static boolean adjacent(net.minecraft.world.level.Level level,BlockPos pos){
        if(level.isClientSide()||RULES.isEmpty()||APPLYING.get())return false;
        var state=level.getFluidState(pos);if(state.isEmpty())return false;
        for(var rule:rules())if(rule.first.isSame(state.getType())){
            var result=state.isSource()?rule.source:rule.flowing;if(result==null)continue;
            for(var direction:Direction.values()){
                if(direction==Direction.DOWN)continue;
                var other=pos.relative(direction);if(!level.hasChunkAt(other))continue;
                if(rule.second.isSame(level.getFluidState(other).getType())){replace(level,pos,pos,result);return true;}
            }
        }
        return false;
    }
    public static boolean downward(LevelAccessor level,BlockPos target,Direction direction,FluidState incoming){
        if(direction!=Direction.DOWN||RULES.isEmpty()||APPLYING.get()||!level.hasChunkAt(target))return false;
        var other=level.getFluidState(target);if(other.isEmpty())return false;
        for(var rule:rules())if(rule.downward!=null&&rule.first.isSame(incoming.getType())&&rule.second.isSame(other.getType())){
            replace(level,target,target.above(),rule.downward);return true;
        }
        return false;
    }
}
