package com.matt.forgehax.mods;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.math.AlignHelper.Align;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.HudMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.text.TextFormatting;

import java.util.Random;

import static com.matt.forgehax.Helper.getPlayerDirection;

@RegisterMod
public class CoordsHud extends HudMod {

  public CoordsHud() {
    super(Category.GUI, "Coords", false, "Displays your current coordinates");
  }

  private Random rand = new Random();

  private final Setting<Boolean> translate =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("translate")
          .description("Show corresponding Nether or Overworld coords")
          .defaultTo(true)
          .build();

  private final Setting<Boolean> multiline =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("multiline")
          .description("Show translated coords above")
          .defaultTo(true)
          .build();

  private final Setting<Boolean> direction =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("direction")
          .description("Show the facing value")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> viewEntity =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("view-entity")
          .description("Show the current coords for viewentity (freecam)")
          .defaultTo(false)
          .build();

  public final Setting<Boolean> spoof =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("spoof")
          .description("Hide true coordinates")
          .defaultTo(false)
          .build();

  @Override
  protected Align getDefaultAlignment() {
    return Align.BOTTOMLEFT;
  }

  @Override
  protected int getDefaultOffsetX() {
    return 1;
  }

  @Override
  protected int getDefaultOffsetY() {
    return 1;
  }

  @Override
  protected double getDefaultScale() {
    return 1d;
  }

  @Override
  protected void onLoad() {
    symbols = new DecimalFormatSymbols(Locale.US);
    symbols.setDecimalSeparator(',');
    symbols.setGroupingSeparator('.');
    formatter = new DecimalFormat("##,###,##0", symbols);
    formatter.setGroupingSize(3);
  }

  double thisX;
  double thisY;
  double thisZ;
  double otherX;
  double otherZ;

  private DecimalFormatSymbols symbols;
  private DecimalFormat formatter;

  @SubscribeEvent
  public void onLocalPlayerUpdate(LocalPlayerUpdateEvent ev) {
    if (MC.world == null) {
      return;
    }

    if (spoof.get()) {
      thisX = rand.nextInt(999999);
      thisY = rand.nextInt(99);
      thisZ = rand.nextInt(999999);
      otherX = rand.nextInt(999999);
      otherZ = rand.nextInt(999999);
    } else {
      Entity entity = getEntity();
      thisX = entity.posX;
      thisY = entity.posY;
      thisZ = entity.posZ;

      double thisFactor = MC.world.provider.getMovementFactor();
      double otherFactor = thisFactor != 1d ? 1d : 8d;
      double travelFactor = thisFactor / otherFactor;
      otherX = thisX * travelFactor;
      otherZ = thisZ * travelFactor;
    }
  }

  @SubscribeEvent
  public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
    List<String> text = new ArrayList<>();

    // Direction
    String facingNormal = String.format("%s " + TextFormatting.GRAY + "[%s]" + TextFormatting.WHITE,
                                  facingTable[getPlayerDirection()], towardsTable[getPlayerDirection()]);
    // Multiline coords + direction
    String facingWithTCoords = String.format(
      TextFormatting.GRAY + "[ " + TextFormatting.WHITE + "%s " + TextFormatting.GRAY + "\u23d0 " +
      TextFormatting.WHITE + "%s " + TextFormatting.GRAY + "] - " + TextFormatting.WHITE + "%s " +
      TextFormatting.GRAY + "[%s]" + TextFormatting.WHITE, formatter.format(otherX), formatter.format(otherZ),
          facingTable[getPlayerDirection()], towardsTable[getPlayerDirection()]);
    // Only OW coords
    String coordsNormal = String.format(
      TextFormatting.GRAY + "[ " + TextFormatting.DARK_GRAY + "X " + TextFormatting.WHITE +
      "%s " + TextFormatting.GRAY + "\u23d0 " + TextFormatting.WHITE + "%s " +
      TextFormatting.DARK_GRAY + "Z " + TextFormatting.GRAY + "] (" + TextFormatting.WHITE + "%.1f " +
      TextFormatting.DARK_GRAY + "Y" + TextFormatting.GRAY + ")" + TextFormatting.WHITE, 
      formatter.format(thisX), formatter.format(thisZ), thisY);
    // Multiline Nether coords
    String coordsMultiTranslated = String.format(
      TextFormatting.GRAY + "[ " + TextFormatting.WHITE + "%s " + TextFormatting.GRAY + "\u23d0 " +
      TextFormatting.WHITE + "%s " + TextFormatting.GRAY + "] - " + TextFormatting.WHITE, formatter.format(otherX), formatter.format(otherZ));
    // Single line OW + Nether coords
    String coordsTranslated = String.format(
      "%s  %s " + TextFormatting.GRAY + "\u23d0 " + TextFormatting.WHITE + "%s", coordsNormal, formatter.format(otherX), formatter.format(otherZ));

    if (!translate.get() || MC.player.dimension == 1) {
      text.add(coordsNormal);
      if (direction.get()) {
        text.add(facingNormal);
      }
    } else if (MC.player.dimension == -1) {
      if (multiline.get()) {
        if (direction.get()) {
          text.add(facingWithTCoords);
        } else {
          text.add(coordsTranslated);
        }
        text.add(coordsNormal);
      } else {
        if (direction.get()) {
          text.add(facingNormal);
        }
        text.add(coordsTranslated);
      }
    } else {
      if (multiline.get()) {
        text.add(coordsNormal);
        if (direction.get()) {
          text.add(facingWithTCoords);
        } else {
          text.add(coordsTranslated);
        }
      } else {
        text.add(coordsTranslated);
        if (direction.get()) {
          text.add(facingNormal);
        }
      }
    }

    // Printing
    SurfaceHelper.drawTextAlign(text, getPosX(0), getPosY(0),
        Colors.WHITE.toBuffer(), scale.get(), true, alignment.get().ordinal());
  }

  private final String[] facingTable = {
      "South",
      "South West",
      "West",
      "North West",
      "North",
      "North East",
      "East",
      "South East"
  };

  private final String[] towardsTable = {
      "+Z",
      "-X +Z",
      "-X",
      "-X -Z",
      "-Z",
      "+X -Z",
      "+X",
      "+X +Z"
  };

  public Entity getEntity() {
    return viewEntity.get() ? MC.getRenderViewEntity() : MC.player;
  }
}
