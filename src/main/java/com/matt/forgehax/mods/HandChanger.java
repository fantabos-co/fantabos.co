package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Merged on 10/10/2020 by tonio
 *  from IronException's AntiHeldItemAnimation and my LowerHeld
 *  added the "viewmodel changing" too
 */
@RegisterMod
public class HandChanger extends ToggleMod {
  
  public HandChanger() {
    super(Category.PLAYER, "HandChanger", false, "Changes how your hands are displayed");
  }
  
  public final Setting<Boolean> instant =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("instant")
      .description("Make held items switch instantly")
      .defaultTo(false)
      .build();
    
  public final Setting<Boolean> hidden =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("hide")
      .description("Completely hide your hands")
      .defaultTo(false)
      .build();

  public final Setting<Boolean> translate =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("translate")
      .description("Apply translations to hands")
      .defaultTo(false)
      .build();

  public final Setting<Float> offhand_cap =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("off-cap")
      .description("Limit offhand equipping progress")
      .min(0.01F)
      .max(1.0F)
      .defaultTo(1.0F)
      .build();

  public final Setting<Float> mainhand_cap =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("main-cap")
      .description("Limit main hand equipping progress")
      .min(0.01F)
      .max(1.0F)
      .defaultTo(1.0F)
      .build();

  private final Setting<Double> mainX =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("main-x")
      .description("Add a X offset to hands drawing")
      .defaultTo(0.)
      .min(-3.0)
      .max(3.0)
      .build();
  private final Setting<Double> mainY =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("main-y")
      .description("Add a Y offset to hands drawing")
      .defaultTo(0.)
      .min(-3.0)
      .max(3.0)
      .build();	
  private final Setting<Double> mainZ =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("main-z")
      .description("Add a Z offset to hands drawing")
      .defaultTo(-0.75)
      .min(-3.0)
      .max(3.0)
      .build();
  private final Setting<Double> offX =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("off-X")
      .description("off hand x")
      .defaultTo(-0.3)
      .min(-3.0)
      .max(3.0)
      .build();	   
  private final Setting<Double> offY =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("off-Y")
      .description("off hand y")
      .defaultTo(-0.3)
      .min(-3.0)
      .max(3.0)
      .build();	 
  private final Setting<Double> offZ =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("off-Z")
      .description("off hand z")
      .defaultTo(-0.1)
      .min(-3.0)
      .max(3.0)
      .build();

  @SubscribeEvent
  public void onDrawSpecificHand(RenderSpecificHandEvent event) {
    if (translate.get()) {
      if (event.getHand() == EnumHand.MAIN_HAND) {
        GlStateManager.translate(mainX.get(), mainY.get(), mainZ.get());
      } else if (event.getHand() == EnumHand.OFF_HAND) {
        GlStateManager.translate(-mainX.get(), -mainY.get(), -mainZ.get()); // need to revert translation made for mainhand
        GlStateManager.translate(offX.get(), offY.get(), offZ.get());
      }
    }
  }

  @SubscribeEvent
  public void onDrawHand(RenderHandEvent event) {
    if (getLocalPlayer() == null || event.isCanceled()) return;

    if (hidden.get()) {
      event.setCanceled(true);
      return;
    }

    // Cap max equipped amount
    if (FastReflection.Fields.ItemRenderer_equippedProgressOffHand.get(MC.entityRenderer.itemRenderer) > offhand_cap.get())
      FastReflection.Fields.ItemRenderer_equippedProgressOffHand.set(MC.entityRenderer.itemRenderer, offhand_cap.get());
    if (FastReflection.Fields.ItemRenderer_equippedProgressMainHand.get(MC.entityRenderer.itemRenderer) > mainhand_cap.get())
      FastReflection.Fields.ItemRenderer_equippedProgressMainHand.set(MC.entityRenderer.itemRenderer, mainhand_cap.get());
    
    // Make held item change instantly
    if (instant.get()) {
      FastReflection.Fields.ItemRenderer_itemStackMainHand.set(MC.getItemRenderer(), getLocalPlayer().getHeldItemMainhand());
      FastReflection.Fields.ItemRenderer_itemStackOffHand.set(MC.getItemRenderer(), getLocalPlayer().getHeldItemOffhand());

      if (FastReflection.Fields.ItemRenderer_equippedProgressOffHand.get(MC.entityRenderer.itemRenderer) < offhand_cap.get()) {
        FastReflection.Fields.ItemRenderer_equippedProgressOffHand.set(MC.entityRenderer.itemRenderer, offhand_cap.get());
        FastReflection.Fields.ItemRenderer_prevEquippedProgressOffHand.set(MC.getItemRenderer(), offhand_cap.get());
      }

      if (FastReflection.Fields.ItemRenderer_equippedProgressMainHand.get(MC.entityRenderer.itemRenderer) < mainhand_cap.get()) {
        FastReflection.Fields.ItemRenderer_equippedProgressMainHand.set(MC.entityRenderer.itemRenderer, mainhand_cap.get());
        FastReflection.Fields.ItemRenderer_prevEquippedProgressMainHand.set(MC.getItemRenderer(), mainhand_cap.get());
      }
    }
  }
}
