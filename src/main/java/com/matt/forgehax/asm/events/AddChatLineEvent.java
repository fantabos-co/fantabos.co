package com.matt.forgehax.asm.events;

import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Created on 14/09/2020 by Tonio
 */
public class AddChatLineEvent extends Event {
  
  private ITextComponent msg;
  
  public AddChatLineEvent(ITextComponent msg) {
    this.msg = msg;
  }
  
  public ITextComponent getMessage() {
    return this.msg;
  }
  
  public void setMessage(ITextComponent newMessage) {
    this.msg = newMessage;
  }
}
