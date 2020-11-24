package com.matt.forgehax.mods;

import java.awt.Color;
import java.awt.image.BufferedImage;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWrittenBook;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class BookRender extends ToggleMod {

	public BookRender() {
		super(Category.RENDER, "BookRender", false, "renders book contents instead of tooltip");
	}


	/**
	 * TheAlphaEpsilon
	 * 17 April 2020
	 */
	
	private final Setting<Float> scale =
		    getCommandStub()
		      .builders()
		      .<Float>newSettingBuilder()
		      .name("scale")
		      .description("The scale of how big the book should be rendered")
		      .defaultTo(0.5F)
		      .build();
	
	private static final int backgroundColor = new Color(203, 188, 147).getRGB();
	private static final int outlineColor = new Color(153, 135, 108).getRGB();

	private static BookRenderer toRender = null;
	
	//Cancel normal tooltips
	@SubscribeEvent
	public void onHover(RenderTooltipEvent.Pre event) {
		Item item = event.getStack().getItem();
				
		if(!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
			return;
		} else if(item != null &&  item instanceof ItemWrittenBook) {
			event.setCanceled(true);
		}
	}
	
	//To draw new gui
	@SubscribeEvent
	public void drawNewGui(GuiScreenEvent.DrawScreenEvent.Post event) {
		
		if(!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) {
			return;
		}
		
		GlStateManager.disableDepth();
		
		GlStateManager.disableLighting();

		GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;
		
		Slot hovered = container.getSlotUnderMouse();
		
		if(hovered != null) {
			
			Item item = hovered.getStack().getItem();
						
			if(item instanceof ItemWrittenBook) {
				
				if(toRender == null || (toRender != null && !toRender.isNBTSame(hovered.getStack().getTagCompound()))) {
					toRender = new BookRenderer(hovered.getStack(), 100, 10);
				} 
				toRender.draw(event.getMouseX(), event.getMouseY());
			}
			
		}
	}
	
	//Draw the big book blob
	class BookRenderer extends Gui {
		
		private final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
						
		/**
		 * space between text and metadata
		 */
		private static final int metaSpace = 11;
		
		private String author;
		private String title;
		private String[] pages;
		private NBTTagCompound nbt;
		private int width;
		private int height;
		private int padding;
		private Generation gen;
		private ResourceLocation background;
		private String nbtString;
		
		private BookRenderer(ItemStack bookStack, int tempwidth, int padding) {
			this.nbt = bookStack.getTagCompound();
			this.nbtString = nbt.toString();
			author = nbt.getString("author");
			title = nbt.getString("title");
			pages = getPageText(nbt);
			width = tempwidth;
			this.padding = padding;
			
			//Generation
			if(!nbt.hasKey("generation")) {
				//if no generation key, assume og
				gen = Generation.OG;
			} else {
				int type = nbt.getInteger("generation");
				switch(type) {
				case(0):
					gen = Generation.OG;
					break;
				case(1):
					gen = Generation.OGCOP;
					break;
				case(2):
					gen = Generation.COPCOP;
					break;
				case(3):
					gen = Generation.TAT;
				}
			}
			
			if(fontRenderer.getStringWidth(title) > width) {
				width = fontRenderer.getStringWidth(title);
			}
			if(fontRenderer.getStringWidth("By: " + author) > width) {
				width = fontRenderer.getStringWidth("By: " + author);
			}
			if(fontRenderer.getStringWidth("Generation: " + gen.toString()) > width) {
				width = fontRenderer.getStringWidth("Generation: " + gen.toString());
			}
			
			//Auto resize
			while(calcHeight() > this.width * 2) {
				this.width *= 2;
			}
			
			height = calcHeight();
			
			BufferedImage background = new BufferedImage(width + padding * 2, height + padding * 2, BufferedImage.TYPE_INT_ARGB);
			
			for(int i = 0; i < width + padding * 2; i++) {
				for(int j = 0; j < height + padding * 2; j++) {
					background.setRGB(i, j, backgroundColor);
				}
			}
			
			//draw background tattering using nbt data as pseudorandom
			
			for(int i = 0; i < padding * 2 + width; i++) {				
				int index = 10 * nbtString.charAt(i % nbtString.length());
				
				int value1 = (int) (2 * Math.sin(nbtString.charAt(index % nbtString.length())));
				
				int value2 = (int) (2 * Math.sin(nbtString.charAt(Math.abs(value1 % nbtString.length()))));
								
				for(int j = 0; j < value1 + padding * 0.25; j++) {
					background.setRGB(i, j, outlineColor);
				}
				
				for(int j = height + padding * 2 - 1; j > height + padding * 1.75 + value2; j--) {
					background.setRGB(i, j, outlineColor);
				}
				
			}
			
			for(int j = 0; j < height + padding * 2; j++) {
				
				int index = nbtString.charAt(j % nbtString.length()) ^ j;
				
				int value1 = (int) (2 * Math.sin(nbtString.charAt(index % nbtString.length())));
				
				int value2 = (int) (2 * Math.sin(nbtString.charAt(Math.abs(value1 % nbtString.length()))));
				
				
				for(int i = 0; i < value1 + padding * 0.25; i++) {
					background.setRGB(i, j, outlineColor);
				}
				
				for(int i = width + padding * 2 - 1; i > width + padding * 1.75 + value2; i--) {
					background.setRGB(i, j, outlineColor);
				}
				
			}
			
			this.background = MC.getTextureManager().getDynamicTextureLocation("BookRenderBGround", new DynamicTexture(background));
			
		}
		
		//Draw white and call text
		private void draw(int x, int y) {
			
			double scaleDouble = scale.getAsDouble();
			
			GlStateManager.scale(scaleDouble, scaleDouble, scaleDouble);
						
			x *= 1 / scaleDouble;
			y *= 1 / scaleDouble;
						
			MC.getTextureManager().bindTexture(background);
			
			Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, width + padding * 2, height + padding * 2, width + padding * 2, height + padding * 2, width + padding * 2, height + padding * 2);
			
			drawHorizontalLine(x + padding, x + padding + width, y + height + padding - fontRenderer.FONT_HEIGHT * 2 - metaSpace / 2, Color.BLACK.getRGB());

			drawText(x + padding, y + padding);
			
			GlStateManager.scale(1 / scaleDouble, 1 / scaleDouble, 1 / scaleDouble);
			
		}
		
		private void drawText(int x, int y) {
						
			fontRenderer.drawString(title, x + width / 2 - fontRenderer.getStringWidth(title) / 2, y, 0);
			fontRenderer.drawString("By: " + author, x + width / 2 - fontRenderer.getStringWidth("By: " + author) / 2, y + fontRenderer.FONT_HEIGHT, 0);
			
			fontRenderer.drawSplitString(arrayToString(pages), x, y + fontRenderer.FONT_HEIGHT * 2, width, 0);
			
			int textHeight = fontRenderer.getWordWrappedHeight(arrayToString(pages), width);
			
			fontRenderer.drawString("Generation: " + gen.toString(), x, y + fontRenderer.FONT_HEIGHT * 2 + textHeight + metaSpace, 0);
			fontRenderer.drawString("Pages: " + pages.length, x, y + fontRenderer.FONT_HEIGHT * 3 + textHeight + metaSpace, 0);
									
		}
		
		private int calcHeight() {
			
			int textHeight = fontRenderer.getWordWrappedHeight(arrayToString(pages), width);
			
			return fontRenderer.FONT_HEIGHT * 4 + textHeight + metaSpace;
			
		}

		private String arrayToString(String[] array) {
			StringBuffer buff = new StringBuffer();
			for(int i = 0; i < array.length; i++) {
				buff.append(array[i]);
				buff.append(" ").append((char)0xa7).append('r');
			}
			return buff.toString();
		}
		
		private String[] getPageText(NBTTagCompound nbt) {
			
			NBTTagList pages = nbt.getTagList("pages", 8);
			
			String[] toReturn = new String[pages.tagCount()];
			
			for(int i = 0; i < pages.tagCount(); i++) {
				
				try { //10
					
					String text = ITextComponent.Serializer.jsonToComponent(pages.getStringTagAt(i)).getUnformattedText();
					
					toReturn[i] = text;
					
					
				} catch (Exception e) {
					
				}
				
			}
			
			return toReturn;
		}
		
		private boolean isNBTSame(NBTTagCompound other) {
			return nbt.equals(other);
		}
		
		
	}
	
	static enum Generation {
		OG, OGCOP, COPCOP, TAT;
		
		@Override
		public String toString() {
			switch(this) {
			case OG:
				return "Original";
			case OGCOP:
				return "Copy of Original";
			case COPCOP:
				return "Copy of a Copy";
			case TAT:
				return "Tattered";
			default:
				return "ERROR";
			}
		}
		
	}
	
}
