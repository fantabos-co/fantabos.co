package com.matt.forgehax.mods.services;

import java.util.concurrent.atomic.AtomicInteger;

import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Created by TAE, made standalone by tonio
 */
@RegisterMod
public class RainbowService extends ServiceMod {

  private static Setting<Float> saturation;
  private static Setting<Float> brightness;
  private static Setting<Float> speed;

  // Make the references static for ease of access

  {
    saturation = getCommandStub()
        .builders()
        .<Float>newSettingBuilder()
        .name("saturation")
        .description("Rainbow saturation")
        .defaultTo(1F)
        .min(0F)
        .max(1F)
        .build();

    brightness = getCommandStub()
        .builders()
        .<Float>newSettingBuilder()
        .name("brightness")
        .description("Rainbow brightness")
        .defaultTo(1F)
        .min(0F)
        .max(1F)
        .build();

    speed = getCommandStub()
        .builders()
        .<Float>newSettingBuilder()
        .name("speed")
        .description("Rainbow speed")
        .defaultTo(1F)
        .min(0F)
        .max(10F)
        .build();
  }
  
  public RainbowService() {
    super("RainbowService", "Provide global and synchronized rainbow color");
    time.set(0);
  }

  private static AtomicInteger time = new AtomicInteger();

  public static Color getRainbowColorClass() {
    return Color.of(getRainbowColor());
  }

	public static int getRainbowColor() {
		float hue = (float)(time.get() * speed.get())/ 360;
		int clr = java.awt.Color.HSBtoRGB(hue, saturation.get(), brightness.get());
		return clr;
	}	
  
  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if (time.incrementAndGet() * speed.get() > 360)
      time.set(0);
  }
}
