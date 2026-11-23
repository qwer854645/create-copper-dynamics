package dev.ccd.api;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

/** Explicit mappings first, then documented opt-in tags; never probes a fabricated world. */
public final class FluidProcessingMappings {
    private FluidProcessingMappings(){}
    public record Mapping(ResourceLocation id,String fluid,ResourceLocation type,int priority){}
    private static final Map<ResourceLocation,Mapping> API=new HashMap<>(),SCRIPTS=new HashMap<>();
    private static List<Mapping> data=List.of();
    private static volatile List<Mapping> ordered=List.of();
    private static void rebuild(){
        var merged=new HashMap<ResourceLocation,Mapping>();
        data.forEach(m->merged.put(m.id(),m));merged.putAll(API);merged.putAll(SCRIPTS);
        ordered=merged.values().stream().sorted(Comparator.comparingInt(Mapping::priority).reversed().thenComparing(Mapping::id)).toList();
        dev.ccd.atomizing.AtomizingRules.invalidate();
    }
    private static Mapping mapping(String id,String fluid,String type,int priority){
        Objects.requireNonNull(fluid);
        ResourceLocation.parse(fluid.startsWith("#")?fluid.substring(1):fluid);
        return new Mapping(ResourceLocation.parse(id),fluid,ResourceLocation.parse(type),priority);
    }
    public static synchronized void register(String id,String fluid,String type,int priority){var m=mapping(id,fluid,type,priority);API.put(m.id(),m);rebuild();}
    public static synchronized void remove(String id){API.remove(ResourceLocation.parse(id));rebuild();}
    public static synchronized void script(String id,String fluid,String type,int priority){var m=mapping(id,fluid,type,priority);SCRIPTS.put(m.id(),m);rebuild();}
    public static synchronized void removeScript(String id){SCRIPTS.remove(ResourceLocation.parse(id));rebuild();}
    public static synchronized void clearScripts(){SCRIPTS.clear();rebuild();}
    public static synchronized void clearWorld(){SCRIPTS.clear();data=List.of();rebuild();}
    public static synchronized void reload(ResourceManager resources){
        var next=new ArrayList<Mapping>();
        resources.listResources("ccd/fluid_processing",p->p.getPath().endsWith(".json")).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
            try(var reader=entry.getValue().openAsReader()){
                var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                if(json.has("enabled")&&!json.get("enabled").getAsBoolean())return;
                var path=entry.getKey().getPath();
                var id=ResourceLocation.fromNamespaceAndPath(entry.getKey().getNamespace(),path.substring("ccd/fluid_processing/".length(),path.length()-5));
                next.add(mapping(id.toString(),json.get("fluid").getAsString(),json.get("processing_type").getAsString(),json.has("priority")?json.get("priority").getAsInt():0));
            }catch(Exception error){org.slf4j.LoggerFactory.getLogger("CCD-FluidMappings").error("Invalid fluid processing mapping {}",entry.getKey(),error);}
        });
        data=List.copyOf(next);rebuild();
    }
    public static FanProcessingType select(FluidStack stack){
        if(stack.isEmpty())return null;
        for(var rule:ordered){
            var type=type(rule.type());if(type==null)continue;
            boolean matches;
            if(rule.fluid().startsWith("#"))matches=stack.getFluid().builtInRegistryHolder().is(TagKey.create(Registries.FLUID,ResourceLocation.parse(rule.fluid().substring(1))));
            else{
                var id=ResourceLocation.parse(rule.fluid());
                matches=net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(id)&&stack.getFluid().isSame(net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id));
            }
            if(matches)return type;
        }
        return null;
    }
    public static FanProcessingType discover(FluidStack stack){
        if(stack.isEmpty())return null;
        // One opt-in convention only: #namespace:fan_processing_catalysts/<registered type path>.
        return stack.getFluid().builtInRegistryHolder().tags().map(TagKey::location)
            .filter(id->id.getPath().startsWith("fan_processing_catalysts/"))
            .sorted().map(id->type(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),id.getPath().substring("fan_processing_catalysts/".length()))))
            .filter(Objects::nonNull).findFirst().orElse(null);
    }
    private static FanProcessingType type(ResourceLocation id){
        if(!CreateBuiltInRegistries.FAN_PROCESSING_TYPE.containsKey(id)||id.equals(ResourceLocation.fromNamespaceAndPath("ccd","atomizing")))return null;
        return CreateBuiltInRegistries.FAN_PROCESSING_TYPE.get(id);
    }
}
