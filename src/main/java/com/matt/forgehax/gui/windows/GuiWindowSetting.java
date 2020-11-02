package com.matt.forgehax.gui.windows;

import com.matt.forgehax.gui.ClickGui;
import com.matt.forgehax.gui.elements.GuiElement;
import com.matt.forgehax.gui.elements.GuiTextInput;
import com.matt.forgehax.gui.elements.GuiTextField;
import com.matt.forgehax.gui.elements.GuiToggle;
import com.matt.forgehax.gui.elements.GuiToggleEnum;
import com.matt.forgehax.gui.elements.GuiSlider;
import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.BaseMod;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.draw.SurfaceHelper;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Babbaj on 9/5/2017.
 */
public class GuiWindowSetting extends GuiWindow {

  // list of toggles, sliders, text inputs, etc.
  public List<GuiElement> inputList = new ArrayList<>();

  private final BaseMod mod;
  private final int x;
  private final int y;

  public GuiWindowSetting(BaseMod modIn, int xIn, int yIn) {
    super(modIn.getModName() + " Settings");
    this.mod = modIn;
    this.x = xIn;
    this.y = yIn;
    this.setPosition(xIn, yIn);
    initializeInputs();
  }

  private void initializeInputs() {
    Collection<Command> commands = getMod().getCommands();
    commands.forEach(command -> {
      try {
        final String settingName = command.getName();
        final Setting<?> setting = getMod().getSetting(settingName);
        GuiElement f;
        if (setting.getDefault() instanceof String)
          f = new GuiTextInput(setting, this);
        else if (setting.getDefault() instanceof Boolean)
          f = new GuiToggle(setting, this);
        else if (setting.getDefault() instanceof Float ||
                 setting.getDefault() instanceof Integer ||
                 setting.getDefault() instanceof Long ||
                 setting.getDefault() instanceof Double)
          f = new GuiSlider(setting, this);
        else if (setting.getDefault() instanceof Enum)
          f = new GuiToggleEnum(setting, this);
        else
          f = new GuiTextField(setting, this);
        f.subY = height;
        height += 12;
        inputList.add(f);
      } catch (Exception ignored) {
      }
    });
  }

  public String getModName() {
    return mod.getModName();
  }

  public BaseMod getMod() {
    return this.mod;
  }

  @Override
  public void drawTooltip(int mouseX, int mouseY) {
    int scale = ClickGui.scaledRes.getScaleFactor();

    if (isHidden){
      return;
    }

    if (mouseX >= posX && mouseX < bottomX &&
      mouseY >= windowY + (5 / scale) && mouseY < bottomY - (5 / scale)) {
      for (GuiElement button : inputList) {
        if (mouseX > button.x && mouseX < (button.x + width) &&
          mouseY > button.y && mouseY < (button.y + button.height)) {
          drawSettingTooltip(button.setting, mouseX, mouseY);
          break;
        }
      }
    }
  }

  private void drawSettingTooltip(Setting sett, int xScaled, int yScaled) {
    int scale = ClickGui.scaledRes.getScaleFactor();

    String description = sett.getDescription();

    int offset = 2;
    int tooltipX = xScaled / scale + offset;
    int tooltipY = yScaled / scale + offset;
    int padding = 2;
    int tooltipWidth = SurfaceHelper.getTextWidth(description) / scale + padding * 2;
    int lineHeight = SurfaceHelper.getTextHeight() / scale;
    int lineSpacing = 2;
    int tooltipHeight = lineHeight + padding * 2;

    if ((tooltipX + tooltipWidth) * scale > ClickGui.scaledRes.getScaledWidth()) {
      tooltipX -= tooltipWidth + offset * 2;
    }

    if ((tooltipY + tooltipHeight) * scale > ClickGui.scaledRes.getScaledHeight()) {
      tooltipY -= tooltipHeight + offset * 2;
    }

    final int col = Color.of(50, 50, 50, 200).toBuffer();

    SurfaceHelper.drawRect(tooltipX * scale, tooltipY * scale + 1,
      tooltipWidth * scale, tooltipHeight * scale - 2,
      col);

    SurfaceHelper.drawRect(tooltipX * scale + 1, tooltipY * scale,
      tooltipWidth * scale - 2, tooltipHeight * scale,
      col);

    // SurfaceHelper
    //   .drawTextShadow(modName, (tooltipX + padding) * scale, (tooltipY + padding) * scale,
    //     0xFFFFFF);
    SurfaceHelper.drawTextShadow(description, (tooltipX + padding) * scale,
      (tooltipY + padding) * scale, 0xAAAAAA);
  }

  public void drawWindow(int mouseX, int mouseY) {
    super.drawWindow(mouseX, mouseY);

    if (!isHidden) {
      for (GuiElement input : inputList) {
        input.x = 2;
        input.y = height + 2;
        input.width = width;
        input.draw(mouseX, mouseY);
      }
    }

    // update variables
    bottomX = posX + width; // set the coords of the bottom right corner for mouse coord testing
    bottomY = windowY + height;
  }

  public void keyTyped(char typedChar, int keyCode) throws IOException {
    for (GuiElement element : inputList) {
      element.keyTyped(typedChar, keyCode);
    }
  }

  public void mouseClicked(int x, int y, int state) {
    super.mouseClicked(x, y, state);

    if (state == MouseButtons.RIGHT.id && isMouseInHeader(x, y)) { // delete the window on right click
      ClickGui.getInstance().windowList.remove(this);
    } else if (!isHidden) {
      for (GuiElement input : inputList) {
        if (input.isMouseInElement(x , y)) {
          input.mouseClicked(x, y, state);
          break;
        }
      }
    }
  }

  public void mouseReleased(int x, int y, int state) {
    super.mouseReleased(x, y, state);
    for (GuiElement input : inputList) {
      input.mouseReleased(x, y, state);
    }
  }

  public void handleMouseInput(int x, int y) throws IOException {
    for (GuiElement input : inputList) {
      if (input.isMouseInElement(x , y)) {
        input.handleMouseInput(x, y);
        break;
      }
    }
  }
}
