package com.matt.forgehax.gui.windows;

import com.google.common.collect.Lists;
import com.matt.forgehax.gui.ClickGui;
import com.matt.forgehax.gui.elements.GuiColorSelect;
import com.matt.forgehax.gui.elements.GuiElement;
import com.matt.forgehax.gui.elements.GuiTextField;
import com.matt.forgehax.gui.elements.GuiToggle;
import com.matt.forgehax.gui.elements.GuiToggleEnum;
import com.matt.forgehax.mods.services.GuiService;
import com.matt.forgehax.gui.elements.GuiSlider;
import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.BaseMod;

import net.minecraft.util.math.MathHelper;

import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.draw.SurfaceHelper;

import static com.matt.forgehax.Globals.MC;
import static com.matt.forgehax.Helper.getModManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * Created by Babbaj on 9/5/2017.
 */
public class GuiWindowSetting extends GuiWindow {

  // list of toggles, sliders, text inputs, etc.
  public List<GuiElement> inputList = new ArrayList<>();

  private GuiElement lastClicked; //Used to "unactive" elements when clicking elsewhere
  
  /**
   * The button list y coord needs to be offset to move them up or down the window 0 = natural state
   * anything above 0 means the button list has moved up and the user has scrolled down
   */
  private int inputListOffset;
  
  private final BaseMod mod;
  //private final int x;
  //private final int y;
  
  public GuiWindowSetting(BaseMod modIn, int xIn, int yIn) {
    super(modIn.getModName() + " Settings");
    this.mod = modIn;
    //this.x = xIn;
    //this.y = yIn;
    //height = 2; //This is modifed later
    this.setPosition(xIn, yIn);
    initializeInputs();
  }

  private void initializeInputs() {
    getMod().getCommands().forEach(command -> {
      try {
        final String settingName = command.getName();
        if (command instanceof Setting<?>) {
          final Setting<?> setting = getMod().getSetting(settingName);
          GuiElement f;
          //TODO: Give settings widths for better gui
          if (setting.getDefault() instanceof Boolean)
            f = new GuiToggle(setting, this);
          else if (setting.getDefault() instanceof Float ||
                   setting.getDefault() instanceof Integer ||
                   setting.getDefault() instanceof Long ||
                   setting.getDefault() instanceof Double)
            f = new GuiSlider(setting, this);
          else if (setting.getDefault() instanceof Enum)
            f = new GuiToggleEnum(setting, this);
          else if (setting.getDefault() instanceof Color) {
        	  f = new GuiColorSelect((Setting<Color>) setting, this);
          }
          else {
            f = new GuiTextField(setting, this);
          }
          height += f.height;
          if(f.width > width) {
        	  width = f.width;
          }
          inputList.add(f);
        } else {
          GuiElement f = new GuiTextField(command, this);
          height += f.height;
          if(f.width > width) {
        	  width = f.width;
          }
          inputList.add(f);
        }
      } catch (Exception ignored) {
      }
    });
    height += 4;
  }

  public String getModName() {
    return mod.getModName();
  }

  public BaseMod getMod() {
    return this.mod;
  }

  @Override
  public void drawTooltip(int mouseX, int mouseY, int index) {
	  
    int scale = ClickGui.getScaledResFactor();

    if (isHidden){
      return;
    }

    boolean isGood = true;
    
    List<GuiWindow> reverse = Lists.reverse(ClickGui.getInstance().windowList);
    for(int i = 0; i < index; i++) {
    	GuiWindow window = reverse.get(i);
    	isGood = !(mouseX >= window.posX && mouseX <= window.posX + window.width &&
    			mouseY >= window.headerY && mouseY <= window.bottomY);
    	if(!isGood) {
    		break;
    	}
    }
    
    if (mouseX >= posX && mouseX < bottomX &&
      mouseY >= windowY + (5 / scale) && mouseY < bottomY - (5 / scale)) {
      for (GuiElement button : inputList) {
        if (mouseX > button.x && mouseX < (button.x + width) &&
          mouseY > button.y && mouseY < (button.y + button.height) &&
          isGood) {
        	drawSettingTooltip(button.command, mouseX, mouseY);
        	break;
        }
      }
    }
  }

