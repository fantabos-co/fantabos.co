package com.matt.forgehax.gui.elements;

import com.matt.forgehax.gui.windows.GuiWindowSetting;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceHelper;

/**
 * Created by Tonio on 10/08/2020.
 */
public class GuiToggle extends GuiElement {

  private static final int ELEMENT_OUTLINE = Color.of(65, 65, 65, 200).toBuffer();
  private static final int ELEMENT_IN = Color.of(150, 150, 150, 200).toBuffer();

  private Setting setting;

  public GuiToggle(Setting settingIn, GuiWindowSetting parent) {
    super(settingIn, parent);
    this.setting = settingIn;
    height = 12;
  }

  public void mouseClicked(int mouseX, int mouseY, int state) {
    this.setting.set(!this.setting.getAsBoolean(), false);
  }

  public void draw(int mouseX, int mouseY) {
    super.draw(x, y);

    if (this.setting.getAsBoolean())
      SurfaceHelper.drawRect(x + 2 + 2, y + 2, 6, 6, ELEMENT_IN);
    SurfaceHelper.drawOutlinedRect(x + 2, y, 10, 10, ELEMENT_OUTLINE);

    SurfaceHelper.drawTextShadow(setting.getName(),
            (x + 6 + 10), (y + 1), Colors.WHITE.toBuffer());
  }
}

