package com.matt.forgehax.mods;

import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.util.text.TextFormatting;
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
  private final Setting<String> real_name =
      getCommandStub()
          .builders()
          .<String>newSettingBuilder()
          .name("real-name")
          .description("Real name, leave empty to use account name")
          .defaultTo("")
          .build();
  public final Setting<String> channel =
      getCommandStub()
          .builders()
          .<String>newSettingBuilder()
          .name("channel")
          .description("Channel to join on login")
          .defaultTo("#fhchat")
          .build();
  public final Setting<String> default_channel_password =
      getCommandStub()
          .builders()
          .<String>newSettingBuilder()
          .name("password")
          .description("If your default channel has a password, you can put it here")
          .defaultTo("")
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
          .description("Automatically connect when mod is enabled")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> log_everything =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("log-all")
          .description("Write everything received from IRC into MC log")
          .defaultTo(true)
          .build();
  private final Setting<Boolean> show_system =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("system")
          .description("Show system messages too in chat")
          .defaultTo(false)
          .build();
  private final Setting<Boolean> strip_whitespace =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("strip-whitespace")
          .description("Remove Zero-Width-White-Space characters")
          .defaultTo(false)
          .build();
  
  public IRC() {
    super(Category.CHAT, "IRC", false, "IRC client built inside minecraft chat");
    INSTANCE = this;
  }

  public static IRC INSTANCE;
  private Socket socket;
  private BufferedWriter writer;
  private AtomicBoolean connected = new AtomicBoolean(); 
  private ServerHandler server_thread = null;
  private ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();
  private String used_nick = " ";
  private boolean loaded = false; // need this because otherwise it connects before your nick/name settings are loaded

  private class ServerHandler extends Thread {
    public AtomicBoolean keep_running = new AtomicBoolean();
    private BufferedReader reader;
    private String buf;

    public void run() {
      keep_running.set(true);
      try {
        socket = new Socket(server.get(), 6667);
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String nick_name = (nick.get().equals("") ? MC.getSession().getProfile().getName() : nick.get());
        String login_name = (login.get().equals("") ? MC.getSession().getProfile().getName() : login.get());
        String real_name_displayed = (real_name.get().equals("") ? MC.getSession().getProfile().getName() : real_name.get());
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
          if (strip_whitespace.get()) buf = buf.replace("\u200b", "").replace("\u0002", "");
          if (buf.startsWith("PING ")) { // Reply to ping
            writer.write("PONG " + buf.substring(5) + "\r\n");
            writer.flush();
          } else if (!connected.get()) { // We sent our login but we still need confirmation
            if (buf.startsWith(String.format(":%s 004", server.get()))) { // Server accepted it and sent back login info
              connected.set(true);
              if (default_channel_password.get().equals(""))
                join_channel(channel.get());
              else join_channel(channel.get(), default_channel_password.get());
              Helper.printInform("Connected to IRC successfully");
            } else if (buf.startsWith(String.format(":%s 433", server.get()))) { // Server rejected our login
              Helper.printError("IRC Nickname already in use");
              break;
            }
          } else { // We are connected to the server
            messages.add(buf);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      Helper.printError("Disconnected from IRC");
      connected.set(false);
      keep_running.set(false);
    }
  }

  @Override
  protected void onLoad() {
    connected.set(false);

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
        .name("join")
        .description("Join a channel")
        .processor(
            data -> { // maybe there's a nicer way to do this?
              if (data.getArgumentCount() > 1)
                join_channel(data.getArgumentAsString(0), data.getArgumentAsString(1));
              else if (data.getArgumentCount() > 0)
                join_channel(data.getArgumentAsString(0));
              else
                join_channel(channel.get());
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
              if (!show_system.get()) Helper.printWarning("You need to enable \"system\" to see the output of this command");
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
              if (!show_system.get()) Helper.printWarning("You need to enable \"system\" to see the output of this command");
              if (data.getArgumentCount() > 0)
                sendRaw("NAMES " + data.getArgumentAsString(0));
              else
                sendRaw("NAMES " + channel.get());
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

    loaded = true;
    if (autoconnect.get()) connect();
  }

  @Override
  protected void onEnabled() {
    if (loaded && autoconnect.get())
      connect();
  }

  @Override
  protected void onDisabled() {
    if (connected.get()) {
      if (Helper.getCurrentScreen() instanceof GuiMainMenu)
        quit("Quit Minecraft");
      else
        quit("Disabled IRC mod");
      connected.set(false);
    }
  }

  private void connect() {
    if (connected.get() || (server_thread != null && server_thread.isAlive() && server_thread.keep_running.get())) {
      Helper.printWarning("Already connected or connecting");
      return;
    }
    if (!this.isEnabled()) {
      Helper.printWarning("Please enable IRC mod");
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
    sendRaw("JOIN " + channel + " " + password);
  }

  private void join_channel(String channel) {
    sendRaw("JOIN " + channel);
  }

  public void sendMessage(String message) {
    sendMessage(channel.get(), message);
  }

  public void sendMessage(String target, String message) {
    if (sendRaw("PRIVMSG " + target + " :" + message)) {
      if (target.startsWith("#") && target.equals(channel.get()))
        printFormattedIRC(used_nick, message);
      else printFormattedIRC(used_nick, target, message);
    }
  }

  private boolean sendRaw(String content) {
    if (!connected.get()) {
      Helper.printWarning("Not connected to IRC");
      return false;
    }
    if (!this.isEnabled()) {
      Helper.printWarning("Please enable IRC mod");
      return false;
    }
    try {
      writer.write(content + "\r\n");
      writer.flush();
      return true;
    } catch (IOException e) {
      Helper.printError("Could not send raw message");
      return false;
    }
  }

  private void parseIRCchat(String msgIn) {
    try {
      String author = msgIn.split("!", 2)[0].substring(1);
      String[] buf = msgIn.split("PRIVMSG", 2)[1].split(":", 2);
      String message = buf[1];
      String dest = buf[0].replace(" ", "");
      if (dest.equals(channel.get())) printFormattedIRC(author, message);
      else printFormattedIRC(author, dest, message);
    } catch (RuntimeException e) {
      e.printStackTrace();
      printIRCSystem(msgIn);
    }
  }

  private String parseIRCjoin(String msgIn) {
    try {
      return msgIn.split("!", 2)[0].substring(1) + " joined";
    } catch (Exception e) {
      return msgIn;
    }
  }

  private String parseIRCleave(String msgIn) {
    try {
      String buf = msgIn.split("!", 2)[0].substring(1) + " left";
      if (msgIn.contains("QUIT")) {
        buf += ": " + msgIn.split("Quit", 2)[1].substring(2);
      }
      return buf;
    } catch (Exception e) {
      return msgIn;
    }
  }

  public static void printIRCSystem(String text) {
    Helper.outputMessage(Helper.timestamp() +
        Helper.getFormattedText("[IRC] ", TextFormatting.DARK_PURPLE, true, false)
          .appendSibling(
            Helper.getFormattedText(text, TextFormatting.DARK_GRAY, false, true)
          ).getFormattedText()
    );
  }

  public static void printFormattedIRC(String author, String message) {
    printFormattedIRC(author, "", message);
  }

  public static void printFormattedIRC(String author, String target, String message) {
    Helper.outputMessage(Helper.timestamp() +
      Helper.getFormattedText("[IRC] ", TextFormatting.DARK_PURPLE, true, false)
        .appendSibling(
          Helper.getFormattedText(String.format("<%s%s%s>", author,
            (!target.equals("") ? " -> " : ""), target), TextFormatting.GRAY, false, false)
              .appendSibling(
                Helper.getFormattedText(" ", TextFormatting.WHITE, false, false)
                  .appendSibling(
                    ForgeHooks.newChatWithLinks(message)
                  )
              )
        ).getFormattedText()
    );
  }

  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    while (messages.size() > 0) {
      String buf = messages.poll();
      if (buf.startsWith(String.format(":%s 005", server.get()))) { // capabilities message
        if (show_system.get()) printIRCSystem(buf); // print it without stripping anything
      } else if (buf.contains("PRIVMSG")) {
        parseIRCchat(buf);
      } else if (buf.contains("JOIN")) {
        printIRCSystem(parseIRCjoin(buf));
      } else if (buf.contains("PART") || buf.contains("QUIT")) {
        printIRCSystem(parseIRCleave(buf));
      } else if (show_system.get() && buf.contains(used_nick)) { // don't print NOTICE messages
        printIRCSystem(buf.split(used_nick, 2)[1]);
      }
    }
  }

  @SubscribeEvent
  public void onPacketSent(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof CPacketChatMessage
        && !PacketHelper.isIgnored(event.getPacket())) {
      String inputMessage = ((CPacketChatMessage) event.getPacket()).getMessage();

	    if (irc_only.get() || inputMessage.startsWith(prefix.get())) {
        event.setCanceled(true);
        String msg = (irc_only.get() ? inputMessage : inputMessage.replaceFirst(prefix.get(), ""));
        sendMessage(channel.get(), msg);
      }
    }
  }
}
