package com.matt.forgehax.gui.elements;

import static com.matt.forgehax.Globals.MC;

import com.matt.forgehax.gui.windows.GuiWindowSetting;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceHelper;
import java.io.IOException;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Keyboard;

public class GuiTextField extends GuiElement {

  public GuiTextField(Setting settingIn, GuiWindowSetting parent) {
    super(settingIn, parent);
    height = 12;
  }

  public void draw(int mouseX, int mouseY) {
    super.draw(x, y);
    SurfaceHelper.drawTextShadow(String.format("%s [%s]", setting.getName(), setting.get().toString()),
                            x + 1, y + 1, Colors.WHITE.toBuffer());
  }
}
