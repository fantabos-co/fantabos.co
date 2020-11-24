package com.matt.forgehax.mods;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

/*TheAlphaEpsilon 6NOV2020*/
@RegisterMod
public class ThrownEntityTrails extends ToggleMod {

	public ThrownEntityTrails() {
		super(Category.RENDER, "EntityTrails", false, "Displays trails behind thrown entities");
	}
	
	private Setting<Boolean> snowball =
			getCommandStub()
			.builders()
			.<Boolean>newSettingBuilder()
			.defaultTo(false)
			.name("snowball")
			.description("Render trails on snowballs")
			.build();
	
	private Setting<Color> colorSnowball =
			getCommandStub()
			.builders()
			.newSettingColorBuilder()
			.defaultTo(Colors.WHITE)
			.name("snowballColor")
			.description("Color of snowball trails")
			.build();
	
	private Setting<Boolean> epearl =
			getCommandStub()
			.builders()
			.<Boolean>newSettingBuilder()
			.defaultTo(true)
			.name("enderpearl")
			.description("Render trails on enderpearls")
			.build();
	
	private Setting<Color> colorPearl =
			getCommandStub()
			.builders()
			.newSettingColorBuilder()
			.defaultTo(Color.of(0, 133, 15))
			.name("enderpearlColor")
			.description("Color of pearl trails")
			.build();
	
	private Setting<Boolean> arrow =
			getCommandStub()
			.builders()
			.<Boolean>newSettingBuilder()
			.defaultTo(false)
			.name("arrow")
			.description("Render trails on arrows")
			.build();
	
	private Setting<Color> colorArrow =
			getCommandStub()
			.builders()
			.newSettingColorBuilder()
			.defaultTo(Colors.DARK_RED)
			.name("arrowColor")
			.description("Color of arrow trails")
			.build();
	
	private Setting<Boolean> potion =
			getCommandStub()
			.builders()
			.<Boolean>newSettingBuilder()
			.defaultTo(false)
			.name("potion")
			.description("Render trails on potions")
			.build();
	
	private Setting<Color> colorPotion =
			getCommandStub()
			.builders()
			.newSettingColorBuilder()
			.defaultTo(Colors.BLUE)
			.name("potionColor")
			.description("Color of potion trails")
			.build();
	
	private Setting<Float> lineWidth =
			getCommandStub()
			.builders()
			.<Float>newSettingBuilder()
			.defaultTo(1F)
			.min(1F)
			.max(5F)
			.name("lineWidth")
			.description("The width of the trail")
			.build();
	
	private Setting<Integer> max =
			getCommandStub()
			.builders()
			.<Integer>newSettingBuilder()
			.defaultTo(50)
			.max(100)
			.min(1)
			.name("max")
			.description("The maximum amount of trails to render")
			.build();
	
	private Setting<Boolean> alwaysShow =
			getCommandStub()
			.builders()
			.<Boolean>newSettingBuilder()
			.defaultTo(true)
			.name("alwaysShow")
			.description("Show the trails through walls")
			.build();
	
	Set<EntityWrapper> wrappers = new HashSet<>();
	
