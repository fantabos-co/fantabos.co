package com.matt.forgehax.asm.events;

import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class RenderEnderCrystalEvent extends Event {

	public ModelBase model;
	public EntityEnderCrystal crystal;
	public float height;
	public float rotation;
	
	public RenderEnderCrystalEvent(ModelBase model, EntityEnderCrystal crystal, float height, float rotation) {
		
		this.model = model;
		this.crystal = crystal;
		this.height = height;
		this.rotation = rotation;
		
	}
	
}
