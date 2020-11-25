package com.matt.forgehax.mods;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.HudMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.math.AlignHelper.Align;
import com.matt.forgehax.util.color.Color;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.matt.forgehax.asm.events.PacketEvent;
import net.minecraft.network.play.server.SPacketTimeUpdate;

@RegisterMod
public class Watermark extends HudMod {

  public Watermark() {
    super(Category.GUI, "Watermark", true, "Display a watermark on your screen");
  }

  private int color = 0;

  private final Setting<Integer> alpha =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("alpha")
          .description("Transparency, 0-255")
          .min(0)
          .max(255)
          .defaultTo(255)
          .build();
  private final Setting<Integer> red =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("red")
          .description("Red amount, 0-255")
          .min(0)
          .max(255)
          .defaultTo(191)
          .build();
  private final Setting<Integer> green =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("green")
          .description("Green amount, 0-255")
          .min(0)
          .max(255)
          .defaultTo(97)
          .build();
  private final Setting<Integer> blue =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("blue")
          .description("Blue amount, 0-255")
          .min(0)
          .max(255)
          .defaultTo(106)
          .build();

  private final Setting<Boolean> rainbow =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("rainbow")
      .description("Change color every 4 ticks")
      .defaultTo(false)
      .build();

  @Override
  protected Align getDefaultAlignment() { return Align.TOPLEFT; }
  @Override
  protected int getDefaultOffsetX() { return 10; }
  @Override
  protected int getDefaultOffsetY() { return 5; }
  @Override
  protected double getDefaultScale() { return 2d; }

  @SubscribeEvent
  public void onPacketPreceived(PacketEvent.Incoming.Pre event) {
    if (event.getPacket() instanceof SPacketTimeUpdate) {
	  int r, g, b;
	  r = (int) (Math.random() * 255);
	  g = (int) (Math.random() * 255);
	  b = (int) (Math.random() * 255);
	  color = Color.of(r, g, b, (int) 255).toBuffer();
    }
  }

  @SubscribeEvent
  public void onRenderScreen(RenderGameOverlayEvent.Text event) {
	  int align = alignment.get().ordinal();	
	  int clr;
	  if (rainbow.get()) clr = color;
	  else clr = Color.of(red.get(), green.get(), blue.get(), alpha.get()).toBuffer();
      SurfaceHelper.drawTextAlign("\u16A0\u16A8\u16BE\u16CF\u16A8\u16D2\u16DF\u16CA\u16EB\u16B2\u16DF", getPosX(0), getPosY(0),
	  							clr, scale.get(), true, align);
  }
}

