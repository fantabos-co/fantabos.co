package com.matt.forgehax.mods;

import org.lwjgl.opengl.GL11;

import com.matt.forgehax.asm.events.RenderEnderCrystalEvent;
import com.matt.forgehax.mods.EntityESP.ESPMode;
import com.matt.forgehax.mods.services.RainbowService;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.RenderUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/*TheAlphaEpsilon 3OCT2020*/

@RegisterMod
public class CrystalESP extends ToggleMod {

	public CrystalESP() {
		super(Category.RENDER, "CrystalESP", false, "Render crystals");
	}

	private final Setting<MODE> mode =
		    getCommandStub()
		      .builders()
		      .<MODE>newSettingEnumBuilder()
		      .name("mode")
		      .description("Rendering mode [outline/wireframe]")
		      .defaultTo(MODE.OUTLINE)
		      .build();
	
	private final Setting<Color> color =
			getCommandStub()
				.builders()
				.newSettingColorBuilder()
				.name("color")
				.description("The color of the crystal esp")
				.defaultTo(Colors.WHITE)
				.build();

	private final Setting<Boolean> rainbow =
			  getCommandStub()
			  	.builders()
			  	.<Boolean>newSettingBuilder()
			  	.name("rainbow")
			  	.description("Use rainbow color instead")
			  	.defaultTo(false)
			  	.build();
	
	private final Setting<Float> linewidth =
			 getCommandStub()
		      .builders()
		      .<Float>newSettingBuilder()
		      .name("width")
		      .description("Line width")
		      .defaultTo(2.0F)
		      .min(0f)
		      .max(10f)
		      .build();
	
	private final Setting<Boolean> ontop =
			  getCommandStub()
			  	.builders()
			  	.<Boolean>newSettingBuilder()
			  	.name("ontop")
			  	.description("Renders the esp over actual entity (only for wireframe)")
			  	.defaultTo(true)
			  	.build();
	
	@SubscribeEvent
	public void onRenderCrystal(RenderEnderCrystalEvent event) {

		
        float rot = event.rotation;
        float hi = event.height;
        
		Color c;
		if (rainbow.get()) c = RainbowService.getRainbowColorClass();
		else c = color.get();

		//Renders the actual entity first
		if (this.ontop.getAsBoolean()) {
            event.model.render(event.crystal, 0, rot * 3F, hi * 0.2F, 0.0F, 0.0F, 0.0625F);
		}
	      
	      
		boolean fancyGraphics = MC.gameSettings.fancyGraphics; //Dont need fancy
		MC.gameSettings.fancyGraphics = false;
		float gamma = MC.gameSettings.gammaSetting;
		MC.gameSettings.gammaSetting = 10000.0f; //For extreme lines
	
        if(mode.get() == MODE.OUTLINE) {
        	
        	RenderUtils.renderOne(linewidth.getAsFloat());
    		event.model.render(event.crystal, 0, rot * 3F, hi * 0.2F, 0.0F, 0.0F, 0.0625F);
            GlStateManager.glLineWidth(linewidth.getAsFloat());
            
            RenderUtils.renderTwo();
    		event.model.render(event.crystal, 0, rot * 3F, hi * 0.2F, 0.0F, 0.0F, 0.0625F);
            GlStateManager.glLineWidth(linewidth.getAsFloat());
            
            RenderUtils.renderThree();

            RenderUtils.renderFour(c);
            event.model.render(event.crystal, 0, rot * 3F, hi * 0.2F, 0.0F, 0.0F, 0.0625F);
            GlStateManager.glLineWidth(linewidth.getAsFloat());

            RenderUtils.renderFive();
        	
        } else {
        	
        	GL11.glPushMatrix();
            GL11.glPushAttrib(1048575);
            GL11.glPolygonMode(1032, 6913);
            
            GL11.glDisable(3553);
            GL11.glDisable(2896);
            GL11.glDisable(2929);
            GL11.glEnable(2848);
            GL11.glEnable(3042);
            GlStateManager.blendFunc(770, 771);
            GlStateManager.color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
            GlStateManager.glLineWidth(linewidth.getAsFloat());
            event.model.render(event.crystal, 0, rot * 3F, hi * 0.2F, 0.0F, 0.0F, 0.0625F);
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        	
        }
        
        
        //Renders the actual entity last
        if (!this.ontop.getAsBoolean()) {
            event.model.render(event.crystal, 0, rot * 3F, hi * 0.2F, 0.0F, 0.0F, 0.0625F);
        }
        try {
            MC.gameSettings.fancyGraphics = fancyGraphics;
            MC.gameSettings.gammaSetting = gamma;
        }
        catch (Exception exception) {
            // empty catch block
        }
        event.setCanceled(true);
        
	}

	static enum MODE {
		OUTLINE, WIREFRAME
	}
}