	@SubscribeEvent
	public void onEntityJoinWorld(EntityJoinWorldEvent event) { //Main thread
		synchronized(wrappers) {
			if(wrappers.size() > max.getAsInteger()) {
				return;
			}
			Entity e = event.getEntity();
			if(snowball.getAsBoolean() && e instanceof EntitySnowball) {
				EntityWrapper ew = new EntityWrapper(e, colorSnowball.get());
				wrappers.add(ew);
			}
			if(epearl.getAsBoolean() && e instanceof EntityEnderPearl) {
				EntityWrapper ew = new EntityWrapper(e, colorPearl.get());
				wrappers.add(ew);
			}
			if(potion.getAsBoolean() && e instanceof EntityPotion) {
				EntityWrapper ew = new EntityWrapper(e, colorPotion.get());
				wrappers.add(ew);
			}
			if(arrow.getAsBoolean() && e instanceof EntityArrow) {
				EntityWrapper ew = new EntityWrapper(e, colorArrow.get());
				wrappers.add(ew);
			}
		}
	}
	
	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) { //Main thread
		if(event.phase == Phase.END) {
			synchronized(wrappers) {
				Iterator<EntityWrapper> iter = wrappers.iterator();
				while(iter.hasNext()) {
					EntityWrapper ew = iter.next();
					if(ew.positions.isEmpty()) {
						iter.remove();
					} else if(ew.isAlive()) {
						ew.tick();
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onDraw(RenderWorldLastEvent event) { //Render thread
		Vec3d e = MC.getRenderViewEntity().getPositionVector().
				add(EntityUtils.getInterpolatedAmount(MC.getRenderViewEntity(), MC.getRenderPartialTicks()));
		synchronized(wrappers){
			wrappers.forEach(x -> x.draw(e));
		}
	}
	
	class EntityWrapper {
	
		private Color color;
		private Entity entity;
		private ArrayList<Node> positions = new ArrayList<>();
		
		private EntityWrapper(Entity entity, Color color) {
			this.entity = entity;
			this.color = color;
			positions.add(new Node(new Vec3d(entity.posX, entity.posY, entity.posZ)));
		}
		
		private boolean isAlive() {
			return entity != null &&
					!entity.isDead &&
					(entity.isAirBorne || (!withinError(entity.posX, entity.lastTickPosX, 0.001) &&
					!withinError(entity.posY, entity.lastTickPosY, 0.001) &&
					!withinError(entity.posZ, entity.lastTickPosZ, 0.001)));
		}
		
		private void tick() { //Called in main thread
			synchronized(positions) {
				positions.add(new Node(entity.getPositionVector().
						add(EntityUtils.getInterpolatedAmount(entity, MC.getRenderPartialTicks()))));
			}
		}
		
		private void draw(Vec3d viewEntity) { //Called in render thread
			
			if(positions.isEmpty()) {
				return;
			}
			
			GlStateManager.pushMatrix();
			
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			GlStateManager.enableBlend();
			GlStateManager.disableLighting();
			GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			GlStateManager.disableTexture2D();
			GlStateManager.depthMask(false);
			GlStateManager.glLineWidth(lineWidth.getAsFloat());
			
			if(alwaysShow.getAsBoolean()) {
				GlStateManager.disableDepth();
			}
	        
	        Tessellator tes = Tessellator.getInstance();
	        BufferBuilder buf = tes.getBuffer();
	        
	        buf.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
	        
	        synchronized(positions) {
	        	
	        	Iterator<Node> iter = positions.iterator();
		        while(iter.hasNext()) {
		        	Node n = iter.next();
		        	if(n.alpha < 0) {
		        		iter.remove();
		        	} else {
		        		Vec3d rel = n.pos.subtract(viewEntity);
		        		buf.pos(rel.x, rel.y , rel.z).
		        			color(color.getRedAsFloat(), 
		        					color.getGreenAsFloat(), 
		        					color.getBlueAsFloat(), 
		        					n.alpha)
		        			.endVertex();
		        		n.alpha -= 0.01;
		        	}
		        }
		        
		        tes.draw();
	        }
	        
	        GlStateManager.popMatrix();
		}
		
		@Override
		public boolean equals(Object other) {
			if(other == this) {
				return true;
			} else if(!(other instanceof EntityWrapper)) {
				return false;
			} else {
				return entity.equals(((EntityWrapper)other).entity);
			}
		}
		
		class Node {
			private float alpha = 1;
			private Vec3d pos;
			private Node(Vec3d pos) {
				this.pos = pos;
			}
		}
		
	}
	
	private static boolean withinError(double val, double comp, double error) {
		return val > comp - error && val < comp + error;
	}

}
