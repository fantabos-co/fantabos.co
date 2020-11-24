package com.matt.forgehax.gui;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getNetworkManager;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import com.matt.forgehax.Globals;
import com.matt.forgehax.Helper;
import com.matt.forgehax.mods.BetterChat;
import com.matt.forgehax.mods.managers.IRC;
import com.matt.forgehax.mods.services.ChatCommandService;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.CommandHelper;
import com.matt.forgehax.util.draw.SurfaceHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.util.ITabCompleter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent.Action;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.lwjgl.input.Keyboard.*;

/**
 * Made a separate thing by OverFloyd with additions and fixes
 * Autocomplete and small fixes from Tonio
 */

public class PromptGui extends GuiScreen implements Globals, ITabCompleter {

  public enum ClientMode {
    FORGEHAX("ForgeHax"),
    // BARITONE("Baritone"),
    IRC("IRC"),
    CHAT("Chat"),
    OMNI("Omni");

    private final String name;

    public String getName() {
      return this.name;
    }

    ClientMode(String nameIn) {
      this.name = nameIn;
    }
  }

  // Histories
  public static Deque<ITextComponent> omniHistory = new ConcurrentLinkedDeque<>();
  public static Deque<ITextComponent> forgehaxHistory = new ConcurrentLinkedDeque<>();
  // public static Deque<ITextComponent> baritoneHistory = new ConcurrentLinkedDeque<>();
  public static Deque<ITextComponent> IRCHistory = new ConcurrentLinkedDeque<>();
  public static Deque<ITextComponent> chatHistory = new ConcurrentLinkedDeque<>(); // lmao wasted
  public static List<String> sentHistory = new ArrayList<>();
  public static List<ITextComponent> screenElements = new ArrayList<>();
  public static PromptGui INSTANCE;

  public static ClientMode mode = ClientMode.OMNI;
  private final float chatOpacity = MC.gameSettings.chatOpacity;
  private final GuiScreen previousScreen;
  private String[] tabComplete = null;
  private int tabIndex = 0;

  public static PromptGui getInstance() {
    return (INSTANCE == null) ? (INSTANCE = new PromptGui("")) : INSTANCE;
  }

  public static void clearAll() {
    IRCHistory.clear();
    chatHistory.clear();
    forgehaxHistory.clear();
    omniHistory.clear();
    sentHistory.clear();
  }

  public static Deque<ITextComponent> getActiveHistory() {
    switch (mode) {
      case IRC: return IRCHistory;
      // case BARITONE: return baritoneHistory;
      case CHAT: return chatHistory;
      case OMNI: return omniHistory;
      default: return forgehaxHistory;
    }
  }

  private List<String> getCorrectSentHistory() {
    return sentHistory;
  }

  GuiButton backButton;
  GuiTextField inputField;
  GuiButton modeButton;

  // ordered from oldest to newest
  private int sentHistoryCursor = -1;

  private final String defaultText;
  private String autoComplete = "";
  private final List<ITextComponent> commandPreview = new ArrayList<>();
  private int historyOffset = 0;
  private ITextComponent mouse_over = null;
  private EntityPlayer.EnumChatVisibility chatSetting;

  public PromptGui(String defaultTextIn) {
    this.defaultText = defaultTextIn;
    previousScreen = Helper.getCurrentScreen();
  }

  @Override
  public void initGui() {
    Keyboard.enableRepeatEvents(true);

    // Hides normal chat
    if (!(MC.gameSettings.chatVisibility == EntityPlayer.EnumChatVisibility.HIDDEN)) {
      chatSetting = MC.gameSettings.chatVisibility;
      MC.gameSettings.chatVisibility = EntityPlayer.EnumChatVisibility.HIDDEN;
    }

    this.inputField =
            new GuiTextField(0, this.fontRenderer, 4, this.height - 12, this.width - 4, 12);
    inputField.setMaxStringLength(Integer.MAX_VALUE);
    this.inputField.setEnableBackgroundDrawing(false);
    this.inputField.setFocused(true);
    this.inputField.setCanLoseFocus(false);
    this.inputField.setText(defaultText);
    this.sentHistoryCursor = -1;
    this.historyOffset = 0;
    this.autoComplete = "";
    this.commandPreview.clear();
    this.mouse_over = null;

    this.buttonList.add(
            modeButton = new GuiButton(
                    0, this.width - 100 - 2, this.height - 12 - 4, 100, 14, mode.getName()));
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }

