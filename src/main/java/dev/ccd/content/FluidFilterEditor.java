package dev.ccd.content;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.fluids.FluidStack;

/** Shared optimistic-client/server edits, including stable sparse sample positions. */
public final class FluidFilterEditor {
    private FluidFilterEditor(){}
    public static int position(CompoundTag entry,int legacyIndex){return entry.contains("slot")?entry.getInt("slot"):legacyIndex;}
    public static boolean apply(CompoundTag data,boolean attributes,HolderLookup.Provider registries,int operation,int slot,FluidStack sample,int choice){
        if(!dev.ccd.CCDFeatures.enabled(attributes?"fluid_attribute_filter":"fluid_filter"))return false;
        var entries=data.getList("entries",Tag.TAG_COMPOUND).copy();
        if(operation==0||operation==6){
            if(slot<0||slot>=FluidFilterRules.SLOTS||sample.isEmpty())return false;
            var entry=new CompoundTag();
            if(attributes){
                var options=FluidFilterRules.choices(sample);
                if(choice<0||choice>=options.size()||entries.size()>=FluidFilterRules.SLOTS)return false;
                entry=options.get(choice).copy();
                for(int i=0;i<entries.size();i++)if(entries.getCompound(i).getString("kind").equals(entry.getString("kind"))&&entries.getCompound(i).getString("value").equals(entry.getString("value")))return false;
                entry.putBoolean("invert",operation==6);
            }else{
                for(int i=0;i<entries.size();i++){
                    var old=entries.getCompound(i);old.putInt("slot",position(old,i));
                }
                for(int i=entries.size()-1;i>=0;i--)if(position(entries.getCompound(i),i)==slot)entries.remove(i);
                entry.putInt("slot",slot);
            }
            entry.put("fluid",sample.copyWithAmount(1).saveOptional(registries));entries.add(entry);
        }else if(operation==1){
            if(attributes){if(slot<0||slot>=entries.size())return false;entries.remove(slot);}
            else{
                for(int i=0;i<entries.size();i++){var entry=entries.getCompound(i);entry.putInt("slot",position(entry,i));}
                for(int i=entries.size()-1;i>=0;i--)if(position(entries.getCompound(i),i)==slot)entries.remove(i);
            }
        }else if(operation==2)data.putBoolean("blacklist",choice!=0);
        else if(operation==3)data.putBoolean(attributes?"all":"components",choice!=0);
        else if(operation==5)entries.clear();
        else return false;
        data.put("entries",entries);return true;
    }
}
