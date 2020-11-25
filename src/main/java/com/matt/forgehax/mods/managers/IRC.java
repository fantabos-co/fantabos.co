package com.matt.forgehax.mods.managers;

import static com.matt.forgehax.Helper.getFileManager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.gui.PromptGui.ClientMode;
import com.matt.forgehax.mods.services.ChatCommandService;
import com.matt.forgehax.util.FileManager;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.SimpleTimer;
import com.matt.forgehax.util.color.ColorClamp;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.irc.ChannelEntry;
import com.matt.forgehax.util.irc.FormatConverter;
import com.matt.forgehax.util.irc.IrcParser;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.command.Command;

import net.minecraft.client.gui.GuiScreen;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import static com.matt.forgehax.Helper.getLog;

@RegisterMod
public class IRC extends ServiceMod {

  private static final Path BASE_PATH = getFileManager().getBaseResolve("irc");
  private static final File CHANNELS_FILE = BASE_PATH.resolve("channels.json").toFile();

  private static final HashMap<String, ChannelEntry> channelList = new HashMap<>();

  private static IRC INSTANCE = null;
  private Socket socket;
  private BufferedWriter writer;
  private final AtomicBoolean connected = new AtomicBoolean();
  private ServerHandler serverThread = null;
  private final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();
  private String usedNick = " ";
  private String connectedServer = "";
  private final List<String> capabilitiesList = new ArrayList<>();
  private GuiScreen lastScreen = null;
  private final SimpleTimer timer = new SimpleTimer();

  private final Setting<String> server =
    getCommandStub()
        .builders()
        .<String>newSettingBuilder()
        .name("server")
        .description("Server to connect to")
        .defaultTo("irc.2b2t.it")
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
              if (cb.getTo().equals("")) return;
              if (connected.get() && !cb.getTo().equals(usedNick)) sendRaw("NICK " + cb.getTo());
              usedNick = cb.getTo();
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

  private final Setting<String> realName =
    getCommandStub()
        .builders()
        .<String>newSettingBuilder()
        .name("real-name")
        .description("Real name, leave empty to use account name")
        .defaultTo("")
        .build();

  public final Setting<String> defaultChannel =
    getCommandStub()
        .builders()
        .<String>newSettingBuilder()
        .name("channel-default")
        .description("Where to send messages")
        .defaultTo("#fhchat")
        .build();

  // private final Options<ChannelEntry> channelList =
  //   getCommandStub()
  //       .builders()
  //       .<ChannelEntry>newOptionsBuilder()
  //       .name("saved-channels")
  //       .description("Contains channels to autoconnect to")
  //       .factory(ChannelEntry::new)
  //       .supplier(Sets::newConcurrentHashSet)
  //       .build();

  private final Setting<String> prefix =
    getCommandStub()
        .builders()
        .<String>newSettingBuilder()
        .name("prefix")
        .description("All chat messages starting with prefix will be sent to IRC")
        .defaultTo("@")
        .build();

  private final Setting<Boolean> ircOnly =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("irc-only")
        .description("Send all chat messages to IRC instead of game chat")
        .defaultTo(false)
        .build();

  private final Setting<Boolean> autoConnect =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("auto-connect")
        .description("Automatically connect to server")
        .defaultTo(false)
        .build();

  private final Setting<Integer> autoConnectTimer =
    getCommandStub()
        .builders()
        .<Integer>newSettingBuilder()
        .name("reconnect-timer")
        .description("Seconds to wait before reconnecting")
        .min(0)
        .defaultTo(10)
        .build();

  private final Setting<Boolean> logEverything =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("log-all")
        .description("Write everything received from IRC into MC log")
        .defaultTo(true)
        .build();

  private final Setting<Boolean> convertColorCodes =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("convert-color-codes")
        .description("Convert IRC color codes into minecraft TextFormatting")
        .defaultTo(true)
        .build();
  private final Setting<Boolean> colorNicks =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("color-nicks")
        .description("Color nicknames based on hash")
        .defaultTo(true)
        .build();
  private final Setting<Integer> hashShift =
    getCommandStub()
        .builders()
        .<Integer>newSettingBuilder()
        .name("hash-shift")
        .description("Make all nick colors different by bit shifting the hash")
        .min(0)
        .max(32)
        .defaultTo(0)
        .build();
  
  public IRC() {
    super("IRC", "IRC client built inside Minecraft");
    INSTANCE = this;
  }

  public static boolean isConnected() { return INSTANCE.connected.get(); }
  public static String getDefaultChannel() { return INSTANCE.defaultChannel.get(); }
  public static String getServer() { return INSTANCE.connectedServer; }
  public static String getNick() { return INSTANCE.usedNick; }
  public static String getPrefix() { return INSTANCE.prefix.get(); }
  public static void sendIRCMessage(String message) { INSTANCE.sendMessage(message); }