  private void drawSettingTooltip(Command comm, int xScaled, int yScaled) {
    int scale = ClickGui.getScaledResFactor();

    String description = comm.getDescription();

    int offset = 2;
    int tooltipX = xScaled / scale + offset;
    int tooltipY = yScaled / scale + offset;
    int padding = 2;
    int tooltipWidth = SurfaceHelper.getTextWidth(description) / scale + padding * 2;
    int lineHeight = SurfaceHelper.getTextHeight() / scale;
    //int lineSpacing = 2;
    int tooltipHeight = lineHeight + padding * 2;

    if ((tooltipX + tooltipWidth) * scale > ClickGui.getScaledWidth()) {
      tooltipX -= tooltipWidth + offset * 2;
    }

    if ((tooltipY + tooltipHeight) * scale > ClickGui.getScaledWidth()) {
      tooltipY -= tooltipHeight + offset * 2;
    }

    final int col = Color.of(50, 50, 50, 200).toBuffer();

    SurfaceHelper.drawRect(tooltipX * scale, tooltipY * scale + 1,
      tooltipWidth * scale, tooltipHeight * scale - 2,
      col);

    SurfaceHelper.drawRect(tooltipX * scale + 1, tooltipY * scale,
      tooltipWidth * scale - 2, tooltipHeight * scale,
      col);

    SurfaceHelper.drawTextShadow(description, (tooltipX + padding) * scale,
      (tooltipY + padding) * scale, 0xAAAAAA);
  }
  
  public void drawWindow(int mouseX, int mouseY) {
	  	super.drawWindow(mouseX, mouseY);
	    windowY = headerY + 23;
	    if (isHidden){
	      return;
	    }
	    int actualHeight = (int) Math.min(height, ClickGui.getScaledHeight() *
	      getModManager().get(GuiService.class).get().max_height.get());

	    SurfaceHelper.drawOutlinedRectShaded(
	    	      posX, 
	    	      windowY, 
	    	      width, 
	    	      actualHeight, 
	    	      gui.settingsColor.get().toBuffer(), 
	    	      gui.settingsAlpha.getAsInteger(), 
	    	      3);
	    int inputY = -inputListOffset + 4;
	    
	    int scale = ClickGui.getScaledResFactor();

	    GL11.glPushMatrix();
	    int scissorY = MC.displayHeight - (scale * windowY + scale * actualHeight);
	    GL11.glScissor(scale * posX, scissorY, scale * width, scale * actualHeight);
	    GL11.glEnable(GL11.GL_SCISSOR_TEST);
	    for (GuiElement input : inputList) {
	    	input.subY = inputY;
	    	input.width = width;
	    	input.draw(mouseX, mouseY);
	    	inputY += input.height;
	    }
	    GL11.glDisable(GL11.GL_SCISSOR_TEST);
	    GL11.glPopMatrix();

	    // update variables
	    bottomX = posX + width; // set the coords of the bottom right corner for mouse coord testing
	    bottomY = windowY + actualHeight;
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
      for (GuiElement input : inputList) {
          input.onRemoved();
      }
    } else if (!isHidden) {
      if(lastClicked != null) {
    	  lastClicked.isActive = false; //Set last clicked element to false
      }
      for (GuiElement input : inputList) {
        if (input.isMouseInElement(x , y)) {
          lastClicked = input;
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
	  
	  int i = Mouse.getEventDWheel();
	  
	  i = MathHelper.clamp(i, -1, 1);
	  inputListOffset -= i * 10;

	  if (inputListOffset < 0) {
		  inputListOffset = 0; // don't scroll up if its already at the top
	  }

	  int actualHeight = (int) Math.min(height, ClickGui.getScaledHeight() *
			  getModManager().get(GuiService.class).get().max_height.get());
	  int lowestButtonY = windowY;
	  for(GuiElement element : inputList) {
		  lowestButtonY += element.height;
	  }
	  int lowestAllowedOffset = lowestButtonY - actualHeight - windowY + 4;
	  if (lowestButtonY - inputListOffset < bottomY) {
		  inputListOffset = lowestAllowedOffset;
	  }
	  for (GuiElement input : inputList) {
		  if (input.isMouseInElement(x , y)) {
			  input.handleMouseInput(x, y);
			  break;
		  }
	  }
  }
  
  @Override
  public boolean equals(Object other) {
	  if(this == other) {
		  return true;
	  } else if(!(other instanceof GuiWindowSetting)) {
		  return false;
	  } else {
		  GuiWindowSetting otherWindow = (GuiWindowSetting)other;
		  return this.mod.getModName().equals(otherWindow.mod.getModName());
	  }
  }
}
