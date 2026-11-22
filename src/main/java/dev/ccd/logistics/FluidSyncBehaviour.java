package dev.ccd.logistics;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;

/** Coalesce updates without replacing capability types or existing tank save keys. */
public final class FluidSyncBehaviour extends BlockEntityBehaviour {
    public static final BehaviourType<FluidSyncBehaviour> TYPE=new BehaviourType<>();
    private boolean dirty;
    private int cooldown;
    public FluidSyncBehaviour(SmartBlockEntity owner){super(owner);}
    public static void changed(SmartBlockEntity owner){
        owner.setChanged();
        var behaviour=owner.getBehaviour(TYPE);
        if(behaviour!=null)behaviour.dirty=true;
    }
    @Override public void tick(){
        if(blockEntity.getLevel()==null||blockEntity.getLevel().isClientSide())return;
        if(cooldown>0)cooldown--;
        if(dirty&&cooldown==0){dirty=false;cooldown=8;blockEntity.sendData();}
    }
    @Override public BehaviourType<?> getType(){return TYPE;}
}
