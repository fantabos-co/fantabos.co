package com.matt.forgehax.mods.services;

import com.matt.forgehax.Helper;
import com.matt.forgehax.gui.PromptGui;
import com.matt.forgehax.gui.PromptGui.ClientMode;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.command.StubBuilder;
import com.matt.forgehax.util.command.callbacks.CallbackData;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.network.play.server.SPacketTabComplete;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.input.Keyboard;
import java.util.Arrays;

import static com.matt.forgehax.Helper.getCurrentScreen;

import com.matt.forgehax.asm.events.AddChatLineEvent;
import com.matt.forgehax.asm.events.PacketEvent;

/**
 * Created by OverFloyd
 * may 2020
 */
@RegisterMod
public class CommandPromptService extends ServiceMod {

  public enum GuiTypes {
    CHAT,     // Regular virgin mode
    TERMINAL  // Chad Terminal mode
  }

  public static ClientMode modeFromString(String text) {
    for (ClientMode possible : ClientMode.values()) {
      if (possible.toString().equalsIgnoreCase(text)) {
        return possible;
      }
    }
    return null;
  }

  public final Setting<GuiTypes> guiType =
          getCommandStub()
                  .builders()
                  .<GuiTypes>newSettingEnumBuilder()
                  .name("mode")
                  .description("Choose what Gui to open.")
                  .defaultTo(GuiTypes.TERMINAL)
                  .build();

  public CommandPromptService() {
    super("CommandPrompt", "Dedicated terminal panel");
  }

  @Override
  protected void onLoad() {
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("nuke")
        .description("Clear all histories")
        .processor(data -> PromptGui.clearAll())
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("sent-clear")
        .description("Clear your sent message history")
        .processor(data -> PromptGui.sentHistory.clear())
        .build();

    // TODO: fix sent history shit (there's only one in here, needs to be adjusted)
    getCommandStub()
            .builders()
            .newCommandBuilder()
            .name("clear")
            .description("Clears target history, or current if no argument is provided")
            .options(p -> p.acceptsAll(Arrays.asList("ForgeHax", "Chat", "IRC", "Omni"), "Clears sent history"))
            .processor(data -> {
              if (data.getArgumentCount() == 1) {
                ClientMode target = modeFromString(data.getArgument(0));
                if (target == null) {
                  Helper.printError("Unknown mode");
                  return;  
                }

                StringBuilder result = new StringBuilder();
                switch (target) {
                  case FORGEHAX:
                    PromptGui.forgehaxHistory.clear();
                    Helper.printInform("Cleared ForgeHax history");

                    break;
                    /*
                  case 1:
                    PromptGui.baritoneHistory.clear();
                    result.append("Cleared Baritone history");

                    if (data.hasOption(index + "")) {
                      PromptGui.baritoneSentHistory.clear();
                      result.append(" [with sent message history]");
                    }

                    Helper.printInform(result + ".");
                    break;
*/
                  case CHAT:
                    try {
                      PromptGui.chatHistory.clear();
                      MC.ingameGUI.getChatGUI().clearChatMessages(false); // don't also clear sent history
                      Helper.printInform("Cleared Chat history");
                    } catch (RuntimeException e) {
                      e.printStackTrace();
                    }
                    break;

                  case IRC:
                    PromptGui.IRCHistory.clear();
                    Helper.printInform("Cleared IRC history");
                    break;

                  case OMNI:
                    PromptGui.omniHistory.clear();
                    Helper.printInform("Cleared Omni history");
                    break;
                }
              } else {
                PromptGui.getActiveHistory().clear();

                if (PromptGui.getActiveHistory() != PromptGui.forgehaxHistory) {
                  Helper.printInform("Cleared " + PromptGui.mode.getName() + " history.");
                }
              }
            })
            .build();

    getCommandStub()
            .builders()
            .newCommandBuilder()
            .name("help")
            .description("Prints help message.")
            .processor(data -> {
              String build = "Keybinds:\n" +
                      "* Arrows: navigate through input field\n" +
                      "* Tab: accept autocomplete or request server autocomplete\n" +
                      "* Shift + Tab: navigate through possible autocompletions\n" +
                      "* PagUp / Down: switch between mode\n" +
                      "* Shift + Scroll: faster scrolling (7x)\n \n" +
                      //
                      //
                      "Modes:\n" +
                      "* ForgeHax: ForgeHax system messages, command only\n" +
                      //"* Baritone: mode for Baritone commands.\n" +
                      "* IRC: IRC chat, all messages are sent to IRC server\n" +
                      "* Chat: contains only messages coming from server\n" +
                      "* Omni: contains everything";
              data.write(build);
              data.markSuccess();
            })
            .build();

  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onChat(ClientChatReceivedEvent event) {
    PromptGui.getInstance().print(event.getMessage(), PromptGui.ClientMode.CHAT);
  }

  @SubscribeEvent
  public void onChatLineAdded(AddChatLineEvent event) {
    PromptGui.omniHistory.push(event.getMessage());
  }

  @Override
  public void onBindPressed(CallbackData cb) {
    if (getCurrentScreen() == null) {
      switch (guiType.get()) {
        case CHAT:
          MC.displayGuiScreen(new GuiChat(ChatCommandService.getActivationCharacter().toString()));
          break;
        case TERMINAL:
          MC.displayGuiScreen(new PromptGui(""));
          break;
      }
    }
  }

  @Override
  protected StubBuilder buildStubCommand(StubBuilder builder) {
    return builder
            .kpressed(this::onBindPressed)
            .kdown(this::onBindKeyDown)
            .bind(Keyboard.KEY_PERIOD); // default to comma
  }
}
