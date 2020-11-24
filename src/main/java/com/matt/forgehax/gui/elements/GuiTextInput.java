package com.matt.forgehax.gui.elements;

import com.matt.forgehax.gui.windows.GuiWindowSetting;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

import static com.matt.forgehax.Globals.MC;

/**
 * Created by Babbaj on 9/15/2017.
 */
public class GuiTextInput extends GuiElement {

  private static final int ELEMENT_OUTLINE = Color.of(65, 65, 65, 200).toBuffer();
  private static final int ELEMENT_IN = Color.of(100, 100, 100, 150).toBuffer();

  private int ticks;

  private int selectedIndex = -1;
  private final StringBuilder input = new StringBuilder();

  private Setting setting;

  public GuiTextInput(Setting settingIn, GuiWindowSetting parent) {
    super(settingIn, parent);
    this.setting = settingIn;
    height = 15;
  }

  public void mouseClicked(int mouseX, int mouseY, int state) {
    isActive = isMouseInElement(mouseX, mouseY);
  }

  public void keyTyped(char typedChar, int keyCode) throws IOException {
    if (isActive) {
      switch (keyCode) {
        case Keyboard.KEY_ESCAPE:
          isActive = false;
          break;

        case Keyboard.KEY_RETURN:
          isActive = false;
          // setValue(input);
          MC.player.sendMessage(new TextComponentString(input.toString()));
          break;

        case Keyboard.KEY_BACK:
          if (selectedIndex > -1) {
            input.deleteCharAt(selectedIndex);
            selectedIndex--;
          }
          break;

        case Keyboard.KEY_LEFT:
          selectedIndex--;
          break;

        case Keyboard.KEY_RIGHT:
          selectedIndex++;
          break;

        default:
          if (isValidChar(typedChar)) {
            selectedIndex++;
            input.insert(selectedIndex, typedChar);
          }
      }

      selectedIndex = MathHelper.clamp(selectedIndex, -1, input.length() - 1);
    }
  }

  public void draw(int mouseX, int mouseY) {
    super.draw(x, y);

    // SurfaceHelper.drawRect(x, y, width - 2, height, ELEMENT_IN);
    SurfaceHelper.drawOutlinedRect(x, y, width - 2, height, ELEMENT_OUTLINE);

    SurfaceHelper.drawTextShadowCentered(setting.get().toString(),
            (x + 2) + width / 2f, y + height / 2f, Colors.WHITE.toBuffer());

    // how often to blink the thing
    int blinkSpeed = 30;
    if (ticks % blinkSpeed * 2 > blinkSpeed && isActive) {
      int width = getBlinkWidth();
      SurfaceHelper.drawLine(x + width + 1, y + 2,x + width + 1, y + height - 2, Colors.BLACK.toBuffer(), width);
    }

    // SurfaceHelper.drawText(getInputString(), x + 1, y + 2, Colors.BLACK.toBuffer());
    ticks++;
  }

  private int getBlinkWidth() {
    if (input.length() > 0) {
      return SurfaceHelper.getTextWidth(input.substring(0, selectedIndex + 1));
    } else {
      return 0;
    }
  }

  private String getInputString() {
    return input.toString();
  }

  private boolean isValidChar(char charIn) {
    return !Character.isISOControl(charIn);
  }

  private void setValue(String in) {
    setting.set(in);
  }
}
