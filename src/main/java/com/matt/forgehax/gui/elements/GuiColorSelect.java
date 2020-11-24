package com.matt.forgehax.gui.elements;

import static com.matt.forgehax.Globals.*;

import java.awt.image.BufferedImage;

import com.matt.forgehax.gui.windows.GuiWindowSetting;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.DynamicModifiableImageTexture;
import com.matt.forgehax.util.draw.SurfaceHelper;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/*TheAlphaEpsilon 1OCT2020*/
public class GuiColorSelect extends GuiElement {

	private static final BufferedImage SLIDERIMAGE = new BufferedImage(360, 1, BufferedImage.TYPE_INT_ARGB);
	
	static {
		for(int i = 0; i < 360; i++) {
			SLIDERIMAGE.setRGB(i, 0, java.awt.Color.HSBtoRGB(i / 360F, 1, 1));
		}
	}
	
	private static final ResourceLocation SCROLLBAR = 
			MC.getTextureManager().getDynamicTextureLocation("GuiColorSelectBGround", 
					new DynamicTexture(SLIDERIMAGE));
	
	private static final int SLIDERHEIGHT = 5;
	private static final int SLIDERWIDTH = 70;
	
	private static final int PICKERSIZE = 50;
	private static final int PICKERYOFFSET = 14;
	
	//Changes with the slider's hue
	private final BufferedImage pickerFGImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
	private final DynamicModifiableImageTexture pickerFGImageTexture = new DynamicModifiableImageTexture(pickerFGImage);
	private final ResourceLocation pickerFG = pickerFGImageTexture.getResource(); 
	
	//Normally always positive
	private int sliderX = -1;
	
	//XPos of the huebar
	private int hueBarX;
	
	//XPos of the color picker
	private int colorPickerX;
	
	//The hue of the color picker
	private float pickerHue = -1;
	
	//To choose the color
	private int pickerX = -1;
	private int pickerY = -1;
	
	private boolean holdingSlider = false;
	private boolean holdingPicker = false;
	
	//True when setting slider
	private boolean pickingNewColor = false;
	
	private float[] initHSB;
	
	private Setting<Color> setting;
	
	public GuiColorSelect(Setting<Color> commandIn, GuiWindowSetting parent) {
		super(commandIn, parent);
		setting = commandIn;
		
		Color color = setting.get();
		
		initHSB = java.awt.Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
						
	    height = 75;
	}

	@Override
	public void draw(int mouseX, int mouseY) {
		super.draw(mouseX, mouseY);
	    SurfaceHelper.drawTextShadow(setting.getName(), (x + 2), y + 1, Colors.WHITE.toBuffer());
		drawHueBar(16 + PICKERSIZE);
		
		float sliderHue = (float)sliderX / SLIDERWIDTH;

		if(holdingSlider) {
			pickingNewColor = true;
		}
		
		if(!pickingNewColor && !holdingPicker) {
			
			Color color = setting.get();
			float settingHue = java.awt.Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[0];
		
			//If not picking a color anymore and hues are different = reset
			if(settingHue < sliderHue - 0.1 || settingHue > sliderHue + 0.1) {
				
				sliderX = -1;
				pickerX = -1;
				pickerY = -1;
				
				initHSB = java.awt.Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
				
			}
			
		}
		
		if(sliderX < 0) {
			sliderX = (int)(SLIDERWIDTH * initHSB[0]);
		}
		if(holdingSlider) {
			setSliderX(mouseX);
		}
		
		drawSlider(16 + PICKERSIZE);
		drawPickerFGround(PICKERYOFFSET);
		if(pickerX < 0 || pickerY < 0) {
			pickerX = (int) (PICKERSIZE * initHSB[1]);
			pickerY = PICKERSIZE - (int) (PICKERSIZE * initHSB[2]);
		}
		if(holdingPicker) {
			setPicker(mouseX, mouseY);
			float sat = (float)pickerX / PICKERSIZE;
			float bri = 1 - (float)pickerY / PICKERSIZE;
			setting.set(Color.of(java.awt.Color.HSBtoRGB(sliderHue, sat, bri)), false);
			pickingNewColor = false;
		}
		drawPicker(PICKERYOFFSET);
	}
	
