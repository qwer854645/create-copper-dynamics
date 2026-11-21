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
    public static void remove(String id){RULES.remove(ResourceLocation.parse(id).toString());}
    public static void clear(){RULES.clear();}
    private static java.util.List<Rule> rules(){return RULES.values().stream().sorted(java.util.Comparator.comparing(Rule::id)).toList();}
    private static void replace(LevelAccessor level,BlockPos pos,BlockPos origin,BlockState result){
        level.setBlock(pos,net.neoforged.neoforge.event.EventHooks.fireFluidPlaceBlockEvent(level,pos,origin,result),3);
        level.levelEvent(1501,pos,0);
    }
    public static boolean adjacent(net.minecraft.world.level.Level level,BlockPos pos){
        if(level.isClientSide()||RULES.isEmpty())return false;
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
        if(direction!=Direction.DOWN||RULES.isEmpty()||!level.hasChunkAt(target))return false;
        var other=level.getFluidState(target);if(other.isEmpty())return false;
        for(var rule:rules())if(rule.downward!=null&&rule.first.isSame(incoming.getType())&&rule.second.isSame(other.getType())){
            replace(level,target,target.above(),rule.downward);return true;
        }
        return false;
    }
}
