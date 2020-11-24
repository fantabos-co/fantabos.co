package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.asm.reflection.FastReflection.Fields.GuiDisconnected_message;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.TrayIcon.MessageType;
import java.util.Optional;
import java.awt.AWTException;

import org.lwjgl.opengl.Display;

import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.network.play.server.SPacketUpdateHealth;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class DesktopNotify extends ToggleMod {

  public final Setting<Boolean> player =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("player")
        .description("Warn when spotting players")
        .defaultTo(true)
        .build();
  private final Setting<Boolean> damage =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("damage")
        .description("Warn on damage received")
        .defaultTo(true)
        .build();
  public final Setting<Boolean> mention =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("mention")
        .description("Warn when mentioned in chat")
        .defaultTo(true)
        .build();
  public final Setting<Boolean> whisper =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("whisper")
        .description("Warn when a player whispers you")
        .defaultTo(true)
        .build();
  private final Setting<Boolean> system_messages =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("system")
        .description("Warn when a system message is printed")
        .defaultTo(true)
        .build();
  public final Setting<Boolean> on_disconnect =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("disconnect")
        .description("Warn when disconnected from server")
        .defaultTo(true)
        .build();
  public final Setting<Boolean> onlyInactive =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("on-lost-focus")
        .description("Only send notifications if game is not in foreground")
        .defaultTo(true)
        .build();

  private TrayIcon icon;
  SystemTray tray;
  private boolean supported = true;
  private static DesktopNotify INSTANCE;

  public DesktopNotify() {
    super(Category.MISC, "DesktopNotify", true, "Send Desktop notifications on events");
    DesktopNotify.INSTANCE = this;
  }

  @Override
  protected void onLoad() {
    if (!SystemTray.isSupported()) {
      Helper.printError("System tray is not supported");
      supported = false;
      return;
    }
    tray = SystemTray.getSystemTray();
    Image image = Toolkit.getDefaultToolkit().createImage(
                getClass().getClassLoader().getResource("icon.png"));
    icon = new TrayIcon(image, "ForgeHax");
    icon.setImageAutoSize(true);
    //Set tooltip text for the tray icon
    icon.setToolTip("It's just ForgeHax");
    try {
      tray.add(icon); 
    } catch (AWTException e) {
      e.printStackTrace();
      LOGGER.warn("Failed to add icon to system tray : ", e.getMessage());
      supported = false;
      return;
    }

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("test")
        .description("Fire a test notification")
        .processor(data -> {
          String title = "Test notification";
          String message = "This is the default text";
          if (data.getArgumentCount() > 2) {
            message = data.getArgumentAsString(1);
            title = data.getArgumentAsString(0);
          } else if (data.getArgumentCount() > 1) {
            message = data.getArgumentAsString(0);
          }

          try {
            icon.displayMessage(title, message, MessageType.INFO);
          } catch (Exception e) {
            e.printStackTrace();
            // ignore
          }
        })
        .build();
  }

  public static void notify(String title, String message) {
    DesktopNotify.INSTANCE.displayNotification(title, message);
  }

  private void displayNotification(String title, String message) {
    displayNotification(title, message, MessageType.INFO);
  }

  private void displayNotification(String title, String message, MessageType type) {
    if (!this.isEnabled() || !supported
        || (onlyInactive.get() && Display.isActive()))
          return;
    
    try {
      icon.displayMessage(title, message, type);
    } catch (Exception e) {
      e.printStackTrace();
      // ignore
    }
  }

  private static String getDimensionName(int dim) { // There probably is an enum but I'm lazy
    switch(dim) {
      case 0: return "Overworld";
      case 1: return "The End";
      case -1: return "Nether";
      default: return "Unknown";
    }
  }

  @SubscribeEvent
  public void onPacketIncoming(PacketEvent.Incoming.Pre event) {
    if (getLocalPlayer() != null && damage.get() &
        event.getPacket() instanceof SPacketUpdateHealth) {
      SPacketUpdateHealth packet = event.getPacket();
      if (packet.getHealth() < getLocalPlayer().getHealth())
        displayNotification("Took damage", String.format("Received %.1f damage",
                                    getLocalPlayer().getHealth() - packet.getHealth()));
    }
  }

  @SubscribeEvent
  public void onEntityJoinWorld(EntityJoinWorldEvent event) {
    if (getLocalPlayer() == null) return;
    if (!player.get()) return;
    if (EntityUtils.isPlayer(event.getEntity()) &&
        !getLocalPlayer().equals(event.getEntity()) &&
        !EntityUtils.isFakeLocalPlayer(event.getEntity())) {
      displayNotification("Spotted player", String.format("Spotted %s", event.getEntity().getName()));
    } else if (EntityUtils.isPlayer(event.getEntity()) &&
               getLocalPlayer().equals(event.getEntity())) {
      if (getLocalPlayer().isSpectator()) {
        displayNotification("Spectating", String.format("Player joined as spectator (%s)",
                            getDimensionName(getLocalPlayer().world.provider.getDimension())));
      } else {
        displayNotification("Join World", String.format("Player joined world (%s)",
                            getDimensionName(getLocalPlayer().world.provider.getDimension())));
      }
    }
  }

  @SubscribeEvent
  public void onChat(ClientChatReceivedEvent event) {
    final String message = event.getMessage().getUnformattedText();
    if (whisper.get() && message.contains("whispers: ")) {
      displayNotification("Whisper", message);
    } else if (mention.get() && message.contains(MC.getSession().getProfile().getName())) { 
      displayNotification("Chat mention", message);
    } 
  }

  @SubscribeEvent
  public void onGuiOpened(GuiOpenEvent event) {
    if (event.getGui() instanceof GuiDisconnected) {
      if (on_disconnect.get()) {
        String reason = Optional.ofNullable(GuiDisconnected_message.get(event.getGui()))
            .map(ITextComponent::getUnformattedText)
            .orElse("");
        if (reason.isEmpty()) {
          displayNotification("Disconnected", "Reason Unknown");
        } else {
          displayNotification("Disconnected", String.format("Reason: %s", reason));
        }
      }
    }
  }
}