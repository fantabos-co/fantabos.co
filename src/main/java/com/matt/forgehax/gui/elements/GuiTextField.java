package com.matt.forgehax.gui.elements;

import static com.matt.forgehax.Globals.MC;

import com.matt.forgehax.Helper;
import com.matt.forgehax.mods.services.ChatCommandService;
import com.matt.forgehax.gui.windows.GuiWindowSetting;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.CommandHelper;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.command.exception.CommandExecuteException;
import com.matt.forgehax.util.draw.SurfaceHelper;
import net.minecraft.client.gui.GuiChat;

public class GuiTextField extends GuiElement {

  public GuiTextField(Command commandIn, GuiWindowSetting parent) {
    super(commandIn, parent);
    height = 12;
  }

  public void draw(int mouseX, int mouseY) {
    super.draw(x, y);
    String text = (command instanceof Setting ?
            String.format("%s: \"%s\"", command.getName(), ((Setting) command).get().toString()) :
            "-> " + command.getName());
    SurfaceHelper.drawTextShadow(text, x + 1, y + 1, Colors.WHITE.toBuffer());
  }

  public void mouseClicked(int mouseX, int mouseY, int state) {
    try { // This is super lazy but command.getRequiredArgs() gave 0 when it should not
      String[] arguments = CommandHelper.translate("");
      command.run(arguments);
    } catch (CommandExecuteException e) {
      Helper.printError(e.getMessage());
      String cmd = ChatCommandService.getActivationCharacter().toString();
      cmd += this.parentWindow.getMod().getModName();
      cmd += (" " + this.command.getName() + " ");
      MC.displayGuiScreen(new GuiChat(cmd));
    }
  }
}
