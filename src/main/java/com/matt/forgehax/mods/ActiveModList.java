package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getCurrentScreen;
import static com.matt.forgehax.Helper.getModManager;

import com.matt.forgehax.gui.PromptGui;
import com.matt.forgehax.mods.services.RainbowService;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.math.AlignHelper.Align;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.mod.BaseMod;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ListMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.*;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class ActiveModList extends ListMod {

  private final Setting<Color> color =
          getCommandStub()
                  .builders()
                  .newSettingColorBuilder()
                  .name("color")
                  .description("activemodlist color")
                  .defaultTo(Color.of(255, 255, 255, 1))
                  .build();

  private final Setting<Boolean> showDebugText =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("debug")
          .description("Enables debug text on mods that have it")
          .defaultTo(false)
          .build();

  private final Setting<Boolean> showServiceMods =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("show-service-mod")
          .description("Shows service mods count when mod list is compressed")
          .defaultTo(false)
          .build();

  private final Setting<Boolean> condense =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("condense")
          .description("Condense ModList when chat is open")
          .defaultTo(false)
          .build();

  private final Setting<Boolean> rainbow =
          getCommandStub()
                  .builders()
                  .<Boolean>newSettingBuilder()
                  .name("rainbow")
                  .description("Change color")
                  .defaultTo(false)
                  .build();

  public ActiveModList() {
    super(Category.GUI, "ActiveMods", true, "Shows list of all active mods");
  }

  @Override
  public boolean isInfoDisplayElement() {
    return false;
  }

  @Override
  protected Align getDefaultAlignment() {
    return Align.BOTTOMRIGHT;
  }

  @Override
  protected int getDefaultOffsetX() {
    return 1;
  }

  @Override
  protected int getDefaultOffsetY() {
    return 1;
  }

  @Override
  protected double getDefaultScale() {
    return 1d;
  }

  @Override
  public boolean isVisible() {
    return false;
  } // default false

  @SubscribeEvent(priority = EventPriority.HIGH)
  public void onRenderScreen(RenderGameOverlayEvent.Text event) {
    int clr;
    if (rainbow.get()) clr = RainbowService.getRainbowColor();
    else clr = color.get().toBuffer();
    List<String> text = new ArrayList<>();

    if ((condense.get() && getCurrentScreen() instanceof GuiChat)
        || MC.gameSettings.showDebugInfo
        || getCurrentScreen() instanceof PromptGui) {

      // Total number of service mods
      long serviceMods = getModManager()
          .getMods()
          .stream()
          .filter(BaseMod::isHidden)
          .count();

      // Total number of mods in the client
      long totalMods = getModManager()
          .getMods()
          .stream()
          .filter(mod -> !mod.isHidden())
          .count();

      // Mods that are enabled
      long enabledMods = getModManager()
          .getMods()
          .stream()
          .filter(BaseMod::isEnabled)
          .filter(mod -> !mod.isHidden())
          .count();

      text.add(enabledMods + "/" + totalMods + " mods enabled");
      if (showServiceMods.get()) {
        text.add(serviceMods + " service mods");
      }
    } else {
      getModManager()
          .getMods()
          .stream()
          .filter(BaseMod::isEnabled)
          .filter(mod -> !mod.isHidden())
          .filter(mod -> !mod.isInfoDisplayElement())
          .filter(BaseMod::isVisible) // prints only visible mods
          .map(mod -> showDebugText.get() ? mod.getDebugDisplayText() : mod.getDisplayText())
          .sorted(sortMode.get().getComparator())
          .map(super::appendArrow)
          .forEach(text::add);
    }
    // Prints on screen
    SurfaceHelper.drawTextAlign(text, getPosX(0), getPosY(0),
        clr, scale.get(), true, alignment.get().ordinal());
  }
}