  @Override
  public void setCompletions(String... newCompletions) {
    if (newCompletions.length > 0) {
      tabComplete = newCompletions;
      tabIndex = 0;
      autoComplete = getTabCompletableText() + newCompletions[0];
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    if (getLocalPlayer() == null) this.drawDefaultBackground();

    // input field
    drawRect(2, this.height - 14, this.width - 104, this.height - 2,
                              Color.of(0, 0, 0, 150).toBuffer());

    if (inputField.getText().isEmpty() && autoComplete.equals("")) {
      String help = "";

      switch (mode) {
        case FORGEHAX:
          help = "<mod|command> <setting|args> [value] -?";
          break;
        // case BARITONE:
        //   help = "<command> [value]";
        //   break;
        case IRC:
          if (IRC.isConnected())
            help = "Send message to " + IRC.getServer() + " > " + IRC.getDefaultChannel();
          else help = "Not connected to any IRC server";
          break;
        case CHAT:
          if (getWorld() == null) help = "Can only send chat messages in-game";
          else help = "Send message to in-game chat";
          break;
        case OMNI:
          help = "OMNI > ";
          if (getWorld() == null) {
            if (IRC.isConnected())
              help += "Send message to " + IRC.getServer() + " > " + IRC.getDefaultChannel();
            else help += "Not connected to any IRC server";
          } else {
            help += "Send message to in-game chat";
          }
          break;
      }

      SurfaceHelper.drawTextShadow(help, 4, this.height - 12, Colors.GRAY.toBuffer());
    } else {
      SurfaceHelper.drawTextShadow(autoComplete, 4, this.height - 12, Colors.GRAY.toBuffer());
    }

    this.inputField.drawTextBox();

    // messageHistory box
    calculateScreenElements();

    drawRect(2, 2, this.width - 2, this.height - 38,
                  Color.of(0, 0, 0, 150).toBuffer());
    this.drawHistory();

    float max = getActiveHistory().size();
    float boxSize = this.height - 40;
    float shown = screenElements.size();
    if (max > 0) {
      float perc = Math.abs(historyOffset) / max;
      float barHeight = (shown * boxSize) / max;

      int scrollBar = (int) Math.min(boxSize * perc, boxSize);
      int low = (int) boxSize - scrollBar;
      int high = (int) Math.max(low - barHeight, 0);
      drawRect(this.width - 3, high + 2, this.width - 2, low + 2, Color.of(128, 128, 128, 200).toBuffer());
    }

    if (mouse_over != null && mouse_over.getStyle().getHoverEvent() != null)
        this.handleComponentHover(mouse_over, mouseX, mouseY);

    super.drawScreen(mouseX, mouseY, partialTicks);
  }

  @Override
  public void updateScreen() {
    this.inputField.updateCursorCounter();
    this.modeButton.displayString = mode.getName();
  }

  @Override
  protected void actionPerformed(GuiButton button) {
    if (button == modeButton) {
      if (mode.ordinal() == ClientMode.values().length - 1) {
        mode = ClientMode.values()[0];
      } else {
        mode = ClientMode.values()[mode.ordinal() + 1];
      }

      this.inputField.setText(""); // resets input field if mode is changed
    }

    if (mode != ClientMode.FORGEHAX) {
      autoComplete = "";
      commandPreview.clear();
    }

    if (button == backButton) {
      MC.displayGuiScreen(null);
    }
  }

  public void calculateScreenElements() {
    screenElements.clear();
    int offset = 0;
    int y = historyOffset * 10;
    for (ITextComponent t : getActiveHistory()) {
      if (y + offset < 0) {
        offset += 10;
        continue;
      }
      if (y + offset >= (this.height - 40)) break;
      for (ITextComponent spl : Lists.reverse(GuiUtilRenderComponents.splitText(t, this.width - 10, MC.fontRenderer, false, false))) {
        screenElements.add(spl);
        offset += 10;
      }
    }
  }

  private void drawIfInScreen(String text, int offset, int color) {
    int y = this.height - 50 - offset;
    int x = 5;
    if (y > 2 && y < (this.height - 30)) { // render extra line at top screen
      SurfaceHelper.drawTextShadow(text, x, y, color);
    }
  }

  private void drawHistory() {
    AtomicDouble offset = new AtomicDouble();
    if (!commandPreview.isEmpty()) {
      for (ITextComponent t : Lists.reverse(commandPreview)) {
        drawIfInScreen(t.getFormattedText(), (offset.intValue()), Colors.GRAY.toBuffer());
        offset.addAndGet(10);
      }
    }

    screenElements
            .forEach(
                    str -> {
                      drawIfInScreen(str.getFormattedText(), (offset.intValue()), Colors.WHITE.toBuffer());
                      offset.addAndGet(10);
                    });
  }

  @Override
  public boolean handleComponentClick(ITextComponent component) {
    if (component == null) return false;

    if (isShiftKeyDown()) {
      if (component.getStyle().getInsertion() != null) {
        autoComplete = component.getStyle().getInsertion();
        return true;
      }
    }
    if (component.getStyle().getClickEvent() == null) return false;
    if (component.getStyle().getClickEvent().getAction() == Action.SUGGEST_COMMAND) {
      autoComplete = component.getStyle().getClickEvent().getValue();
      return true;
    } else if (component.getStyle().getClickEvent().getAction() == Action.RUN_COMMAND) {
      this.runCommand(component.getStyle().getClickEvent().getValue());
      return true;
    } else {
      return super.handleComponentClick(component);
    }
  }

  @Override
  public void handleMouseInput() throws IOException {
    super.handleMouseInput();
    mouse_over = this.getChatComponent(Mouse.getEventX(), Mouse.getEventY());
    if (Mouse.getEventButtonState()) {
      if (mouse_over != null) {
        this.handleComponentClick(mouse_over);
      }
    }

    int i = Mouse.getEventDWheel();
    if (i != 0) {
      i = MathHelper.clamp(i, -1, 1);
      if (isShiftKeyDown()) i *= 7;
      historyOffset -= i;

      // TODO: stop scrolling at top of the screen
      if (historyOffset < -(getActiveHistory().size())) {
        historyOffset = -(getActiveHistory().size());
      }

      if (historyOffset > 0) {
        historyOffset = 0; // don't scroll down if its already at the bottom
      }
    }
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    super.keyTyped(typedChar, keyCode);
    String FH_PREFIX = ChatCommandService.getActivationCharacter().toString(); // fire autocomplete always if using prefix

    if (keyCode != KEY_TAB && keyCode != KEY_LSHIFT) {
      autoComplete = "";
      commandPreview.clear();
    }

    if (keyCode == KEY_ESCAPE) {
      if (previousScreen instanceof ClickGui) {
        MC.displayGuiScreen(ClickGui.getInstance());
      } else MC.displayGuiScreen(null);
    } else if (keyCode == KEY_TAB) {
      if (isShiftKeyDown()) {
        if (tabComplete.length > 0) {
          tabIndex++;
          if (tabIndex >= tabComplete.length) {
            tabIndex = 0;
          }
          if (tabIndex < 0) {
            LOGGER.error("lmao tonio wtf have u done u massive ding dong, tabIndex was " + tabIndex); // epic debugger
            tabIndex = 0;
          }
          autoComplete = getTabCompletableText() + tabComplete[tabIndex];
        }
      } else {
        if (autoComplete.length() >= this.inputField.getText().length()) {
          this.inputField.setText(autoComplete);
        } else {
          try {
            String[] words = inputField.getText().split(" ");
            if (getNetworkManager() != null)
              getNetworkManager().sendPacket(new CPacketTabComplete(words[words.length-1], null, false));
          } catch (RuntimeException e) {
            e.printStackTrace();
            // No autocomplete!
          }
        }
      }
    } else if (keyCode == KEY_RSHIFT || keyCode == KEY_LSHIFT) {
      // do nothing, easier catching it here than doing keyCode != in both this and the last else
    } else if (keyCode != KEY_RETURN && keyCode != KEY_NUMPADENTER) {
      if (keyCode == KEY_UP) { // up arrow
        // older
        String sent = getSentHistory(1);
        if (sent != null) {
          inputField.setText(sent);
        } else {
          inputField.setText("");
        }
      } else if (keyCode == KEY_DOWN) { // down arrow
        // newer
        String sent = getSentHistory(-1);
        if (sent != null) {
          inputField.setText(sent);
        } else {
          inputField.setText("");
        }
      } else if (keyCode == KEY_PRIOR) {
        if (mode.ordinal() == 0) {
          mode = ClientMode.values()[ClientMode.values().length - 1];
        } else {
          mode = ClientMode.values()[mode.ordinal() - 1];
        }
      } else if (keyCode == KEY_NEXT) {
        if (mode.ordinal() == ClientMode.values().length - 1) {
          mode = ClientMode.values()[0];
        } else {
          mode = ClientMode.values()[mode.ordinal() + 1];
        }
      } else {
        this.inputField.textboxKeyTyped(typedChar, keyCode); // add character to box
      }

      if (mode == ClientMode.FORGEHAX || inputField.getText().startsWith(FH_PREFIX)) {
        // try to autocomplete last arg
        try {
          String input;
          if (inputField.getText().startsWith(FH_PREFIX)) {
            input = inputField.getText().substring(1);
            autoComplete += FH_PREFIX;
          } else input = inputField.getText();

          String[] args = CommandHelper.translate(input);
          Command match = GLOBAL_COMMAND.getClosestChildDeep(args);

          if (match != null) {
            autoComplete = getTabCompletableText() + match.getName();
            autoComplete = autoComplete.toLowerCase(); // what psychos write uppercase commands???
            tabIndex = 0;
            tabComplete = new String[]{ match.getName() };
          }

          List<Command> possibleMatches = Objects.requireNonNull(GLOBAL_COMMAND.getPossibleChildsDeep(args));
          if (possibleMatches.size() > 0) {
            tabIndex = -1;
            tabComplete = new String[possibleMatches.size()];  // if you know how to make this
            for (int i = 0; i < possibleMatches.size(); i++) { //  nicer please show me :(
              tabComplete[i] = possibleMatches.get(i).getName().toLowerCase();
            }
          }

          for (String s : printCommandsRecursive(0, args, GLOBAL_COMMAND, possibleMatches).split("\n")) {
            commandPreview.addAll(GuiUtilRenderComponents.splitText(new TextComponentString(s), this.width - 10, MC.fontRenderer, false, false));
          }
        } catch (RuntimeException ignored) {
        }
      }
    } else { // on enter
      String str = this.inputField.getText().trim();

      if (!str.isEmpty()) {
        this.inputField.setText("");
        List<String> sentHistory = getCorrectSentHistory();

        if (sentHistory.isEmpty() ||
              !sentHistory.get(sentHistory.size() - 1).equals(str)) {
          sentHistory.add(str);
        }

        this.sentHistoryCursor = -1;
        this.historyOffset = 0;
        this.runCommand(str);
      }
    }
  }

  private String getTabCompletableText() {
    StringBuilder out = new StringBuilder();
    String FH_PREFIX = ChatCommandService.getActivationCharacter().toString();
    String[] spl = this.inputField.getText().split(" ");

    for (int i = 0; i < spl.length - 1; i++) {
      out.append(spl[i]).append(" ");
    }

    if (spl.length == 1 && this.inputField.getText().startsWith(FH_PREFIX)) {
      out.append(FH_PREFIX);
    }

    return out.toString();
  }

  private String printCommandsRecursive(int depth, String[] args, Command cmd, List<Command> possibleMatches) {
    StringBuilder out = new StringBuilder();
    if (depth > args.length) {
      out.append(printFormatted(cmd, depth, 1, possibleMatches));
      return out.toString();
    } else if (depth < args.length && cmd.getClosestChild(args[depth]) != null) {
      out.append(printFormatted(cmd, depth, 1, possibleMatches));
      out.append(printCommandsRecursive(depth + 1, args, cmd.getClosestChild(args[depth]), possibleMatches));
    } else {
      out.append(printFormatted(cmd, depth, 1, possibleMatches));

      if (cmd.getChildren().size() > 40) out.append("[ ");
      
      for (Command c : cmd.getChildren())
        out.append(printFormatted(c, depth + 1, cmd.getChildren().size(), possibleMatches));

      if (cmd.getChildren().size() > 40) out.append("]");
    }

    return out.toString();
  }

  private String printFormatted(Command c, int depth, int size, List<Command> possibleMatches) {
    String out = "";
    if (depth == 0) return out;

    if (size > 40) {
      if (possibleMatches.contains(c))
        out += TextFormatting.WHITE.toString() + TextFormatting.BOLD + c.getName() + TextFormatting.RESET + " ";
      else out += c.getName() + " ";
    } else {
      if (possibleMatches.contains(c)) {
        out += depthArrow(depth) + TextFormatting.WHITE + TextFormatting.BOLD + c.getPrintText() + TextFormatting.RESET + "\n";
      } else out += depthArrow(depth) + c.getPrintText() + "\n";
    }

    return out;
  }

  private static String depthArrow(int depth) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < depth; i++) out.append(">");
    return out.append(" ").toString();
  }

  @Nullable
  private String getSentHistory(int offset) {
    int max = getCorrectSentHistory().size() - 1;
    this.sentHistoryCursor = MathHelper.clamp(this.sentHistoryCursor + offset, -1, max);
    if (this.sentHistoryCursor == -1) return null;
    return getCorrectSentHistory().get(max - this.sentHistoryCursor);
  }


  public void print(ITextComponent message, ClientMode target) {
    if (message.getUnformattedText().isEmpty()) return;
    message = BetterChat.addTimestamp(message);
    switch (target) {
      // case BARITONE: baritoneHistory.push(message); break;
      case IRC: IRCHistory.push(message); break;
      case CHAT: chatHistory.push(message); break;
      default: forgehaxHistory.push(message); break;
    }
    if (getWorld() == null) {
      // message won't go in GuiNewChat's history, add it to omni manually
      omniHistory.push(message);
    }
  }

  private void runCommand(String s) {
    try {
      ClientMode used_mode = mode;
      if (s.startsWith(ChatCommandService.getActivationCharacter().toString())) {
        used_mode = ClientMode.FORGEHAX;
      // } else if (s.startsWith("#")) {
      //   used_mode = ClientMode.BARITONE;
      } else if (s.startsWith(IRC.getPrefix())) {
        used_mode = ClientMode.IRC;
      } else if (s.startsWith("/")) {
        used_mode = ClientMode.CHAT;
      }

      switch (used_mode) {
        case FORGEHAX:
          if (s.startsWith(ChatCommandService.getActivationCharacter().toString())) {
            s = s.substring(1);
          }

          if (previousScreen instanceof ClickGui) {
            MC.displayGuiScreen(ClickGui.getInstance());
          }

          for (String comm : s.split(";")) // allow multiple commands split by ;
            ChatCommandService.handleCommand(comm);
          break;

        // case BARITONE: // TODO: Baritone API
        //   if (getWorld() != null) {
        //     if (s.startsWith("#")) {
        //       s = s.substring(1);
        //     }

        //     getLocalPlayer().sendChatMessage("#" + s);
        //   } else Helper.printError("You must be in game to run Baritone commands.");
        //   break;

        case CHAT:
          if (getWorld() != null)
            getLocalPlayer().sendChatMessage(s);
          else
            Helper.outputMessage(Helper.getFormattedText("[FH] ", TextFormatting.DARK_RED, true, false)
                                  .appendSibling(
                                    Helper.getFormattedText("can only send chat messages while in-game", TextFormatting.GRAY, false, false)
                                  ), ClientMode.CHAT);
        break;

        case IRC:
          if (!s.equals(IRC.getPrefix())) {
            if (s.startsWith(IRC.getPrefix())) {
              s = s.substring(1);
            }

            IRC.sendIRCMessage(s);
          } else IRC.printIRCSystem("Message is empty.");
          break;

        case OMNI:
          if (getWorld() != null)
            getLocalPlayer().sendChatMessage(s);
          else {
            if (!s.equals(IRC.getPrefix())) {
              if (s.startsWith(IRC.getPrefix())) {
                s = s.substring(1);
              }

              IRC.sendIRCMessage(s);
            } else IRC.printIRCSystem("Message is empty.");
          }
          break;
      }
    } catch (Throwable t) {
      Helper.printError(t.toString());
    }
  }

  public void onGuiClosed() {
    MC.gameSettings.chatVisibility = chatSetting;
    autoComplete = "";
    commandPreview.clear();
  }


  /**
   * Shit from below here is "skidded" and adapted from minecraft source code
   *  to allow clickable and hoverable components. Check GuiChat.java for originals
   */

  @Nullable
  public ITextComponent getChatComponent(int mouseX, int mouseY) {
    ScaledResolution scaledresolution = new ScaledResolution(MC);
    int i = scaledresolution.getScaleFactor();
    float f = 1f; // can't change our chat scale
    int j = mouseX / i - 4;
    int k = mouseY / i - 42;
    j = MathHelper.floor((float)j / f);
    k = MathHelper.floor((float)k / f);
    if (j >= 0 && k >= 0) {
      int l = Math.min(((this.height - 42) / 10), screenElements.size() + commandPreview.size());
      if (j <= MathHelper.floor((this.width - 4)) && k <= 10 * l)
      {
        int i1 = (k / 10) - commandPreview.size();
        if (i1 >= 0 && i1 < screenElements.size()) {
          ITextComponent start = screenElements.get(i1);
          int j1 = 0;
          if (MC.fontRenderer.getStringWidth(GuiUtilRenderComponents.removeTextColorsIfConfigured(((TextComponentString)start).getText(), false)) > j)
            return start;
          else {
            for (ITextComponent itextcomponent : start.getSiblings()) {
              if (itextcomponent instanceof TextComponentString) {
                j1 += this.mc.fontRenderer.getStringWidth(GuiUtilRenderComponents.removeTextColorsIfConfigured(((TextComponentString)itextcomponent).getText(), false));
                if (j1 > j) {
                  return itextcomponent;
                }
              }
            }
          }
        }
        return null;
      }
      else return null;
    }
    else return null;
  }
}
