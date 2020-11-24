package com.matt.forgehax.gui.elements;

import com.matt.forgehax.gui.windows.GuiWindowSetting;
import com.matt.forgehax.util.command.Command;
import java.io.IOException;

/**
 * Created by Babbaj on 9/6/2017.
 */
public class GuiElement {
  
  public GuiWindowSetting parentWindow;
  
  public boolean isActive = false; // for deciding if you can type in
  
  public int width, height; // width and height of the element
  public int subX, subY; // coords of the element relative to the parent window
  public int x, y; // coords of the element posX + subX
  
  public Command command;
  
  public GuiElement(Command commandIn, GuiWindowSetting parent) {
    this.parentWindow = parent;
    this.command = commandIn;
  }
  
  public void mouseClicked(int x, int y, int state) {
  }
  
  public void mouseReleased(int x, int y, int state) {
  }

  public void handleMouseInput(int x, int y) {
  }
  
  public void keyTyped(char typedChar, int keyCode) throws IOException {
  }
  
  public void onRemoved() {  
  }
  
  public void draw(int mouseX, int mouseY) {
    this.x = getPosX() + this.subX + 1;
    this.y = getPosY() + this.subY + 22;
  }
  
  public int getPosX() {
    return parentWindow.posX;
  }
  
  public int getPosY() {
    return parentWindow.headerY;
  }
  
  public boolean isMouseInElement(int mouseX, int mouseY) {
    return mouseX > this.x
      && mouseX < this.x + width
      && mouseY > this.y
      && mouseY < this.y + height;
  }
}
