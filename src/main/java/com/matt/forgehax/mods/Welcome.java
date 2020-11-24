package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.HudMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.math.AlignHelper.Align;
import com.matt.forgehax.util.color.Color;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import com.matt.forgehax.mods.services.RainbowService;


@RegisterMod
public class Welcome extends HudMod {
  //Coded by Fleyr on 4th June 2020

  public Welcome() {
    super(Category.GUI, "Welcome", true, "Display some text and your name on screen");
  }

  private final Setting<Color> color =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color")
      .description("Text color")
      .defaultTo(Color.of(191, 97, 106, 255))
      .build();

  private final Setting<Boolean> rainbow =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("rainbow")
      .description("Change color")
      .defaultTo(true)
      .build();

  private final Setting<String> preName =
    getCommandStub()
      .builders()
      .<String>newSettingBuilder()
      .name("preName")
      .description("The text before the name displayed")
      .defaultTo("Welcome, ")
      .build();

  private final Setting<String> afterName =
    getCommandStub()
      .builders()
      .<String>newSettingBuilder()
      .name("afterName")
      .description("The text after the name displayed")
      .defaultTo("")
      .build();
  
  @Override
  protected Align getDefaultAlignment() { return Align.TOP; }
  @Override
  protected int getDefaultOffsetX() { return 0; }
  @Override
  protected int getDefaultOffsetY() { return 5; }
  @Override
  protected double getDefaultScale() { return 1d; }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public void onRenderScreen(RenderGameOverlayEvent.Text event) {
	int align = alignment.get().ordinal();	
  int c;
  String text = preName.get()+getLocalPlayer().getName()+afterName.get();
	if (rainbow.get()) c = RainbowService.getRainbowColor();
  else c = color.get().toBuffer();
  
  SurfaceHelper.drawTextAlign(text, getPosX(0), getPosY(0),
								c, scale.get(), true, align);
  }
}
