package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getModManager;

import com.matt.forgehax.asm.events.RenderTabNameEvent;
import com.matt.forgehax.mods.managers.FriendManager;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.entity.PlayerUtils;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.color.ColorClamp;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.math.AlignHelper;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.HudMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextFormatting;

import java.util.*;
import java.util.stream.Collectors;

@RegisterMod
public class PlayerList extends HudMod {

  private final Setting<Boolean> distance =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("distance")
      .description("Show distance of player")
      .defaultTo(true)
      .build();
  private final Setting<Boolean> gamemode =
    getCommandStub()
       .builders()
       .<Boolean>newSettingBuilder()
       .name("gamemode")
       .description("Show gamemode of player")
       .defaultTo(true)
       .build();
  private final Setting<Boolean> health =
    getCommandStub()
       .builders()
       .<Boolean>newSettingBuilder()
       .name("health")
       .description("Show health of player")
       .defaultTo(true)
       .build();
  private final Setting<Boolean> ping =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("ping")
      .description("Show ping of player")
      .defaultTo(true)
      .build();
  private final Setting<Boolean> above_below =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("relative")
      .description("Show relative position (above or below)")
      .defaultTo(true)
      .build();

  private final Setting<Boolean> color =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("color")
        .description("Color player names depending on gear")
        .defaultTo(false)
        .build();

  private final Setting<Float> maxdist =
    getCommandStub()
        .builders()
        .<Float>newSettingBuilder()
        .name("maxdist")
        .description("Don't show players further than this, set 0 to disable")
        .min(0F)
        .max(1000F)
        .defaultTo(0F)
        .build();

  private final Setting<ListSorter> sortMode =
    getCommandStub()
        .builders()
        .<ListSorter>newSettingEnumBuilder()
        .name("sorting")
        .description("Sorting mode")
        .defaultTo(ListSorter.LENGTH)
        .build();

  public final Setting<TextFormatting> tab_mark =
    getCommandStub()
        .builders()
        .<TextFormatting>newSettingEnumBuilder()
        .name("tab-mark")
        .description("Apply extra formatting to players in render distance, set to RESET to disable")
        .defaultTo(TextFormatting.UNDERLINE)
        .build();

  public PlayerList() {
    super(Category.GUI, "PlayerList", false, "Displays nearby players and some stats");
  }

  protected enum ListSorter {
    ALPHABETICAL(Comparator.comparing(e -> ((EntityPlayer) e).getName())), // mod list is already sorted alphabetically
    LENGTH(Comparator.comparing(e -> ((EntityPlayer) e).getName().length(), Comparator.reverseOrder())),
    REVLENGTH(Comparator.comparingInt(e -> ((EntityPlayer) e).getName().length())),
    DISTANCE(Comparator.comparing(e -> getLocalPlayer().getDistance((EntityPlayer) e), Comparator.reverseOrder())),
    REVDISTANCE(Comparator.comparing(e -> getLocalPlayer().getDistance((EntityPlayer) e)));

    private final Comparator<EntityPlayer> comparator;

    public Comparator<EntityPlayer> getComparator() {
      return this.comparator;
    }

    ListSorter(Comparator<EntityPlayer> comparatorIn) {
      this.comparator = comparatorIn;
    }
  }

  @Override
  protected AlignHelper.Align getDefaultAlignment() {
    return AlignHelper.Align.TOPLEFT;
  }

  @Override
  protected int getDefaultOffsetX() { return 100; }

  @Override
  protected int getDefaultOffsetY() { return 1; }

  @Override
  protected double getDefaultScale() { return 0.5d; }

  @Override
  public boolean isInfoDisplayElement() { return false; }

  private int count = 0;
  List<String> text = new ArrayList<>();
  List<EntityPlayer> players = new ArrayList<>();

  @Override
  public String getDisplayText() {
    return (getModName() + " [" + TextFormatting.DARK_AQUA + count + TextFormatting.RESET + "]");
  }

  @SubscribeEvent
  public void onTabUpdate(RenderTabNameEvent event) {
    if (tab_mark.get().equals(TextFormatting.RESET)) return;
    if (null != players.stream()
          .map(player -> player.getName())
          .filter(name -> name.equals(event.getName()))
          .findAny()
          .orElse(null))
      event.setName(tab_mark.get() + event.getName());
  }

  @SubscribeEvent
  public void onRenderScreen(RenderGameOverlayEvent.Text event) {
    if (!MC.gameSettings.showDebugInfo) {
      int align = alignment.get().ordinal();
	    text.clear();

      players = getWorld().loadedEntityList.stream()
        .filter(EntityUtils::isPlayer)
        .filter(e -> maxdist.get() == 0 || e.getDistance(getLocalPlayer()) <= maxdist.get())
        .filter(e -> !Objects.equals(getLocalPlayer(), e))
        .map(entity -> (EntityPlayer) entity)
        .sorted(sortMode.get().getComparator())
        .collect(Collectors.toList());
        
      players.stream()
        .map(entity -> makePlayerString(entity))
        .forEach(line -> text.add(line));

      count = text.size();

      // Prints on screen
      SurfaceHelper.drawTextAlign(text, getPosX(0), getPosY(0),
        Colors.WHITE.toBuffer(), scale.get(), true, align);
    }
  }

  private String makePlayerString(EntityPlayer in) {
    StringBuilder out = new StringBuilder();
    if (distance.get()) out.append(PlayerUtils.getDistanceColor(in)).append(" ");
    if (gamemode.get()) out.append(PlayerUtils.getGmode(in)).append(" ");
    out.append(getNameColor(in));
    if (ping.get()) out.append(" ").append(PlayerUtils.getColorPing(in));
    if (health.get()) out.append(" ").append(PlayerUtils.getHPColor(in));
    if (above_below.get()) out.append(" ").append(PlayerUtils.above_below(in));
    return out.toString();
  }

  private String getNameColor(EntityPlayer entity) {
    if (FriendManager.isFriend(entity.getName())) {
      TextFormatting clr = ColorClamp.getClampedColor(FriendManager.getFriendColor(entity.getName()));
      return clr + entity.getName() + TextFormatting.GRAY;
    }
    if (color.get()) {
      return PlayerUtils.getGearColor(entity).getFormattedText() + TextFormatting.GRAY;
    }
    return TextFormatting.GRAY + entity.getName();
  }
}
