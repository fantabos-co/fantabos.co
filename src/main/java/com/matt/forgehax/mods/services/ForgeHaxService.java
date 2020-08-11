package com.matt.forgehax.mods.services;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

@RegisterMod
public class ForgeHaxService extends ServiceMod {

  public ForgeHaxService() {
    super("FHSettings");
    INSTANCE = this;
  }

  public static ForgeHaxService INSTANCE;

  public final Setting<Boolean> toggleMsgs =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("toggle-msgs")
          .description("Enables toggle messages in chat")
          .defaultTo(true)
          .build();
}
