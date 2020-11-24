package com.matt.forgehax.gui.elements;

import com.matt.forgehax.gui.windows.GuiWindowSetting;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

import static com.matt.forgehax.Globals.MC;

/**
 * Created by Tonio on 10/08/2020.
 */
public class GuiSlider extends GuiElement {

  private static final int ELEMENT_OUTLINE = Color.of(65, 65, 65, 200).toBuffer();
  private static final int ELEMENT_IN = Color.of(150, 150, 150, 200).toBuffer();

  private boolean holding = false;
  private Setting setting = null;

  public GuiSlider(Setting settingIn, GuiWindowSetting parent) {
    super(settingIn, parent);
    this.setting = settingIn;
    height = 12;
  }

  private void setVal(int mouseX) {
    double val = getRelativeValue(mouseX);
    setVal(val);
  }
  
  private void setVal(double val) {
	  if (this.setting.getDefault() instanceof Float)
		  this.setting.set((float) val, false);
	  else if (this.setting.getDefault() instanceof Double)
		  this.setting.set(val, false);
	  else if (this.setting.getDefault() instanceof Integer)
		  this.setting.set((int) val, false);
	  else if (this.setting.getDefault() instanceof Long)
		  this.setting.set((long) val, false);
  }

  /*TheAlphaEpsilon*/
  
  public void keyTyped(char typedChar, int keyCode) throws IOException {
	  
	  if(!isActive) {
		  return;
	  }
	  
	  double value = setting.getAsDouble();

	  switch(keyCode) {
	  case Keyboard.KEY_BACK:
		  setVal(Math.floor(value / 10));
		  return;
	  case Keyboard.KEY_MINUS:
		  setVal(-value);
		  return;
	  case Keyboard.KEY_0:
	  case Keyboard.KEY_1:
	  case Keyboard.KEY_2:
	  case Keyboard.KEY_3:
	  case Keyboard.KEY_4:
	  case Keyboard.KEY_5:
	  case Keyboard.KEY_6:
	  case Keyboard.KEY_7:
	  case Keyboard.KEY_8:
	  case Keyboard.KEY_9:
		  setVal(value * 10 + (value < 0 ? -1 : 1) * (typedChar - 48));
		  return;
	  default:
		  return;
	  }
	  	  
  }
  
  /**/
  
  public void mouseClicked(int mouseX, int mouseY, int state) {
	isActive = isMouseInElement(mouseX, mouseY);
    setVal(mouseX);
    holding = true;
  }

  public void mouseReleased(int mouseX, int mouseY, int state) {
    holding = false;
  }

  public void handleMouseInput(int x, int y) {
    int i = Mouse.getEventDWheel();

    if (this.setting.getDefault() instanceof Integer) {
      int val = MathHelper.clamp(i, -1, 1);
      this.setting.set(this.setting.getAsInteger() + val, false);
    } else if (this.setting.getDefault() instanceof Long) {
      long val = (long) MathHelper.clamp(i, -1, 1);
      this.setting.set(this.setting.getAsLong() + val, false);
    } else if (this.setting.getDefault() instanceof Float) {
      float val = MathHelper.clamp(i, -0.01f, 0.01f);
      this.setting.set(this.setting.getAsFloat() + val, false);
    } else if (this.setting.getDefault() instanceof Double) {
      double val = MathHelper.clamp(i, -0.01d, 0.01d);
      this.setting.set(this.setting.getAsDouble() + val, false);
    }
  }

  public void draw(int mouseX, int mouseY) {
    super.draw(x, y);

    if (holding) setVal(mouseX);

    int width_value = (int) (getPercValue(this.setting.getAsFloat()) * width) - 2;
    if (width_value < 0) width_value = 0;

    SurfaceHelper.drawRect(x, y, width_value, 10, ELEMENT_IN);
    SurfaceHelper.drawOutlinedRect(x, y, width - 2, 10, ELEMENT_OUTLINE);
    
    String text;
    if (this.setting.get() instanceof Integer || this.setting.get() instanceof Long)
      text = String.format("%s [%d]", this.setting.getName(), this.setting.get());
    else
      text = String.format("%s [%.2f]", this.setting.getName(), ((Number) this.setting.get()).floatValue());
    SurfaceHelper.drawTextShadow(text, (x + 2), y + 1, Colors.WHITE.toBuffer());
  }

  private float getOff() { // this is for sliders which allow negative values
    return Math.abs((Float) (this.setting.getMin() != null ? ((Number) this.setting.getMin()).floatValue() : 0f));
  }

  private float getRange() {
    float max = (Float) (this.setting.getMax() != null ? ((Number) this.setting.getMax()).floatValue() : 5f);
    float min = (Float) (this.setting.getMin() != null ? ((Number) this.setting.getMin()).floatValue() : 0f);
    return max - min;
  }

  private float getPercValue(float val) {
    return (val + getOff()) / getRange();
  }

  private double getRelativeValue(int w) {
    return (((double) (w - x) / (double) width) * getRange()) - getOff();
  }
}

