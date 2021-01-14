package com.matt.forgehax.mods.commands;

import com.matt.forgehax.Helper;
import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.CommandBuilders;
import com.matt.forgehax.util.mod.CommandMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

@RegisterMod
public class TrollModeCommand extends CommandMod {
  
  public TrollModeCommand() {
    super("TrollModeCommand");
  }
  
  @RegisterCommand
  public Command nbthack(CommandBuilders builders) {
    return builders
      .newCommandBuilder()
      .name("nbthack")
      .description("This is dumb")
      .processor(
        data -> {
          
            Helper.printInform("This command was successfully called in game.");
        	
        })
      .build();
  }
}
