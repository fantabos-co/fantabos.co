package com.matt.forgehax.asm.events;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class RenderItemAndEffectIntoGuiEvent extends Event {

	private ItemStack stack;
	private int x;
	private int y;
	
	/**
	 * This event is super scuffed. DO NOT USE UNLESS YOU MAKE SURE THAT BOTH THE WORLD AND PLAYER ARE NOT NULL.
	 * That should only happen after everything else is rendered properly. <3 TheAlphaEpsilon
	 */
	public RenderItemAndEffectIntoGuiEvent(ItemStack stack, int x, int y) {
		this.stack = stack;
		this.x = x;
		this.y = y;
	}
	
	public ItemStack getStack() {
		return stack;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
}
