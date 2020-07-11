package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.util.math.VectorUtils.distance;
import static com.matt.forgehax.Helper.getModManager;

import com.matt.forgehax.mods.services.FriendService;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.entity.PlayerUtils;
import com.matt.forgehax.util.color.Colors;
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

@RegisterMod
public class PlayerList extends HudMod {

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
        .description("Don't show players further than this")
        .min(0F)
        .defaultTo(500F)
        .build();

  private final Setting<ListSorter> sortMode =
    getCommandStub()
        .builders()
        .<ListSorter>newSettingEnumBuilder()
        .name("sorting")
        .description("Sorting mode")
        .defaultTo(ListSorter.LENGTH)
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

  @Override
  public String getDisplayText() {
    return (getModName() + " [" + TextFormatting.DARK_AQUA + count + TextFormatting.WHITE + "]");
  }

  @SubscribeEvent
  public void onRenderScreen(RenderGameOverlayEvent.Text event) {
    if (!MC.gameSettings.showDebugInfo) {
      int align = alignment.get().ordinal();
	    List<String> text = new ArrayList<>();

      getWorld()
        .loadedEntityList
        .stream()
        .filter(EntityUtils::isPlayer)
        .filter(e -> e.getDistance(getLocalPlayer()) < maxdist.get())
        .filter(
          entity ->
            !Objects.equals(getLocalPlayer(), entity) && !EntityUtils.isFakeLocalPlayer(entity))
        .map(entity -> (EntityPlayer) entity)
        .sorted(sortMode.get().getComparator())
        .map(entity -> (getDistanceColor(getLocalPlayer().getDistance(entity)) +
                        getNameColor(entity) + " [" +
                        getHPColor(entity.getHealth() + entity.getAbsorptionAmount()) + TextFormatting.GRAY + "] " +
                        above_below(getLocalPlayer().posY, entity.posY)) + TextFormatting.RESET)
        .forEach(line -> text.add(line));

      count = text.size();

      // Prints on screen
      SurfaceHelper.drawTextAlign(text, getPosX(0), getPosY(0),
        Colors.WHITE.toBuffer(), scale.get(), true, align);
    }
  }

  private static String getHPColor(float hp) {
    if (hp > 20F) return TextFormatting.YELLOW + String.format("%.0f", hp);
    if (hp > 17F) return TextFormatting.DARK_GREEN + String.format("%.0f", hp);
    if (hp > 12F) return TextFormatting.GREEN + String.format("%.0f", hp);
    if (hp > 8F) return TextFormatting.GOLD + String.format("%.0f", hp);
    if (hp > 5F) return TextFormatting.RED + String.format("%.1f", hp);
    if (hp > 2F) return TextFormatting.DARK_RED + String.format("%.1f", hp);
    return TextFormatting.DARK_GRAY + String.format("%.1f", hp);
  }

  private static String above_below(double pos1, double pos2) {
    if (pos1 > pos2) return TextFormatting.GOLD + "++ ";
    if (pos1 < pos2) return TextFormatting.DARK_GRAY + "-- ";
    return TextFormatting.GRAY + "== ";
  }

  private static String getDistanceColor(double distance) {
    if (distance > 30D) return TextFormatting.DARK_AQUA + String.format("%.1fm ", distance);
    if (distance > 10D) return TextFormatting.AQUA + String.format("%.1fm ", distance);
    return TextFormatting.WHITE + String.format("%.1fm ", distance);
  }

  private String getNameColor(EntityPlayer entity) {
    if (getModManager().get(FriendService.class).get().isFriend(entity.getName()))
      return TextFormatting.LIGHT_PURPLE + entity.getName() + TextFormatting.GRAY;
    if (color.get()) {
      return PlayerUtils.getGearColor(entity).getFormattedText() + TextFormatting.GRAY;
    }
    return TextFormatting.GRAY + entity.getName();
  }
}
