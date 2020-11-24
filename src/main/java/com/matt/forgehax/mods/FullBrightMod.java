package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.potion.PotionEffect;
import net.minecraft.init.MobEffects;

@RegisterMod
public class FullBrightMod extends ToggleMod {
  
  public FullBrightMod() {
    super(Category.WORLD, "FullBright", false, "Makes everything render with maximum brightness");
  }

  public enum BrightMode {
    GAMMA,
    POTION
  }

  private final Setting<Float> defaultGamma =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("gamma")
          .description("default gamma to revert to")
          .defaultTo(MC.gameSettings.gammaSetting)
          .min(0.F)
          .max(16F)
          .defaultTo(0.F)
          .build();

  private final Setting<BrightMode> mode =
      getCommandStub()
          .builders()
          .<BrightMode>newSettingEnumBuilder()
          .name("mode")
          .description("mode for brightening [gamma/potion]")
          .defaultTo(BrightMode.GAMMA)
          .changed(cb -> {
            if (this.isEnabled()) {
              if (cb.getTo() == BrightMode.POTION) {
                MC.gameSettings.gammaSetting = defaultGamma.get();
              } else if (getLocalPlayer() != null) {
                getLocalPlayer().removePotionEffect(MobEffects.NIGHT_VISION);
              }
            }
          })
          .build();
  
  @Override
  public void onEnabled() {
    switch(mode.get()) {
      case GAMMA:
        MC.gameSettings.gammaSetting = 16F;
        break;
      case POTION:
        if (getLocalPlayer() != null) {
          getLocalPlayer().removePotionEffect(MobEffects.NIGHT_VISION);
          getLocalPlayer().addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 5200, 0));
        }
        break;
    }
  }
  
  @Override
  public void onDisabled() {
    switch(mode.get()) {
      case GAMMA:
        MC.gameSettings.gammaSetting = defaultGamma.get();
        break;
      case POTION:
        if (getLocalPlayer() != null) {
          getLocalPlayer().removePotionEffect(MobEffects.NIGHT_VISION);
        }
        break;
    }
  }
  
  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (getLocalPlayer() == null) return;
    switch(mode.get()) {
      case GAMMA:
        MC.gameSettings.gammaSetting = 16F;
        break;
      case POTION:
        if (getLocalPlayer() != null) {
          getLocalPlayer().addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 5204, 0));
        }
        break;
    }
  }
}
