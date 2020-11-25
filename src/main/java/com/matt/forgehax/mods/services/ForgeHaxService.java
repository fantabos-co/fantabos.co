package com.matt.forgehax.mods.services;

import com.matt.forgehax.Helper;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterMod
public class ForgeHaxService extends ServiceMod {

  public ForgeHaxService() {
    super("FHSettings", "Global values");
    ForgeHaxService.INSTANCE = this;
  }

  private static ForgeHaxService INSTANCE;
  public static ForgeHaxService getInstance() { return ForgeHaxService.INSTANCE; }

  private long last_save = 0;

  public final Setting<Boolean> toggleMsgs =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("toggle-msgs")
          .description("Log to chat when toggling any mod")
          .defaultTo(false)
          .build();

  public final Setting<Boolean> ascii =
       getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("ascii-art")
          .description("Display cool ASCII art in Terminal (kinda breaks with resource packs).")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> autosave =
     getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("autosave")
        .description("Automatically save config on switch, unload and every <interval> minutes")
        .defaultTo(true)
        .build();

  public final Setting<Boolean> per_server =
     getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("per-server")
        .description("Automatically save and load configurations names like the server you're joining")
        .defaultTo(false)
        .build();

  public final Setting<Integer> autosave_interval =
     getCommandStub()
        .builders()
        .<Integer>newSettingBuilder()
        .name("interval")
        .description("Also save config with this interval (in minutes)")
        .defaultTo(15)
        .build();

  @Override
  protected void onLoad() {
    last_save = System.currentTimeMillis();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("save")
        .description("Save all configurations. If no name is provided, load default")
        .processor(data -> {
          if (data.getArgumentCount() > 0) {
            Helper.getGlobalCommand().saveConfiguration(data.getArgumentAsString(0));
          } else {
            Helper.getGlobalCommand().saveConfiguration();
          }
        })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("load")
        .description("Load configuration from file, overwriting current settings. If no name is provided, load default")
        .processor(data -> {
          if (data.getArgumentCount() > 0) {
            Helper.getGlobalCommand().loadConfiguration(data.getArgumentAsString(0));
          } else {
            Helper.getGlobalCommand().loadConfiguration();
          }
        })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("where")
        .description("where is the config file?")
        .processor(data -> Helper.printLog(Helper.getFormattedText(".minecraft/forgehax/config/config.json",
                                          TextFormatting.DARK_GRAY, false, false, configClick(), configHover())))
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("which")
        .description("which config is active?")
        .processor(data -> Helper.printLog(String.format("Using loadout \"%s\"", Helper.getGlobalCommand().getLoadout())))
        .build();

    if (ascii.get()) {
      // Adjusted by OverFloyd
      printFormattedLine(
           "\n\n"+
           "   / "+TextFormatting.OBFUSCATED+"$$$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"                                               /"+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"      /"+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"\n" +
           "   | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY + "_____/                                                | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"      | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"\n" +
           "   | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"     /"+TextFormatting.OBFUSCATED+"$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"    /"+TextFormatting.OBFUSCATED+"$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"  /"+TextFormatting.OBFUSCATED+"$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"  /"+TextFormatting.OBFUSCATED+"$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"  | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"      | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"   /"+TextFormatting.OBFUSCATED+"$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"  /"+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"   /"+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"\n" +
           "   | "+TextFormatting.OBFUSCATED+"$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" /"+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY + "__  "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"  /"+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY + "__ "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" /"+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY + "_   "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" /"+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY + "__ "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" | "+TextFormatting.OBFUSCATED+"$$$$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"   |____  "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" |  "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" /"+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"/\n" +
           "   | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY + "_/ | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"   \\ "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" \\__/ | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"  \\ "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" | "+TextFormatting.OBFUSCATED+"$$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY + "____  "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"  /"+TextFormatting.OBFUSCATED+"$$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" \\  "+TextFormatting.OBFUSCATED+"$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"/\n" +
           "   | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"    | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"    | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"        | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"   | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY + "____/ | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"      | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" /"+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY + "__   "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"  > "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"\n" +
           "   | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"    |  "+TextFormatting.OBFUSCATED+"$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"/ | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"        | "+TextFormatting.OBFUSCATED+"$$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" | "+TextFormatting.OBFUSCATED+"$$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"      | "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" |  "+TextFormatting.OBFUSCATED+"$$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" / "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"/\\ "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"\n" +
           "   |__/    \\_____/   |__/        \\___   "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+" \\______/|__/      |__/ \\_______/|__/   \\_/\n" +
           "                                      /"+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"  \\ "+TextFormatting.OBFUSCATED+"$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"\n" +
           "                                      | "+TextFormatting.OBFUSCATED+"$$$$$$"+TextFormatting.RESET + TextFormatting.DARK_GRAY+"/\n" +
           "                                      \\_____/\n"
                      );
      if (MC.getSession().getProfile().getName().equals("TacticalFaceplant")) {
        printFormattedLine("Your coords are totally safe :)");
        printFormattedLine(" ");
      }
    }
  }

  private static HoverEvent defaultHover() {
    return new HoverEvent(HoverEvent.Action.SHOW_TEXT,
          Helper.getFormattedText("ForgeHax", TextFormatting.GRAY, true, false)
                          .appendSibling(Helper.getFormattedText(" v69.420\n", TextFormatting.DARK_GRAY, false, false))
                            .appendSibling(Helper.getFormattedText("fantabos.co on toppe\n\n", TextFormatting.GOLD, false, false))
                                .appendSibling(Helper.getFormattedText("ASCII art from patorjk.com\n" +
                                                                     "tediously fixed by OverFloyd <3", TextFormatting.DARK_GRAY, false, true))
    );
  }

  private static HoverEvent configHover() {
    return new HoverEvent(HoverEvent.Action.SHOW_TEXT,
          Helper.getFormattedText("Click me to open config file", TextFormatting.GRAY, true, false)
    );
  }

  private static ClickEvent defaultClick() {
    return new ClickEvent(ClickEvent.Action.OPEN_URL, "http://www.patorjk.com/software/taag/#p=display&f=Big%20Money-ne&t=ForgeHax");
  }

  private static ClickEvent configClick() {
    return new ClickEvent(ClickEvent.Action.OPEN_FILE, "forgehax/config/config.json");
  }

  private void printFormattedLine(String in) {
    Helper.outputMessage(Helper.getFormattedText(in, TextFormatting.DARK_GRAY, false, false,
                                                    defaultClick(), defaultHover()));
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if (!autosave.get()) return;
    if (System.currentTimeMillis() - last_save > (autosave_interval.get() * 1000L * 60L)) {
      last_save = System.currentTimeMillis();
      Helper.getGlobalCommand().saveConfiguration();
    }
  }

  @SubscribeEvent
  public void onWorldLoad(WorldEvent.Load event) {
    if (!per_server.get() || MC.getCurrentServerData() == null) return;
    String name = (MC.getCurrentServerData().serverIP != null ? MC.getCurrentServerData().serverIP.replace(":", "-") : "singleplayer");
    if (autosave.get()) Helper.getGlobalCommand().saveConfiguration(); // save previous
    Helper.getGlobalCommand().loadConfiguration(name);
  }
}
