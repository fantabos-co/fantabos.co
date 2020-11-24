package com.matt.forgehax.mods;

import java.awt.Color;
import java.util.LinkedList;

import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.math.AlignHelper.Align;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.HudMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/*
 * By TheAlphaEpsilon
 * 16 August 2020
 */
@RegisterMod
public class PlayerData extends HudMod {

	private final Setting<Boolean> renderArmor =
			getCommandStub()
				.builders()
				.<Boolean>newSettingBuilder()
				.description("Render your armor info on screen")
				.name("renderArmor")
				.defaultTo(true)
				.build();
	
	private final Setting<Boolean> renderHealth =
			getCommandStub()
				.builders()
				.<Boolean>newSettingBuilder()
				.description("Render your health info on screen")
				.name("renderHealth")
				.defaultTo(false)
				.build();
	
	private final Setting<Boolean> renderFood =
			getCommandStub()
				.builders()
				.<Boolean>newSettingBuilder()
				.description("Render your food info on screen")
				.name("renderFood")
				.defaultTo(true)
        .build();
        
  private final Setting<Integer> alpha =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("alpha")
      .description("make the render see-through")
      .defaultTo(200)
      .max(255)
      .min(0)
      .build();
	
	public PlayerData() {
		super(Category.GUI, "PlayerInfo", false, "See player data on screen");
	}

	@Override
	protected Align getDefaultAlignment() {
		return Align.TOPLEFT;
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
		return 1;
	}
	
	private static LinkedList<ItemStack> armorStacks = new LinkedList<>();
	
	@SubscribeEvent
	public void onRenderScreen(RenderGameOverlayEvent.Text event) {
		
		GlStateManager.pushMatrix();
		
		GlStateManager.color(1, 1, 1, alpha.getAsFloat() / 255);
		
		int yOffset = 0;
		
		GlStateManager.scale(scale.getAsDouble(), scale.getAsDouble(), scale.getAsDouble());
					       
		if(renderHealth.getAsBoolean()) {
			
			renderHealthBar(getPosX(0), getPosY(yOffset), MC.player);
			yOffset += 14 * scale.getAsDouble();
			
		}
		
		if(renderFood.getAsBoolean()) {
			
			renderFoodData(getPosX(0), getPosY(yOffset), MC.player);
			yOffset += 28 * scale.getAsDouble();
			
		}
		
		if(renderArmor.getAsBoolean()) {
			
			renderArmorData(getPosX(0), getPosY(yOffset), MC.player);
			
		}
		
		GlStateManager.popMatrix();

	}
	
	private void renderArmorData(int xraw, int yraw, EntityPlayer entity) {
		
		int x = (int) (xraw / scale.getAsDouble());
		int yConverted = (int) (yraw / scale.getAsDouble());
		
		final int width = 80;
		final int height = 14;
		
		int yOffset = 0;
				
		entity.getArmorInventoryList().forEach(stack -> armorStacks.addFirst(stack));
		
		for(ItemStack armorStack : armorStacks) {
			
			if(armorStack.isEmpty()) {
				continue;
			}
			
			int y = yConverted + yOffset;
			
			int damage = armorStack.getItemDamage();
			
			int maxDamage = armorStack.getMaxDamage();
			
			if(maxDamage == 0) {
				continue;
			}
			
			String toRender = String.format("%d/%d", maxDamage - damage, maxDamage);
			
			float percentage = ((maxDamage - damage) / (float)maxDamage);
			
			percentage = MathHelper.clamp(percentage, 0, 1);
			
			int color = Color.HSBtoRGB(percentage * 0.25F - 1, 1, 1);
			
			int colorDarker = new Color(color).darker().getRGB();

			
			SurfaceHelper.drawRect(x, y, width, height, Colors.BLACK.toBuffer()); //Black outline
			
			SurfaceHelper.drawRect(x + 1, y + 1, width - 2, height - 2, color); //Background
			
			SurfaceHelper.drawRect(x + 1, y + 1, (int) ((width - 2) * percentage), height - 2, colorDarker); //Foreground
			
			GlStateManager.color(1, 1, 1, alpha.getAsFloat() / 255);
			
			MC.getRenderItem().renderItemAndEffectIntoGUI(armorStack, x + 1, y - 1);

	        SurfaceHelper.drawText(toRender, x + 18, y + 3, Colors.BLACK.toBuffer());
			
			
			yOffset += height;
			
		}
		
		armorStacks.clear();
		
	}
	
