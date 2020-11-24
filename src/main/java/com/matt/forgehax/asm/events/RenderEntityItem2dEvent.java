package com.matt.forgehax.asm.events;

import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class RenderEntityItem2dEvent extends Event {

	public RenderItem renderItem;
	public ItemStack stack;
	public IBakedModel transformedModel;
	
	public RenderEntityItem2dEvent(RenderItem renderItem, ItemStack stack, IBakedModel transformedModel) {
		this.renderItem = renderItem;
		this.stack = stack;
		this.transformedModel = transformedModel;
	}

}
