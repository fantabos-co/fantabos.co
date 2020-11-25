package com.matt.forgehax.asm.events;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class ITextComponentClickEvent extends Event {

	public GuiScreen guiScreen;
	public ITextComponent textComponent;
	public ClickEvent clickEvent;
	
	public ITextComponentClickEvent(GuiScreen screen, ITextComponent comp, ClickEvent clickEvent) {
		guiScreen = screen;
		textComponent = comp;
		this.clickEvent = clickEvent;
	}
	
}