	private void renderFoodData(int xraw, int yraw, EntityPlayer entity) {
	
		int x = (int) (xraw / scale.getAsDouble());
		int y = (int) (yraw / scale.getAsDouble());
		
		FoodStats foodStats = entity.getFoodStats();
		
		int foodLevel = foodStats.getFoodLevel();
		
		float saturationLevel = foodStats.getSaturationLevel();
		
		final int width = 80;
		
		final int height = 14;
		
		String foodToRender = String.format("%d/%d", foodLevel, 20);
		String saturationToRender = String.format("%1.1f/%d", saturationLevel, 20);
		
		float foodPercentage = foodLevel / 20f;
		float satPercentage = saturationLevel / 20f;
		
		foodPercentage = MathHelper.clamp(foodPercentage, 0, 1);
		satPercentage = MathHelper.clamp(satPercentage, 0, 1);
		
		int foodColor = Color.HSBtoRGB(foodPercentage * 0.33F, 1, 1);
		int foodColorDarker = new Color(foodColor).darker().getRGB();
		
		int satColor = Color.HSBtoRGB(satPercentage * 0.33F, 1, 1);
		int satColorDarker = new Color(satColor).darker().getRGB();

		SurfaceHelper.drawRect(x, y, width, height * 2, Colors.BLACK.toBuffer()); //Black outline
		
		SurfaceHelper.drawRect(x + 1, y + 1, width - 2, height - 2, foodColor); //Food Background

		SurfaceHelper.drawRect(x + 1, y + height + 1, width - 2, height - 2, satColor); //Sat Background

		SurfaceHelper.drawRect(x + 1, y + 1, (int) ((width - 2) * foodPercentage), height - 2, foodColorDarker); //Food Foreground

		SurfaceHelper.drawRect(x + 1, y + height + 1, (int) ((width - 2) * satPercentage), height - 2, satColorDarker); //Food Foreground
	
		GlStateManager.color(1, 1, 1, alpha.getAsFloat() / 255);
		MC.getTextureManager().bindTexture(Gui.ICONS);
        SurfaceHelper.drawTexturedRect(x + 4, y + 3, 16, 27, 9, 9, 0); //Food background
        SurfaceHelper.drawTexturedRect(x + 4, y + 3, 52, 27, 9, 9, 0); //Food draw

        SurfaceHelper.drawTexturedRect(x + 4, y + height + 3, 16, 0, 9, 9, 0); //Sat background
        SurfaceHelper.drawTexturedRect(x + 4, y + height + 3, 162, 0, 9, 9, 0);
        
        SurfaceHelper.drawText(foodToRender, x + 18, y + 3, Colors.BLACK.toBuffer());
        SurfaceHelper.drawText(saturationToRender, x + 18, y + height + 3, Colors.BLACK.toBuffer());

	
	}
	
	private void renderHealthBar(int xraw, int yraw, Entity entity) {
		
		int x = (int) (xraw / scale.getAsDouble());
		int y = (int) (yraw / scale.getAsDouble());
		
		float health = EntityUtils.getHealth(MC.player);
		
		float maxhealth = MC.player.getMaxHealth();
		
		final int width = 80;
		
		final int height = 14;
		
		final int TOP =  9 * (MC.world.getWorldInfo().isHardcoreModeEnabled() ? 5 : 0);
		
		String toRender = String.format("%1.1f/%1.0f", health, maxhealth);
		
		float percentage = health / maxhealth;
		
		percentage = MathHelper.clamp(percentage, 0, 1);
		
		int color = Color.HSBtoRGB(percentage * 0.33F, 1, 1);
		
		int colorDarker = new Color(color).darker().getRGB();
		
		SurfaceHelper.drawRect(x, y, width, height, Colors.BLACK.toBuffer()); //Black outline
		
		SurfaceHelper.drawRect(x + 1, y + 1, width - 2, height - 2, color); //Background
		
		SurfaceHelper.drawRect(x + 1, y + 1, (int) ((width - 2) * percentage), height - 2, colorDarker); //Foreground
		
		GlStateManager.color(1, 1, 1, alpha.getAsFloat() / 255);
		MC.getTextureManager().bindTexture(Gui.ICONS);
		
		SurfaceHelper.drawTexturedRect(x + 4, y + 3, 16, TOP, 9, 9, 0); //Heart background
        SurfaceHelper.drawTexturedRect(x + 4, y + 3, 52, TOP, 9, 9, 0);
        SurfaceHelper.drawText(toRender, x + 18, y + 3, Colors.BLACK.toBuffer());
		
	}
	
}