  private class ServerHandler extends Thread {
    public AtomicBoolean keepRunning = new AtomicBoolean();
    private BufferedReader reader;
    private String buf;

    public void run() {
      keepRunning.set(true);
      try {
        capabilitiesList.clear();
        connectedServer = server.get();
        socket = new Socket(connectedServer, 6667);
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String nickName = (nick.get().equals("") ? MC.getSession().getProfile().getName() : nick.get());
        String loginName = (login.get().equals("") ? MC.getSession().getProfile().getName() : login.get());
        String realNameDisplayed = (realName.get().equals("") ? MC.getSession().getProfile().getName() : realName.get());
        writer.write("NICK " + nickName + "\r\n");
        writer.write("USER " + loginName + " FH-IRC " + "TONIOinc :" + realNameDisplayed + "\r\n");
        writer.flush();
        usedNick = nickName; // ewww but works for now ig
        getLog().warn("Connecting to IRC with nick " + usedNick);

        while (keepRunning.get()) {
          buf = reader.readLine();
          if (buf == null) { // Connection ended
            break;
          }

          if (logEverything.get()) getLog().info(buf);
          if (convertColorCodes.get()) buf = FormatConverter.replaceColorCodes(buf);

          if (buf.startsWith("PING ")) { // Reply to ping
            writer.write("PONG " + buf.substring(5) + "\r\n");
            writer.flush();
          } else if (!connected.get()) { // We sent our login but we still need confirmation
            if (buf.startsWith(String.format(":%s 004", server.get()))) { // Server accepted it and sent back login info
              connected.set(true);

              for (String ckey : channelList.keySet()) {
                ChannelEntry c = channelList.get(ckey);
                joinChannel(c.getUniqueHeader(), c.getPassword());
              }

              Helper.printInform("Connected to IRC successfully");
            } else if (buf.startsWith(String.format(":%s 433", server.get()))) { // Server rejected our login
              printIRCError("Nick " + usedNick + " is already in use");
              break;
            }
          } else { // We are connected to the server
            messages.add(buf);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

      printIRCError("Disconnected from server");
      timer.start();
      connected.set(false);
      keepRunning.set(false);
    }
  }

  @Override
  protected void onUnload() {
    disconnect();
    // Serialize
    //  irc channels are not part of the config file
    JsonObject channels_out = new JsonObject();
    for (String fname : channelList.keySet())
      channelList.get(fname).serialize(channels_out);
    FileManager.save(CHANNELS_FILE, channels_out);
  }

  @Override
  protected void onLoad() {
    // Deserialize
    // irc channels are not part of the config file

    JsonObject chans = FileManager.load(CHANNELS_FILE);
    if (chans != null) {
      for (Map.Entry<String, JsonElement> e : chans.entrySet()) {
        ChannelEntry newChan = new ChannelEntry(e.getKey());
        newChan.deserialize(chans);
        channelList.put(newChan.getUniqueHeader(), newChan);
      }
    }

    timer.start();
    connected.set(false);

    Command commands_root = getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("command")
        .description("Send a raw IRC command (wrap it in \"\") or use macro subcommands")
        .requiredArgs(1)
        .processor(data -> sendRaw(data.getArgumentAsString(0)))
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("connect")
        .description("Connect to server")
        .processor(data -> connect())
        .build();
        
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("disconnect")
        .description("Disconnect from server")
        .processor(data -> disconnect())
        .build();

    commands_root
        .builders()
        .newCommandBuilder()
        .name("join")
        .description("Join a channel, or all default if none is provided")
        .processor(
            data -> { // maybe there's a nicer way to do this?
              if (data.getArgumentCount() > 1) {
                joinChannel(data.getArgumentAsString(0), data.getArgumentAsString(1));
              } else if (data.getArgumentCount() > 0) {
                joinChannel(data.getArgumentAsString(0));
              } else {
                for (String ckey : channelList.keySet()) {
                  ChannelEntry c = channelList.get(ckey);
                  joinChannel(c.getUniqueHeader(), c.getPassword());
                }
              }
            })
        .build();

   commands_root
        .builders()
        .newCommandBuilder()
        .name("part")
        .description("Leave a channel, or default channel if none is provided")
        .processor(
            data -> { // maybe there's a nicer way to do this?
              if (data.getArgumentCount() > 0) {
                sendRaw("PART " + data.getArgumentAsString(0));
              } else {
                sendRaw("PART " + defaultChannel.get());
              }
            })
        .build();

    commands_root
        .builders()
        .newCommandBuilder()
        .name("msg")
        .description("Send a message to target channel or user")
        .requiredArgs(2)
        .processor(
            data -> {
              String target = data.getArgumentAsString(0);
              StringBuilder msg = new StringBuilder();

              for (int i = 1; i < data.getArgumentCount(); i++) {
                msg.append(data.getArgumentAsString(i)).append(" ");
              }

              sendMessage(target, msg.toString());
            })
        .build();

    commands_root
        .builders()
        .newCommandBuilder()
        .name("whois")
        .description("Shows details about a user")
        .requiredArgs(1)
        .processor(data -> sendRaw("WHOIS " + data.getArgumentAsString(0)))
        .build();

    commands_root
        .builders()
        .newCommandBuilder()
        .name("names")
        .description("List connected users")
        .processor(
            data -> {
              if (data.getArgumentCount() > 0) {
                sendRaw("NAMES " + data.getArgumentAsString(0));
              } else {
                sendRaw("NAMES " + defaultChannel.get());
              }
            })
        .build();

    commands_root
        .builders()
        .newCommandBuilder()
        .name("stats")
        .description("Show stats about the current server")
        .processor(data -> sendRaw("LUSERS"))
        .build();

    commands_root
        .builders()
        .newCommandBuilder()
        .name("list")
        .description("List channels on server")
        .processor(data -> sendRaw("LIST"))
        .build();

    commands_root
        .builders()
        .newCommandBuilder()
        .name("capabilities")
        .description("Show server capabilities")
        .processor(
            data -> {
              for (String s : capabilitiesList) {
                printIRCSystem(s);
              }
            })
        .build();

    Command chanlist_root = getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("saved-channels")
        .description("Show all registered channels (but not their password)")
        .processor(data -> {
          for (String ckey : channelList.keySet())
            data.write(ckey);
          data.write(String.format("Saved channels: %d", channelList.size()));
        })
        .build();

    chanlist_root
        .builders()
        .newCommandBuilder()
        .name("add")
        .description("Add a channel to autojoin (with eventually a password)")
        .requiredArgs(1)
        .processor(
            data -> { // maybe there's a nicer way to do this?
              String channel = data.getArgumentAsString(0);
              String pwd = "";
              if (data.getArgumentCount() > 1) {
                pwd = data.getArgumentAsString(1);
              }

              channelList.put(channel, new ChannelEntry(channel, pwd));
              if (connected.get()) {
                joinChannel(channel, pwd);
              }
            })
        .build();

    chanlist_root
        .builders()
        .newCommandBuilder()
        .name("remove")
        .description("Remove a channel by name")
        .requiredArgs(1)
        .processor(data -> {
          data.requiredArguments(1);
          channelList.remove(data.getArgument(0));
          Helper.printLog("Removed channel " + data.getArgument(0));
        })
        .build();
  }

  private void connect() {
    if (connected.get() || (serverThread != null && serverThread.isAlive() && serverThread.keepRunning.get())) {
      Helper.printWarning("Already connected or connecting");
      return;
    }
    serverThread = new ServerHandler();
    serverThread.start();
  }

  private void disconnect() {
    if (connected.get()) {
      quit(FormatConverter.getStateFromLastScreen(lastScreen));
      timer.start();
      connected.set(false);
    }
  }

  private void quit() { quit(""); }
  private void quit(String msg) {
    sendRaw("QUIT :" + msg);
    if (serverThread != null && serverThread.isAlive()) {
      serverThread.keepRunning.set(false);
    }
  }

  private void joinChannel(String channel, String password) {
    if (password.equals("")) {
      joinChannel(channel);
    } else sendRaw("JOIN " + channel + " " + password);
  }

  private void joinChannel(String channel) {
    sendRaw("JOIN " + channel);
  }

  private void sendMessage(String message) { sendMessage(defaultChannel.get(), message); }
  public void sendMessage(String target, String message) {
    if (sendRaw("PRIVMSG " + target + " :" + message))
      printFormattedIRC(usedNick, target, message);
  }

  private boolean sendRaw(String content) {
    if (!connected.get()) {
      printIRCError("Not connected");
      return false;
    }

    if (convertColorCodes.get()) {
      content = FormatConverter.convertToIRCColor(content);
    }

    try {
      writer.write(content + "\r\n");
      writer.flush();
      return true;
    } catch (IOException e) {
      printIRCError("Could not send payload");
      return false;
    }
  }

  private void parseIRCchat(String msgIn) {
    try {
      String author = msgIn.split("!", 2)[0].substring(1);
      String[] buf = msgIn.split("PRIVMSG", 2)[1].split(":", 2);
      String message = buf[1];
      String destination = buf[0].replace(" ", "");
      printFormattedIRC(author, destination, message);
    } catch (RuntimeException e) {
      e.printStackTrace();
      printIRCSystem(msgIn);
    }
  }

  //
  //
  //

  public static void printIRCError(String text) {
    Helper.outputMessage(Helper.getFormattedText("[IRC] ", TextFormatting.DARK_RED, true, false, null, getHover())
        .appendSibling(
          Helper.getFormattedText(text, TextFormatting.GRAY, false, true)
      ), ClientMode.IRC);
  }

  public static void printIRCSystem(String text) {
    Helper.outputMessage(Helper.getFormattedText("[IRC] ", TextFormatting.DARK_PURPLE, true, false, null, getHover())
        .appendSibling(
          Helper.getFormattedText(text, TextFormatting.DARK_GRAY, false, true)
      ), ClientMode.IRC);
  }

  private static HoverEvent getHover() {
    return new HoverEvent(Action.SHOW_TEXT,
      Helper.getFormattedText("Server: ", TextFormatting.DARK_PURPLE, true, false)
        .appendSibling(
          Helper.getFormattedText(IRC.getServer(), TextFormatting.GRAY, false, false)
        )
    );
  }

  private String addHashColor(String in) {
    if (!colorNicks.get()) return in;
    int raw_color = in.hashCode();
    int hash_shift = hashShift.get();
    raw_color = (raw_color >>> hash_shift) | (raw_color << (32 - hash_shift));
    TextFormatting color = ColorClamp.getClampedColor(raw_color);
    if (color.equals(TextFormatting.BLACK))
      color = TextFormatting.DARK_GRAY; // hash can result in black but makes nick unreadable
    return color + in + TextFormatting.GRAY;
  }

  public static void printFormattedIRC(String author, String target, String message) {
    author = INSTANCE.addHashColor(author);

    final String FH_PREFIX = ChatCommandService.getActivationCharacter().toString();

    Helper.outputMessage(
        Helper.getFormattedText("[" + target + "] ", TextFormatting.DARK_PURPLE, true, false,
             new ClickEvent(ClickEvent.Action.RUN_COMMAND, FH_PREFIX + "irc channel-default " + target), null)
          .appendSibling(
            Helper.getFormattedText(String.format("<%s>", author), TextFormatting.GRAY, false, false,
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, FH_PREFIX + "irc msg " + author + " "), null)
                .appendSibling(
                  Helper.getFormattedText(" ", TextFormatting.WHITE, false, false)
                    .appendSibling(ForgeHooks.newChatWithLinks(message))
                )
          ), ClientMode.IRC);
  }

  //
  //
  //
  
  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    lastScreen = Helper.getCurrentScreen();
    if (!connected.get() && autoConnect.get() && timer.hasTimeElapsed(autoConnectTimer.get() * 1000L)) {
      timer.start();
      connect();
    }

    while (messages.size() > 0) {
      String buf = messages.poll();
      if (buf.startsWith(String.format(":%s 005", server.get()))) { // capabilities message
        capabilitiesList.add(buf);
        // printIRCSystem(buf); // print it without stripping anything
      } else if (buf.contains("End of /NAMES list") ||
                 buf.contains("End of /WHOIS list") ||
                 buf.contains("End of /LIST") ||
                 buf.contains("NOTICE * :***")) {
        // ignore
      } else if (buf.contains("PRIVMSG")) {
        parseIRCchat(buf);
      } else if (buf.contains("JOIN")) {
        printIRCSystem(IrcParser.parseIRCjoin(buf));
      } else if (buf.contains("PART") || buf.contains("QUIT")) {
        printIRCSystem(IrcParser.parseIRCleave(buf));
      } else if (buf.contains("NICK")) {
        printIRCSystem(IrcParser.parseIRCnickChange(buf));
      } else if (buf.contains("MODE")) {
        printIRCSystem("Mode: " + buf.split("MODE")[1]); // TODO parse better
      } else if (buf.contains(usedNick)) {
        printIRCSystem(buf.split(usedNick, 2)[1]);
      }
    }
  }

  @SubscribeEvent
  public void onPacketSent(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof CPacketChatMessage && !PacketHelper.isIgnored(event.getPacket())) {
      String inputMessage = ((CPacketChatMessage) event.getPacket()).getMessage();

      if (inputMessage.startsWith("/")) return;

	    if (ircOnly.get() || inputMessage.startsWith(prefix.get())) {
        event.setCanceled(true);
        String msg = (ircOnly.get() ? inputMessage : inputMessage.replaceFirst(prefix.get(), ""));
        sendMessage(defaultChannel.get(), msg);
      }
    }
  }
}
