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
 * Created by Tonio on 10/08/2020.
 */
public class GuiToggleEnum extends GuiElement {

  private static final int ELEMENT_OUTLINE = Color.of(65, 65, 65, 200).toBuffer();
  private static final int ELEMENT_IN = Color.of(100, 100, 100, 150).toBuffer();
  private static final int ELEMENT_IN_PRESSED = Color.of(150, 150, 150, 150).toBuffer();

  private boolean pressed = false;

  private Setting setting;

  public GuiToggleEnum(Setting settingIn, GuiWindowSetting parent) {
    super(settingIn, parent);
    this.setting = settingIn;
    height = 12;
  }

  public void mouseClicked(int mouseX, int mouseY, int state) {
    this.pressed = true;
    Object[] opts = this.setting.get().getClass().getEnumConstants();
    for (int i=0; i<opts.length; i++) { // This is ugly, if you know a better way show me
      if (opts[i].equals(this.setting.get())) {
        if (i+1 >= opts.length) {
          this.setting.set(opts[0], false);
        } else {
          this.setting.set(opts[i+1], false);
        }
        break;
      }
    }
  }

  public void mouseReleased(int mouseX, int mouseY, int state) {
    this.pressed = false;
  }

  public void draw(int mouseX, int mouseY) {
    super.draw(x, y);

    if (pressed) {
      SurfaceHelper.drawOutlinedRect(x, y, width - 2, 10, ELEMENT_IN_PRESSED);
      SurfaceHelper.drawRect(x + 8, y + 2, width - 2 - 16, 6, ELEMENT_IN_PRESSED);
    } else {
      SurfaceHelper.drawOutlinedRect(x, y, width - 2, 10, ELEMENT_OUTLINE);
      SurfaceHelper.drawRect(x + 8, y + 2, width - 2 - 16, 6, ELEMENT_IN);
    }
    SurfaceHelper.drawTextShadow(String.format("%s : %s", setting.getName(), setting.get().toString()),
            x + 2, y + 1, Colors.WHITE.toBuffer());
  }
}

