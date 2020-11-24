package com.matt.forgehax.mods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.math.AlignHelper;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ListMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;

@RegisterMod
public class EntityList extends ListMod {

  public EntityList() {
    super(Category.GUI, "EntityList", false, "Displays a list of all rendered entities");
  }

  private final Setting<Boolean> items =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("items")
      .description("Include non-living entities")
      .defaultTo(true)
      .build();

  private final Setting<Boolean> animate =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("animate")
      .description("Add entities to screen one at a time")
      .defaultTo(true)
      .build();

  private final Setting<Boolean> players =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("players")
      .description("Include players")
      .defaultTo(false)
      .build();

  private final Setting<String> search_key =
    getCommandStub()
      .builders()
      .<String>newSettingBuilder()
      .name("search")
      .description("Entities named like this will be highlighted")
      .defaultTo("Slime")
      .build();

  private final Setting<TextFormatting> color =
    getCommandStub()
      .builders()
      .<TextFormatting>newSettingEnumBuilder()
      .name("highlight-color")
      .description("Color to use for the highlighted names")
      .defaultTo(TextFormatting.GREEN)
      .build();

  private final Setting<Boolean> show_class =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("show-type")
      .description("Don't display entity name but instead entity type")
      .defaultTo(false)
      .build();

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

  @Override
  public String getDisplayText() {
    return (getModName() + " [" + TextFormatting.DARK_RED + count + TextFormatting.RESET + "]");
  }

  private int count = 0, max_len = 0;
  private List<Entity> entityList = new LinkedList<>();
  private List<String> text_multiples = new ArrayList<>();
  private List<String> text = new ArrayList<>();

  @SubscribeEvent
  public void onRenderScreen(RenderGameOverlayEvent.Text event) {
    if (!MC.gameSettings.showDebugInfo) {
      int align = alignment.get().ordinal();

      entityList.clear();
      text_multiples.clear();
      text.clear();

      getWorld()
        .loadedEntityList
        .stream()
        .filter(e -> items.get() || EntityUtils.isLiving(e))
        .filter(e -> items.get() || EntityUtils.isAlive(e))
        .filter(e -> players.get() || !EntityUtils.isPlayer(e))
        .filter(
          entity ->
            !Objects.equals(getLocalPlayer(), entity) && !EntityUtils.isFakeLocalPlayer(entity))
        .filter(EntityUtils::isValidEntity)
        .forEach(entity -> entityList.add(entity));

      if (show_class.get()) {
        entityList.stream()
          .map(entity -> entity.getClass().getSimpleName())
          .forEach(type -> text_multiples.add(type));

      } else {
        entityList.stream()
          .map(entity -> { if (entity instanceof EntityItem)
                              return ((EntityItem) entity).getItem().getDisplayName();
                           else if (entity instanceof EntityEnderCrystal) // ye janky but whaterver
                              return "Ender Crystal";                     // it doesn't seem to have a name anywhere
                           else
                              return entity.getDisplayName().getUnformattedText();
                         })
          .forEach(name -> text_multiples.add(name));
      }

	    String buf = "";
      int num = 0;
      count = entityList.size();
	    for (String element : text_multiples.stream().distinct().collect(Collectors.toList())) {
        buf = String.format("%s", element);
        if (buf.contains(search_key.get())) {
          buf = buf.replace(search_key.get(), color.get() + search_key.get() + TextFormatting.RESET);
        }
		    num = Collections.frequency(text_multiples, element);
        if (num > 1) buf += String.format(" (x%d)", num);
        if (show_class.get()) buf = buf.replace("Entity", "");
		    text.add(appendArrow(buf));
        if (animate.get() && text.size() >= (max_len + 1)) break;
	    }
      max_len = text.size();

      text.sort(sortMode.get().getComparator());

      // Prints on screen
      SurfaceHelper.drawTextAlign(text, getPosX(0), getPosY(0),
        Colors.WHITE.toBuffer(), scale.get(), true, align);
    }
  }
}