	@Override
	public void mouseClicked(int mouseX, int mouseY, int state) {
		
		if(inHueBar(mouseX, mouseY)) {
			setSliderX(mouseX);
		    holdingSlider = true;
		}
		
		if(inPicker(mouseX, mouseY)) {
			setPicker(mouseX, mouseY);
			holdingPicker = true;
		}
		
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int state) {
	    holdingSlider = false;
	    holdingPicker = false;
	}
	
	@Override
	public void onRemoved() {
		pickerFGImageTexture.deleteGlTexture();
	}
	
	private boolean inPicker(int mouseX, int mouseY) {
		
		return colorPickerX - 1 <= mouseX &&
				colorPickerX + PICKERSIZE + 1 >= mouseX &&
				y + 13 <= mouseY &&
				y + 15 + PICKERSIZE >= mouseY;
		
	}
	
	private void drawPickerFGround(int yOffset) {

		colorPickerX = x + width / 2 - PICKERSIZE / 2;

		float sliderHue = (float)sliderX / SLIDERWIDTH;
				
		attemptColorPickerRedraw(sliderHue);
		
		MC.getTextureManager().bindTexture(pickerFG);
		
        Gui.drawScaledCustomSizeModalRect(colorPickerX, y + yOffset, 0, 0, 100, 100, PICKERSIZE, PICKERSIZE, 100, 100);
		        
	}
	
	private void drawPicker(int offset) {
		
		SurfaceHelper.drawLine(colorPickerX + pickerX - 2, y + offset + pickerY, colorPickerX + pickerX + 2, y + offset + pickerY, Colors.BLACK.toBuffer(), 2);
		SurfaceHelper.drawLine(colorPickerX + pickerX, y + offset + pickerY - 2, colorPickerX + pickerX, y + offset + pickerY + 2, Colors.BLACK.toBuffer(), 2);
	
	}
	
	private void drawSlider(int yOffset) {
		SurfaceHelper.drawLine(sliderX + hueBarX - 1, y + yOffset, hueBarX + sliderX - 1, y + yOffset + SLIDERHEIGHT, Colors.BLACK.toBuffer(), 2);
		SurfaceHelper.drawLine(sliderX + hueBarX + 1, y + yOffset, hueBarX + sliderX + 1, y + yOffset + SLIDERHEIGHT, Colors.BLACK.toBuffer(), 2);
	}
	
	private boolean inHueBar(int mouseX, int mouseY) {
		
		return hueBarX - 1 <= mouseX &&
				hueBarX + SLIDERWIDTH + 1 >= mouseX &&
				y + 15 + PICKERSIZE <= mouseY &&
				y + 17 + PICKERSIZE + SLIDERHEIGHT >= mouseY;
		
	}
	
	private void drawHueBar(int yOffset) {
		
		hueBarX = x + width / 2 - SLIDERWIDTH / 2;
		
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        MC.getTextureManager().bindTexture(SCROLLBAR);
        
        Gui.drawScaledCustomSizeModalRect(hueBarX, y + yOffset, 0, 0, 360, 1, SLIDERWIDTH, SLIDERHEIGHT, 360, 1);
				
	}
	
	//TODO: Math stuff to make this more efficient?
	private void attemptColorPickerRedraw(float hueBar) {
		
		if(pickerHue == hueBar) {
			return;
		}
		
		pickerHue = hueBar;
		
		for(int i = 0; i < 100; i++) {
			for(int j = 0; j < 100; j++) {
				
				pickerFGImage.setRGB(i, 99-j, java.awt.Color.HSBtoRGB(pickerHue, (float)i/99, (float)j/99));
				
			}
		}
		
		pickerFGImageTexture.updateDynamicTexture();
		
	}
	
	private void setPicker(int mouseX, int mouseY) {
		pickerX = MathHelper.clamp(mouseX, colorPickerX, colorPickerX + PICKERSIZE) - colorPickerX;
		pickerY = MathHelper.clamp(mouseY, y + PICKERYOFFSET, y + PICKERYOFFSET + PICKERSIZE) - (y+PICKERYOFFSET);
	}
	
	private void setSliderX(int mouseX) {
		sliderX = MathHelper.clamp(mouseX, hueBarX, hueBarX + SLIDERWIDTH) - hueBarX;
	}
	
}
