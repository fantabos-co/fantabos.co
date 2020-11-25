package com.matt.forgehax.mods.services;

import java.util.HashMap;
import java.util.LinkedList;

import com.matt.forgehax.asm.events.ITextComponentClickEvent;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.gui.GuiChat;
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
	
	//TODO: A more elegant implementation
	private static long counter;
	
	private static LinkedList<ClickEvent> chatQueue = new LinkedList<>();
	private static HashMap<ClickEvent, Runnable> chatMap = new HashMap<>();
	private static HashMap<ClickEvent, Runnable> staticMap = new HashMap<>();
	
	/**
	 * Use this method to create static events (never deleted). Only use for
	 * static or other one-time events to prevent memory leaks.
	 */
	public static ClickEvent createStaticCustomEvent(Runnable action) {
		ClickEvent event = new ClickEventFlag();
		staticMap.put(event, action);
		return event;
	}

	/**
	 * Use this method to create chat events. You must print the chat message to
	 * the chat gui before calling this method again to avoid errors.
	 */
	public static ClickEvent createChatCustomEvent(Runnable action) {
		ClickEvent event = new ClickEventFlag();
		chatQueue.add(event);
		chatMap.put(event, action);
		
		try {
			while(chatQueue.size() > MC.ingameGUI.getChatGUI().getSentMessages().size() + 1) {
				chatMap.remove(chatQueue.pop());
			}
		} catch (Exception e) {
			LOGGER.error("Error in custom click event: " + e.getClass().toString() + ": " + e.getMessage());
		}
		
		return event;
	}
	
	@SubscribeEvent
	public void onClick(ITextComponentClickEvent event) {
		Runnable action = staticMap.get(event.clickEvent);
		if(action == null) {
			action = chatMap.get(event.clickEvent);
		}
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
