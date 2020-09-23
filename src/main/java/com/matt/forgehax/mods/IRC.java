package com.matt.forgehax.mods;

import com.google.common.collect.Sets;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.gui.ClickGui;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.SimpleTimer;
import com.matt.forgehax.util.color.ColorClamp;
import com.matt.forgehax.util.command.Options;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.serialization.ISerializableJson;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenServerList;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.HoverEvent.Action;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@RegisterMod
public class IRC extends ToggleMod {

  public static IRC INSTANCE = null;
  private Socket socket;
  private BufferedWriter writer;
  private AtomicBoolean connected = new AtomicBoolean(); 
  private ServerHandler server_thread = null;
  private ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();
  private String used_nick = " ";
  private String connected_server = "";
  private GuiScreen last_screen = null;
  private SimpleTimer timer = new SimpleTimer();

  private final Setting<String> server =
      getCommandStub()
          .builders()
          .<String>newSettingBuilder()
          .name("server")
          .description("Server to connect to")
          .defaultTo("")
          .build();
  private final Setting<String> nick =
      getCommandStub()
          .builders()
          .<String>newSettingBuilder()
          .name("nick")
          .description("Nickname to use, leave empty to use MC name")
          .defaultTo("")
          .changed(
              cb -> {
                if (connected.get())
                  sendRaw("NICK " + cb.getTo());
                used_nick = cb.getTo();
              })
          .build();
  private final Setting<String> login =
      getCommandStub()
          .builders()
          .<String>newSettingBuilder()
          .name("login")
          .description("Login name, leave empty to use MC name")
          .defaultTo("")
          .build();
  private final Setting<String> default_channel =
      getCommandStub()
          .builders()
          .<String>newSettingBuilder()
          .name("channel-default")
          .description("Where to send messages")
          .defaultTo("#fhchat")
          .build();

  public final Options<ChannelEntry> channel_list =
    getCommandStub()
        .builders()
        .<ChannelEntry>newOptionsBuilder()
        .name("saved-channels")
        .description("Contains channels to autoconnect to")
        .factory(ChannelEntry::new)
        .supplier(Sets::newConcurrentHashSet)
        .build();

  public final Setting<String> prefix =
      getCommandStub()
          .builders()
          .<String>newSettingBuilder()
          .name("prefix")
          .description("All chat messages starting with prefix will be sent to IRC")
          .defaultTo("@")
          .build();
  private final Setting<Boolean> irc_only =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("irc-only")
          .description("Send all chat messages to IRC instead of game chat")
          .defaultTo(false)
          .build();
  private final Setting<Boolean> autoconnect =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("autoconnect")
          .description("Automatically connect to server")
          .defaultTo(true)
          .build();
  private final Setting<Integer> autoconnect_timer =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("reconnect-timer")
          .description("Seconds to wait before reconnecting")
          .min(0)
          .max(120)
          .defaultTo(10)
          .build();
  private final Setting<Boolean> log_everything =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("log-all")
          .description("Write everything received from IRC into MC log")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> strip_whitespace =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("strip-whitespace")
          .description("Remove Zero-Width-White-Space characters")
          .defaultTo(false)
          .build();
  private final Setting<Boolean> convert_color_codes =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("convert-color-codes")
          .description("Convert IRC color codes into minecraft TextFormatting")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> color_nicks =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("color-nicks")
          .description("Color nicknames based on hash")
          .defaultTo(true)
          .build();
  
  public IRC() {
    super(Category.CHAT, "IRC", false, "IRC client built inside minecraft chat");
    INSTANCE = this;
  }

  public boolean isConnected() { return connected.get(); }
  public String getDefaultChannel() { return default_channel.get(); }
  public String getServer() { return connected_server; }

  private class ServerHandler extends Thread {
    public AtomicBoolean keep_running = new AtomicBoolean();
    private BufferedReader reader;
    private String buf;

