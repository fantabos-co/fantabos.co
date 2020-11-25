package com.matt.forgehax.mods.services;

import java.util.HashMap;

import com.matt.forgehax.asm.events.ITextComponentClickEvent;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * TheAlphaEpsilon
 * 24NOV2020
 */

@RegisterMod
public class CustomTextComponentClickEvent extends ServiceMod {

	public CustomTextComponentClickEvent() {
		super("CustomTextComponentClickEvent");
	}
	
	//TODO: A more elegant implementation because this can cause memory leaks if too many
	//Are created
	private static long counter;
	
	private static HashMap<ClickEvent, Runnable> actionMap = new HashMap<>();
	
	public static ClickEvent createCustomEvent(Runnable action) {
		ClickEvent event = new ClickEventFlag();
		actionMap.put(event, action);
		return event;
	}

	@SubscribeEvent
	public void onClick(ITextComponentClickEvent event) {
		Runnable action = actionMap.get(event.clickEvent);
		if(action != null) {
			event.setCanceled(true);
			action.run();
		}
	}
	
	static class ClickEventFlag extends ClickEvent {

		private long num = counter++;
		
		public ClickEventFlag() {
			super(Action.CHANGE_PAGE, "");
		}
		
		@Override
		public boolean equals(Object other) {
			if(this == other) {
				return true;
			} else if (other == null) {
				return false;
			} else if (!(other instanceof ClickEventFlag)) {
				return false;
			} else {
				return this.num == ((ClickEventFlag)other).num;
			}
		}
		
	}
	
}
