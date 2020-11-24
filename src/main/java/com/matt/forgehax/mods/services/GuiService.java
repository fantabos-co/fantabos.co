package com.matt.forgehax.mods.services;

import java.util.List;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.matt.forgehax.Helper;
import com.matt.forgehax.gui.ClickGui;
import com.matt.forgehax.gui.windows.GuiWindow;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.command.Options;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.command.StubBuilder;
import com.matt.forgehax.util.command.callbacks.CallbackData;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.serialization.ISerializableJson;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.ScaledResolution;



/**
 * Created by Babbaj on 9/10/2017.
 */
@RegisterMod
public class GuiService extends ServiceMod {

    public final Options<WindowPosition> windows =
      getCommandStub()
          .builders()
          .<WindowPosition>newOptionsBuilder()
          .name("windows")
          .description("used to save the window positions")
          .supplier(Sets::newConcurrentHashSet)
          .factory(WindowPosition::new)
          .build();

  public final Setting<Color> color =
    getCommandStub()
      .builders()
      .newSettingColorBuilder()
      .name("color")
      .description("GUI color")
      .defaultTo(Color.of(191, 97, 106, 255))
      .build();

  public final Setting<Float> max_height =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("height")
          .description("Max percent of the screen, from 0 to 1")
          .min(0F)
          .max(1F)
          .defaultTo(0.75F)
          .build();
  
  public GuiService() {
    super("ClickGUI", "Configuration for ClickGui");
  }

  @Override
  protected void onLoad() {
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("un-hide")
        .description("Sets all windows to visible")
        .processor(
            data -> {
              for (GuiWindow g : ClickGui.getInstance().windowList)
                g.isHidden = false;
            })
        .build();
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("hide")
        .description("Sets all windows to compressed")
        .processor(
            data -> {
              for (GuiWindow g : ClickGui.getInstance().windowList)
                g.isHidden = true;
            })
        .build();
    
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("reset")
        .description("Resets all positions")
        .processor(
            data -> {
              int size = ClickGui.getInstance().windowList.size();
              ScaledResolution scaledRes = ClickGui.scaledRes;
              List<GuiWindow> windowList = ClickGui.getInstance().windowList;
              for (int i = 0; i < ClickGui.getInstance().windowList.size(); i++) {
                // Calculate fresh if none is found
                final int x = (i + 3) / 2 * scaledRes.getScaledWidth() / (size - 2)
                    - windowList.get(i).width / 2;
                final int y = scaledRes.getScaledHeight() / 25 + order(i) * scaledRes.getScaledHeight() / 2;
          
                // Here check if the window goes offscreen, if true push it down all the others
                windowList.get(i).setPosition(x, y);
              }
            })
        .build();
  }

  private int order(final int i) {
    if(i < 2) {
      return 0;
    }
    return (i + 1) % 2; // Distance between windows
  }

  @Override
  public void onBindPressed(CallbackData cb) {
    if (Helper.getLocalPlayer() != null) {
      MC.displayGuiScreen(ClickGui.getInstance());
    }
  }
  
  @Override
  protected StubBuilder buildStubCommand(StubBuilder builder) {
    return builder
      .kpressed(this::onBindPressed)
      .kdown(this::onBindKeyDown)
      .bind(Keyboard.KEY_RSHIFT) // default to right shift
      ;
  }
  public static class WindowPosition implements ISerializableJson {
    public final String title;
    public int x=0, y=0;
    public boolean hidden = false;

    public WindowPosition(String title, int x, int y) {
      this.title = title;
      this.x = x;
      this.y = y;
    }

    public WindowPosition(String title) {
      this.title = title;
    }

    @Override
    public void serialize(JsonObject in) {
      JsonObject add = new JsonObject();
      for (GuiWindow w : ClickGui.getInstance().windowList) {
        if (w.title.equals(this.title)) {
          add.addProperty("x", w.posX);
          add.addProperty("y", w.headerY);
          add.addProperty("hidden", w.isHidden);
          break;
        }
      }
      in.add(title, add);
    }

    @Override
    public void deserialize(JsonObject in) {
      JsonObject from = in.getAsJsonObject(title);
      if (from == null) return;

      if (from.get("x") != null) this.x = from.get("x").getAsInt();
      if (from.get("y") != null) this.y = from.get("y").getAsInt();
      if (from.get("hidden") != null) this.hidden = from.get("hidden").getAsBoolean();
    }

    @Override
    public String getUniqueHeader() {
      return this.title;
    }

    @Override
    public String toString() {
      return getUniqueHeader();
    }

  }
  
}