    public void run() {
      keep_running.set(true);
      try {
        connected_server = server.get();
        socket = new Socket(connected_server, 6667);
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String nick_name = (nick.get().equals("") ? MC.getSession().getProfile().getName() : nick.get());
        String login_name = (login.get().equals("") ? MC.getSession().getProfile().getName() : login.get());
        String real_name_displayed = MC.getSession().getProfile().getName();
        writer.write("NICK " + nick_name + "\r\n");
        writer.write("USER " + login_name + " FH-IRC " + "TONIOinc :" + real_name_displayed + "\r\n");
        writer.flush();
        used_nick = nick_name; // ewww but works for now ig
        LOGGER.warn("Connecting to IRC with nick " + used_nick);

        while (keep_running.get()) {
          buf = reader.readLine();
          if (buf == null) { // Connection ended
            break;
          }
          if (log_everything.get()) LOGGER.info(buf);
          if (convert_color_codes.get()) buf = replaceColorCodes(buf);
          if (strip_whitespace.get()) buf = buf.replace("\u200b", "").replace("\u0002", "");
          if (buf.startsWith("PING ")) { // Reply to ping
            writer.write("PONG " + buf.substring(5) + "\r\n");
            writer.flush();
          } else if (!connected.get()) { // We sent our login but we still need confirmation
            if (buf.startsWith(String.format(":%s 004", server.get()))) { // Server accepted it and sent back login info
              connected.set(true);
              for (ChannelEntry channel : channel_list) {
                join_channel(channel.getUniqueHeader(), channel.getPassword());
              }
              Helper.printInform("Connected to IRC successfully");
            } else if (buf.startsWith(String.format(":%s 433", server.get()))) { // Server rejected our login
              printIRCSystem("Nick " + used_nick + " is already in use");
              break;
            }
          } else { // We are connected to the server
            messages.add(buf);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      printIRCSystem("Disconnected from server");
      timer.start();
      connected.set(false);
      keep_running.set(false);
    }
  }

  @Override
  protected void onLoad() {
    timer.start();
    connected.set(false);

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("connect")
        .description("Connect to server")
        .processor(data -> {
          if (!this.isEnabled())
            Helper.printWarning("Please enable IRC mod");
          else
            connect();
        })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("join")
        .description("Join a channel, or all default if none is provided")
        .processor(
            data -> { // maybe there's a nicer way to do this?
              if (data.getArgumentCount() > 1)
                join_channel(data.getArgumentAsString(0), data.getArgumentAsString(1));
              else if (data.getArgumentCount() > 0)
                join_channel(data.getArgumentAsString(0));
              else {
                for (ChannelEntry channel : channel_list) {
                  join_channel(channel.getUniqueHeader(), channel.getPassword());
                }
              }
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("part")
        .description("Leave a channel, or default channel if none is provided")
        .processor(
            data -> { // maybe there's a nicer way to do this?
              if (data.getArgumentCount() > 0)
                sendRaw("PART " + data.getArgumentAsString(0));
              else {
                sendRaw("PART " + default_channel.get());
              }
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("msg")
        .description("Send a message to target channel or user")
        .processor(
            data -> {
              data.requiredArguments(2);
              String target = data.getArgumentAsString(0);
              String msg = "";
              for (int i=1; i< data.getArgumentCount(); i++)
                msg += data.getArgumentAsString(i) + " ";
              sendMessage(target, msg);
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("whois")
        .description("Shows details about a user")
        .processor(
            data -> {
              data.requiredArguments(1);
              sendRaw("WHOIS " + data.getArgumentAsString(0));
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("names")
        .description("List connected users")
        .processor(
            data -> {
              if (data.getArgumentCount() > 0)
                sendRaw("NAMES " + data.getArgumentAsString(0));
              else
                sendRaw("NAMES " + default_channel.get());
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("raw")
        .description("Send a raw IRC payload (wrap it in \" \")")
        .processor(
            data -> {
              data.requiredArguments(1);
              sendRaw(data.getArgumentAsString(0));
            })
        .build();

    channel_list
        .builders()
        .newCommandBuilder()
        .name("add")
        .description("Add a channel to autojoin (with eventually a password)")
        .processor(
            data -> { // maybe there's a nicer way to do this?
              data.requiredArguments(1);
              if (data.getArgumentCount() > 1)
                channel_list.add(new ChannelEntry(data.getArgumentAsString(0), data.getArgumentAsString(1)));
              else channel_list.add(new ChannelEntry(data.getArgumentAsString(0)));
            })
        .build();

    channel_list
        .builders()
        .newCommandBuilder()
        .name("remove")
        .description("Remove a channel by name")
        .processor(
            data -> { 
              data.requiredArguments(1);
              channel_list.removeIf(c -> c.getUniqueHeader().equals(data.getArgument(0)));
            })
        .build();

    channel_list
        .builders()
        .newCommandBuilder()
        .name("list")
        .description("Show all registered channels (but not their pwd)")
        .processor(data -> {
          for (ChannelEntry entry : channel_list) {
            data.write(entry.name);
          }
          data.write(String.format("number: %d", channel_list.size()));
        })
        .build();
  }

  @Override
  protected void onEnabled() {
    timer.start();
  }

  @Override
  protected void onDisabled() {
    if (connected.get()) {
      quit(getStateFromLastScreen(last_screen));
      timer.start();
      connected.set(false);
    }
  }

  private void connect() {
    if (connected.get() || (server_thread != null && server_thread.isAlive() && server_thread.keep_running.get())) {
      Helper.printWarning("Already connected or connecting");
      return;
    }
    server_thread = new ServerHandler();
    server_thread.start();
  }

  private void quit() { quit(""); }
  private void quit(String msg) {
    sendRaw("QUIT :" + msg);
    if (server_thread != null && server_thread.isAlive()) {
      server_thread.keep_running.set(false);
    }
  }

  private void join_channel(String channel, String password) {
    if (password.equals("")) join_channel(channel);
    else sendRaw("JOIN " + channel + " " + password);
  }

  private void join_channel(String channel) {
    sendRaw("JOIN " + channel);
  }

  public void sendMessage(String message) {
    sendMessage(default_channel.get(), message);
  }

  public void sendMessage(String target, String message) {
    if (convert_color_codes.get())
      message = replaceUserFriendlyCodes(message);
    if (sendRaw("PRIVMSG " + target + " :" + message)) {
      printFormattedIRC(used_nick, target, message);
    }
  }

  private boolean sendRaw(String content) {
    if (!connected.get()) {
      printIRCSystem("Not connected");
      return false;
    }
    if (!this.isEnabled()) {
      Helper.printError("Please enable IRC mod");
      return false;
    }
    if (convert_color_codes.get())
      content = convertToIRCColor(content);
    try {
      writer.write(content + "\r\n");
      writer.flush();
      return true;
    } catch (IOException e) {
      printIRCSystem("Could not send payload");
      return false;
    }
  }

  private void parseIRCchat(String msgIn) {
    try {
      String author = msgIn.split("!", 2)[0].substring(1);
      String[] buf = msgIn.split("PRIVMSG", 2)[1].split(":", 2);
      String message = buf[1];
      String dest = buf[0].replace(" ", "");
      printFormattedIRC(author, dest, message);
    } catch (RuntimeException e) {
      e.printStackTrace();
      printIRCSystem(msgIn);
    }
  }

  private String parseIRCjoin(String msgIn) {
    try {
      String channel = "#" + msgIn.split(":#")[1];
      return msgIn.split("!", 2)[0].substring(1) + " joined " + channel;
    } catch (Exception e) {
      e.printStackTrace();
      return msgIn;
    }
  }

  private String parseIRCleave(String msgIn) {
    try {
      String buf;
      if (msgIn.contains("QUIT")) {
        buf = msgIn.split("!", 2)[0].substring(1) + " disconnected: ";
        buf += msgIn.split("QUIT", 2)[1].substring(2);
      } else {
        String channel = "#" + msgIn.split("#")[1];
        buf = msgIn.split("!", 2)[0].substring(1) + " left " + channel;
      }
      return buf;
    } catch (Exception e) {
      e.printStackTrace();
      return msgIn;
    }
  }

  private String parseIRCnickChange(String msgIn) {
    try {
      return msgIn.replace(":", "").replace("NICK", "changed their nick to");
    } catch (RuntimeException e) {
      e.printStackTrace();
      return msgIn;
    }
  }

  public static void printIRCSystem(String text) {
    Helper.outputMessage(Helper.getFormattedText("[IRC] ", TextFormatting.DARK_PURPLE, true, false, null, getHover())
        .appendSibling(
          Helper.getFormattedText(text, TextFormatting.DARK_GRAY, false, true)
      )
    );
  }

  private static HoverEvent getHover() {
    if (INSTANCE == null) return null;
    return new HoverEvent(Action.SHOW_TEXT,
      Helper.getFormattedText("Server : ", TextFormatting.DARK_PURPLE, true, false)
        .appendSibling(
          Helper.getFormattedText(INSTANCE.connected_server, TextFormatting.GRAY, false, false)
        )
    );
  }

  public static void printFormattedIRC(String author, String target, String message) {
    if (INSTANCE != null && INSTANCE.color_nicks.get()) {
      author = ColorClamp.getClampedColor(author.hashCode()) + author + TextFormatting.GRAY;
    }
    Helper.outputMessage(
        Helper.getFormattedText("[" + target + "] ", TextFormatting.DARK_PURPLE, true, false,
             new ClickEvent(ClickEvent.Action.RUN_COMMAND, ".irc channel-default " + target), null)
          .appendSibling(
            Helper.getFormattedText(String.format("<%s>", author), TextFormatting.GRAY, false, false,
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, ".irc msg " + author + " "), null)
                .appendSibling(
                  Helper.getFormattedText(" ", TextFormatting.WHITE, false, false)
                    .appendSibling(
                      ForgeHooks.newChatWithLinks(message)
                    )
                )
          )
    );
  }

  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    last_screen = Helper.getCurrentScreen();
    if (!connected.get() && autoconnect.get() && timer.hasTimeElapsed(autoconnect_timer.get() * 1000L)) {
      timer.start();
      connect();
    }
    while (messages.size() > 0) {
      String buf = messages.poll();
      if (buf.startsWith(String.format(":%s 005", server.get()))) { // capabilities message
        printIRCSystem(buf); // print it without stripping anything
      } else if (buf.contains("End of /NAMES list") || buf.contains("NOTICE * :***")) {
        // ignore
      } else if (buf.contains("PRIVMSG")) {
        parseIRCchat(buf);
      } else if (buf.contains("JOIN")) {
        printIRCSystem(parseIRCjoin(buf));
      } else if (buf.contains("PART") || buf.contains("QUIT")) {
        printIRCSystem(parseIRCleave(buf));
      } else if (buf.contains("NICK")) {
        printIRCSystem(parseIRCnickChange(buf));
      } else if (buf.contains(used_nick)) {
        printIRCSystem(buf.split(used_nick, 2)[1]);
      }
    }
  }

  @SubscribeEvent
  public void onPacketSent(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof CPacketChatMessage
        && !PacketHelper.isIgnored(event.getPacket())) {
      String inputMessage = ((CPacketChatMessage) event.getPacket()).getMessage();
      if (inputMessage.startsWith("/")) return;
	    if (irc_only.get() || inputMessage.startsWith(prefix.get())) {
        event.setCanceled(true);
        String msg = (irc_only.get() ? inputMessage : inputMessage.replaceFirst(prefix.get(), ""));
        sendMessage(default_channel.get(), msg);
      }
    }
  }

  private String replaceColorCodes(String msgIn) {
    return msgIn.replace("\u000315", TextFormatting.GRAY.toString())
                .replace("\u000314", TextFormatting.DARK_GRAY.toString())
                .replace("\u000313", TextFormatting.LIGHT_PURPLE.toString())
                .replace("\u000312", TextFormatting.BLUE.toString())
                .replace("\u000311", TextFormatting.AQUA.toString())
                .replace("\u000310", TextFormatting.DARK_AQUA.toString())
                .replace("\u00039", TextFormatting.GREEN.toString())
                .replace("\u00038", TextFormatting.YELLOW.toString())
                .replace("\u00037", TextFormatting.GOLD.toString())
                .replace("\u00036", TextFormatting.DARK_PURPLE.toString())
                .replace("\u00035", TextFormatting.DARK_RED.toString())
                .replace("\u00034", TextFormatting.RED.toString())
                .replace("\u00033", TextFormatting.DARK_GREEN.toString())
                .replace("\u00032", TextFormatting.DARK_BLUE.toString())
                .replace("\u00031", TextFormatting.BLACK.toString()) // Fucking js bot adding useless characters
                .replace("\u000309", TextFormatting.GREEN.toString())
                .replace("\u000308", TextFormatting.YELLOW.toString())
                .replace("\u000307", TextFormatting.GOLD.toString())
                .replace("\u000306", TextFormatting.DARK_PURPLE.toString())
                .replace("\u000305", TextFormatting.DARK_RED.toString())
                .replace("\u000304", TextFormatting.RED.toString())
                .replace("\u000303", TextFormatting.DARK_GREEN.toString())
                .replace("\u000302", TextFormatting.DARK_BLUE.toString())
                .replace("\u000301", TextFormatting.BLACK.toString())
                .replace("\u000300", TextFormatting.RESET.toString())
                .replace("\u00030", TextFormatting.RESET.toString())
                .replace("\u0003", TextFormatting.RESET.toString())
                .replace("\u000F", TextFormatting.RESET.toString())
                .replace("\u0002", TextFormatting.BOLD.toString())
                .replace("\u001D", TextFormatting.ITALIC.toString())
                .replace("\u001F", TextFormatting.UNDERLINE.toString());
                // .replace("\u001E", TextFormatting.STRIKETHROUGH.toString()); // unsupported
  }

  private String convertToIRCColor(String msgIn) {
    return msgIn.replace(TextFormatting.GRAY.toString(), "\u000315")
                .replace(TextFormatting.DARK_GRAY.toString(), "\u000314")
                .replace(TextFormatting.LIGHT_PURPLE.toString(), "\u000313")
                .replace(TextFormatting.BLUE.toString(), "\u000312")
                .replace(TextFormatting.AQUA.toString(), "\u000311")
                .replace(TextFormatting.DARK_AQUA.toString(), "\u000310")
                .replace(TextFormatting.GREEN.toString(), "\u00039")
                .replace(TextFormatting.YELLOW.toString(), "\u00038")
                .replace(TextFormatting.GOLD.toString(), "\u00037")
                .replace(TextFormatting.DARK_PURPLE.toString(), "\u00036")
                .replace(TextFormatting.DARK_RED.toString(), "\u00035")
                .replace(TextFormatting.RED.toString(), "\u00034")
                .replace(TextFormatting.DARK_GREEN.toString(), "\u00033")
                .replace(TextFormatting.DARK_BLUE.toString(), "\u00032")
                .replace(TextFormatting.BLACK.toString(), "\u00031")
                .replace(TextFormatting.RESET.toString(), "\u000F")
                .replace(TextFormatting.BOLD.toString(), "\u0002")
                .replace(TextFormatting.ITALIC.toString(), "\u001D")
                .replace(TextFormatting.UNDERLINE.toString(), "\u001F");
                // .replace(TextFormatting.STRIKETHROUGH.toString(), "\u001E"); // unsupported
  }

  private String replaceUserFriendlyCodes(String msgIn) {
    return msgIn.replace("&7", TextFormatting.GRAY.toString())
                .replace("&8", TextFormatting.DARK_GRAY.toString())
                .replace("&d", TextFormatting.LIGHT_PURPLE.toString())
                .replace("&9", TextFormatting.BLUE.toString())
                .replace("&b", TextFormatting.AQUA.toString())
                .replace("&3", TextFormatting.DARK_AQUA.toString())
                .replace("&a", TextFormatting.GREEN.toString())
                .replace("&e", TextFormatting.YELLOW.toString())
                .replace("&6", TextFormatting.GOLD.toString())
                .replace("&5", TextFormatting.DARK_PURPLE.toString())
                .replace("&4", TextFormatting.DARK_RED.toString())
                .replace("&c", TextFormatting.RED.toString())
                .replace("&2", TextFormatting.DARK_GREEN.toString())
                .replace("&1", TextFormatting.DARK_BLUE.toString())
                .replace("&0", TextFormatting.BLACK.toString())
                .replace("&f", TextFormatting.RESET.toString())
                .replace("&r", TextFormatting.RESET.toString())
                .replace("&l", TextFormatting.BOLD.toString())
                .replace("&o", TextFormatting.ITALIC.toString())
                .replace("&n", TextFormatting.UNDERLINE.toString());
                // .replace("&m", TextFormatting.STRIKETHROUGH.toString()); // unsupported
  }

  private String getStateFromLastScreen(GuiScreen in) {
    if (in == null) return "Disabled while in-game";
    if (in instanceof ClickGui) return "Disabled from GUI";
    if (in instanceof GuiChat) return "Disabled from chat";
    if (in instanceof GuiMainMenu) return "Quit Minecraft";
    if (in instanceof GuiScreenServerList) return "Disabled while browsing servers?";
    if (in instanceof GuiDisconnected) return "Disabled after being disconnected";
    if (in instanceof GuiConnecting) return "Disabled while connecting";
    if (in instanceof GuiIngameMenu) return "Rage quit! Not even back to main menu!";
    if (in instanceof GuiInventory) return "Items too strong, mod disabled";
    return String.format("Disconnected in an unknown screen (%s), woot!", in.getClass().getSimpleName());
  }

  private static class ChannelEntry implements ISerializableJson {
    final String name;
    private String password;

    ChannelEntry(String name) {
      this.name = name;
      this.password = "";
    }

    ChannelEntry(String name, String password) {
      this.name = name;
      this.password = password;
    }

    public String getPassword() {
      return this.password;
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
      writer.value(this.password);
    }

    @Override
    public void deserialize(JsonReader reader)  {
      this.password  = new JsonParser().parse(reader).getAsString();
    }

    @Override
    public String getUniqueHeader() {
      return this.name;
    }

    @Override
    public String toString() {
      return getUniqueHeader();
    }
  }
}
