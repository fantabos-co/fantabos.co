package com.matt.forgehax.mods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.matt.forgehax.Helper;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.mods.services.AtlasService;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

import org.lwjgl.input.Mouse;

/**
 * 
 * TheAlphaEpsilon
 * 8NOV2020
 *
 */
@RegisterMod
public class AtlasWaypoints extends ToggleMod {

	private static final double[][] circle = new double[1000][2];
	static {
		for(int i = 0; i < circle.length; i++) {
			double angle = Math.PI * 2 * (i / (double)circle.length);
			circle[i][0] = Math.sin(angle);
			circle[i][1] = Math.cos(angle);
		}
	}
	
	public AtlasWaypoints() {
		super(Category.RENDER, "AtlasWaypoints", false, "Render waypoints from www.2b2tatlas.com");
	}
	
	private Setting<Boolean> antialias =
			getCommandStub()
			.builders()
			.<Boolean>newSettingBuilder()
			.name("antialias")
			.description("Smoother rendering")
			.defaultTo(true)
			.build();
	
	private Setting<Color> color =
			getCommandStub()
			.builders()
			.newSettingColorBuilder()
			.name("color")
			.description("The color of the waypoints")
			.defaultTo(Colors.WHITE)
			.build();
	
	private Setting<Integer> waypoints;
	
	@SuppressWarnings("unchecked")
	private Setting<Boolean> forceLoad =
			getCommandStub()
			.builders()
			.<Boolean>newSettingBuilder()
			.name("forceLoad")
			.description("Load waypoints even if not on 2b2t")
			.defaultTo(false)
			.changed(c -> {
				if(!c.getTo()) {
					locations = Collections.EMPTY_LIST;
				} else {
					Helper.printMessageNaked("Updated");
					updateLocs(waypoints.get(), c.getTo());
				}
			})
			.build();
	
	private Setting<Integer> hoverDelay =
			getCommandStub()
			.builders()
			.<Integer>newSettingBuilder()
			.name("hoverDelay")
			.description("How long to hover over a waypoint to render the name. 0 is instant.")
			.defaultTo(100)
			.min(0)
			.max(100)
			.changed(c -> {
				ticksHovered = 0; //Avoid out of bounds
				circleSpeedHover = 110 - c.getTo();
			})
			.build();
	
	private Setting<Integer> clickDelay =
			getCommandStub()
			.builders()
			.<Integer>newSettingBuilder()
			.name("clickDelay")
			.description("How long to rightclick over a waypoint to render the short description. 0 is instant.")
			.defaultTo(100)
			.min(0)
			.max(100)
			.changed(c -> {
				ticksClicked = 0; //Avoid out of bounds
				circleSpeedClick = 110 - c.getTo();
			})
			.build();
	
	private static final int dynamicRange = 5000;
	
	private int circleSpeedHover = 110 - hoverDelay.getAsInteger();
	private int circleSpeedClick = 110 - clickDelay.getAsInteger();
	
	private Vec3d lastposition = Vec3d.ZERO;
	private int ticksHovered = 0;
	private int ticksClicked = 0;
	
	private int distToTravel = 0;
	
	@SuppressWarnings("unchecked")
	private List<AtlasService.Location> locations = Collections.EMPTY_LIST;
	
	AtlasService.Location lastHovered = null;
	
	private boolean changeWorld = false;

	@Override
	public void onLoad() {
		
		/*Because of cringe*/
		waypoints =
				getCommandStub()
				.builders()
				.<Integer>newSettingBuilder()
				.name("waypoints")
				.description("How many waypoints to render")
				.defaultTo(20)
				.min(1)
				.max(200)
				.changed(c -> {
					updateLocs(c.getTo(), forceLoad.get());
				})
				.build(); 
				
		getCommandStub()
		.builders()
		.newCommandBuilder()
		.name("info")
		.description("search for and get info for locations")
		.processor(data -> {
			//TODO: Clicking the name = opening the gui screen (or just print info in chat?)
			//Utilize hover text?
			data.requiredArguments(1);
			String tag = data.getArgumentAsString(0);
			for(AtlasService.Location location : AtlasService.searchFor(tag)) {
				ITextComponent comp = new TextComponentString(location.getName());
				Helper.printInform(comp);
			}
		})
		.build();
	}
	
	private void updateLocs(int amt, boolean force) {
		if(!force && 
				(MC.getCurrentServerData() == null || 
				!MC.getCurrentServerData().serverIP.equals("2b2t.org"))) {
			distToTravel = Integer.MAX_VALUE;
			return;
		}
		if(MC.world == null || MC.player == null) {
			return;
		}
		changeWorld = false;
		lastposition = MC.getRenderViewEntity().getPositionVector();
		Tuple<List<AtlasService.Location>, Integer> vals = AtlasService.getNearest(lastposition, amt, MC.world.provider.getDimensionType());
		locations = vals.getFirst();
		distToTravel = vals.getSecond();
	}
	
	@Override
	public void onEnabled() {
		Helper.printInform("You have enabled ATLAS waypoints. Hover over a waypoint to see its name and distance. Hover over and right click to get extra information.");
		updateLocs(waypoints.getAsInteger(), forceLoad.getAsBoolean());
	}
	
	@SubscribeEvent
	public void onWorldChange1(WorldEvent.Load event) {
		changeWorld = true;
	}
	
	@SubscribeEvent
	public void updateLocation(LocalPlayerUpdateEvent event) {
		if(changeWorld || MC.getRenderViewEntity().getPositionVector().distanceTo(lastposition) > distToTravel) { //Arbitrary value
			updateLocs(waypoints.getAsInteger(), forceLoad.getAsBoolean());
		}
	}
	
	@SubscribeEvent
	public void keyPress(InputEvent.KeyInputEvent event) {
		if(MC.currentScreen == null && 
				ticksClicked == circle.length / circleSpeedClick && 
				lastHovered != null &&
				MC.gameSettings.keyBindSneak.isKeyDown()) {
			MC.addScheduledTask(() -> {
				MC.displayGuiScreen(new LocationInfoGui(lastHovered));
			});
		}
	}
	
	@SubscribeEvent
	public void onDraw(RenderWorldLastEvent event) {
		AtlasService.Location internalLastHovered = null;
		for (AtlasService.Location loc : locations) {
			Vec3d pos = new Vec3d(loc.getX(), loc.getY() == 0 ? 70 : loc.getY(), loc.getZ());
			Vec3d norm = pos.subtract(MC.getRenderViewEntity().getPositionVector()).normalize(); //Player -> Loc
			Vec3d look = MC.getRenderViewEntity().getLookVec().normalize(); //Player -> Look
			boolean hovered = Math.acos(norm.dotProduct(look)) < ((2*Math.PI)/ 180D); 
			//Angle between vectors is less than 2 arc minutes
			drawWaypoint(MC.getRenderViewEntity(), pos, false);
			if(hovered) {
				internalLastHovered = loc;
			}
		}
		
		if(lastHovered != internalLastHovered) {
			ticksHovered = 0;
			ticksClicked = 0;
		}
		
		lastHovered = internalLastHovered;
		
		//TODO: Make a cooler, less glaring waypoints image
		if(lastHovered != null) {
			int y = lastHovered.getY();
			Vec3d pos = new Vec3d(lastHovered.getX(), y == 0 ? 70 : y, lastHovered.getZ());
			
			//Draw the actual waypoint
			drawWaypoint(MC.getRenderViewEntity(), pos, true);
			
			int textPos = Math.abs((lastHovered.getName().hashCode() % 8) / 2);
			
			//Handles hovering and name
			if(hoverDelay.get() == 0) { //Instant name display
				ticksHovered = circle.length / circleSpeedHover;
			}
			drawCircle(MC.getRenderViewEntity(), pos, 4, 6, 2, ticksHovered * circleSpeedHover, circleSpeedHover);
			ticksHovered++;
			if(ticksHovered * circleSpeedHover > circle.length) {
				ticksHovered = circle.length / circleSpeedHover;
				drawNameTag(MC.getRenderViewEntity(), lastHovered.getName(), pos, textPos);
			}
			
			//Handles click and info
			if(ticksClicked > 0) {
				drawCircle(MC.getRenderViewEntity(), pos, 6, 4, 2, ticksClicked * circleSpeedClick, circleSpeedClick);
			}
			if(ticksClicked == circle.length / circleSpeedClick) {
				ticksHovered = circle.length / circleSpeedHover; //Show name if blurb is shown
				drawBlurb(MC.getRenderViewEntity(), lastHovered, pos, textPos);
			}
			if(MC.gameSettings.keyBindUseItem.isKeyDown()) {
				if(clickDelay.get() == 0) { //Instant info display
					ticksClicked = circle.length / circleSpeedClick;
				}
				ticksClicked++;
				if(ticksClicked * circleSpeedClick > circle.length) {
					ticksClicked = circle.length / circleSpeedClick;
				}
			} else if(ticksClicked < circle.length / circleSpeedClick) {
				ticksClicked = 0;
			}
			
		} else {
			ticksHovered = 0;
			ticksClicked = 0;
		}
	}
	
	private void drawBlurb(Entity renderer, AtlasService.Location loc, Vec3d pos, int textpos) {
		GlStateManager.pushMatrix();
		initializeGl(renderer, pos, true);
		double distance = renderer.getPositionVector().distanceTo(pos);
		//GlStateManager.scale(0.5, 0.5, 0.5);
		drawBlurbAndBGround(loc, textpos, distance);
		GlStateManager.popMatrix();
	}
	
	private void drawBlurbAndBGround(AtlasService.Location loc, int pos, double distance) {
		
		//1 NAME
		//2 COORDS
		//3 DESCL1
		//4 DESCL2
		//5 DESCL3
		
		FontRenderer renderer = MC.fontRenderer;
		
		int nameWid = renderer.getStringWidth(loc.getName());
		String coord = "X:" + loc.getX() + " Z:" + loc.getZ();
		int locWid = renderer.getStringWidth(coord);
		String desc = loc.getDescription();
		int descWid = renderer.getStringWidth(desc);
		
		int maxNeeded = Math.max(nameWid, locWid);
		int max = Math.max(maxNeeded, descWid);
		
		max = Math.min(max, maxNeeded + 50);//Seems like a good number
				
		double left;
		double top;
		boolean drawDown;
				
		double scale = 1;
		
		//Opposite of name
		switch(pos) {
		case 3:
			left = -max - 5 * scale;
			top = -5 - 5 * scale;
			drawDown = false;
			break;
		case 2:
			left = 5 * scale;
			top = -5 - 5 * scale; 
			drawDown = false;
			break;
		case 1:
			left = -max - 5 * scale;
			top = 5 + 5 * scale;
			drawDown = true;
			break;
		case 0:
		default:
			left = 5 * scale;
			top = 5 + 5 * scale;
			drawDown = true;
			break;
		}
		
		float descscale = 0.5f;
		int lines = 3;
		List<String> cutdesc = renderer.listFormattedStringToWidth(desc, (int)(max / descscale));
		ArrayList<String> toDraw = new ArrayList<>(cutdesc.subList(0, Math.min(lines, cutdesc.size())));
		toDraw.add(". . .");
		toDraw.add("Sneak for More");
		
		double height = 4 * renderer.FONT_HEIGHT + ((toDraw.size() * renderer.FONT_HEIGHT) * descscale);
		
		drawBlurbBGround(left, max + 10, top, height, drawDown);
		drawBlurb(renderer, loc.getName(), coord, toDraw, left, max + 10, drawDown ? top : top - height, descscale);
		
	}
	
	private void drawBlurbBGround(double left, double max, double top, double height, boolean drawDown) {
		
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buff = tess.getBuffer();
		GlStateManager.glLineWidth(2f);
		GlStateManager.color(0, 0, 0);
		buff.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
		
		buff.pos(0, 0, 0).endVertex();
		buff.pos(left, top, 0).endVertex();
		
		buff.pos(0, 0, 0).endVertex();
		buff.pos(left + max, top, 0).endVertex();
		
		tess.draw();
		
		buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
		
		if(drawDown) {
			buff.pos(left, top + height, 0).endVertex();
			buff.pos(left + max, top + height, 0).endVertex();
			buff.pos(left + max, top, 0).endVertex();
			buff.pos(left, top, 0).endVertex();
		} else {
			buff.pos(left, top, 0).endVertex();
			buff.pos(left + max, top, 0).endVertex();
			buff.pos(left + max, top - height, 0).endVertex();
			buff.pos(left, top - height, 0).endVertex();
		}
		
		tess.draw();
		
	}
	
	private void drawBlurb(FontRenderer renderer, String name, String loc, List<String> info, double left, double max, double top, float scale) {
		GlStateManager.enableTexture2D();
		int nameWid = renderer.getStringWidth(name);
		int distWid = renderer.getStringWidth(loc);
		renderer.drawString(name, (int)(left + max / 2 - nameWid / 2), (int)(top) + 5, Colors.WHITE.toBuffer());
		renderer.drawString(loc, (int)(left+ max / 2 - distWid / 2), (int)(top) + renderer.FONT_HEIGHT + 5, Colors.WHITE.toBuffer());
		GlStateManager.scale(scale, scale, scale);
		int counter = 0;
		for(String s : info) {
			int swid = renderer.getStringWidth(s);
			renderer.drawString(
					s,
					(int) (-swid / 2 + ((1 / scale) * (left + (max / 2f)))),
					(int) (renderer.FONT_HEIGHT * (counter++ + 1) + 
							(1 / scale) * (5 + top + renderer.FONT_HEIGHT * 2)),
					Colors.WHITE.toBuffer()
					);
		}
		
	}

	private void drawCircle(Entity renderer, Vec3d pos, int radx, int rady, float width, int ticks, int speed) {
		GlStateManager.pushMatrix();
		initializeGl(renderer, pos, false);
		GlStateManager.glLineWidth(width);
		
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buff = tess.getBuffer();
				
		buff.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
		
		for(int i = 0; i < ticks; i++) {
			Color c = colorFromTick(i);
			buff.pos(radx * circle[i][0], rady * circle[i][1], 0)
				.color(c.getRedAsFloat(), c.getGreenAsFloat(), c.getBlueAsFloat(), 1).endVertex();
			if(i > circle.length - speed - 1) {
				c = colorFromTick(0);
				buff.pos(radx * circle[0][0], rady * circle[0][1], 0)
					.color(c.getRedAsFloat(), c.getGreenAsFloat(), c.getBlueAsFloat(), 1).endVertex();
			}
		}
		
		tess.draw();
		
		GlStateManager.popMatrix();
	}
	
	private Color colorFromTick(int tick) {
		float hue = (240/360f) + (30/360f) * Math.abs(1 - (2 * tick / (float)circle.length));
		float bri = (360/360f) - (180/360f) * Math.abs(1 - (2 * tick / (float)circle.length));
		Color c = Color.of(java.awt.Color.HSBtoRGB(hue, 1, bri));
		return c;
	}
	
	private void drawNameAndBGround(String name, int pos, double distance) {
		FontRenderer renderer = MC.fontRenderer;
		String dist = String.format("[%.0fm]", distance);
				
		int max = 10 + Math.max(renderer.getStringWidth(name), renderer.getStringWidth(dist));
		int height = renderer.FONT_HEIGHT * 3;
		double left;
		double top;
		boolean drawDown;
				
		double scale = 1;
		
		switch(pos) {
		case 0: //Up left
			left = -max - 5 * scale;
			top = -height - 5 * scale;
			drawDown = false;
			break;
		case 1: //Up right
			left = 5 * scale;
			top = -height - 5 * scale; 
			drawDown = false;
			break;
		case 2: //Down left
			left = -max - 5 * scale;
			top = height + 5 * scale;
			drawDown = true;
			break;
		case 3: //Down right
		default:
			left = 5 * scale;
			top = height + 5 * scale;
			drawDown = true;
			break;
		}
		
		drawNameBGround(left, max, top, height, drawDown);
		drawName(renderer, name, dist, left, max, drawDown ? top : top - height);
	}
	
	private void drawName(FontRenderer renderer, String name, String dist, double left, double max, double top) {
		
		GlStateManager.enableTexture2D();
		int nameWid = renderer.getStringWidth(name);
		int distWid = renderer.getStringWidth(dist);
		renderer.drawString(name, (int)(left + max / 2 - nameWid / 2), (int)(top) + 5, Colors.WHITE.toBuffer());
		renderer.drawString(dist, (int)(left+ max / 2 - distWid / 2), (int)(top) + renderer.FONT_HEIGHT + 5, Colors.WHITE.toBuffer());

	}
	
private void drawNameBGround(double left, double max, double top, double height, boolean drawDown) {
		
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buff = tess.getBuffer();
		GlStateManager.glLineWidth(2f);
		GlStateManager.color(0, 0, 0);
		buff.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
		
		buff.pos(0, 0, 0).endVertex();
		buff.pos(left, top, 0).endVertex();
		
		buff.pos(0, 0, 0).endVertex();
		buff.pos(left + max, top, 0).endVertex();
		
		tess.draw();
		
		buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
		
		if(drawDown) {
			buff.pos(left, top + height, 0).endVertex();
			buff.pos(left + max, top + height, 0).endVertex();
			buff.pos(left + max, top, 0).endVertex();
			buff.pos(left, top, 0).endVertex();
		} else {
			buff.pos(left, top, 0).endVertex();
			buff.pos(left + max, top, 0).endVertex();
			buff.pos(left + max, top - height, 0).endVertex();
			buff.pos(left, top - height, 0).endVertex();
		}
		
		tess.draw();
		
	}
	
	private void drawNameTag(Entity renderEntity, String name, Vec3d loc, int pos) {
		GlStateManager.pushMatrix();
		initializeGl(renderEntity, loc, true);
		double distance = renderEntity.getPositionVector().distanceTo(loc);
		drawNameAndBGround(name, pos, distance);
		GlStateManager.popMatrix();
	}
	
	//TODO: Make a cooler, less glaring waypoints image
	private void drawWaypoint(Entity renderEntity, Vec3d pos, boolean hovered) {
		GlStateManager.pushMatrix();
		
		initializeGl(renderEntity, pos, false);
		double distance = renderEntity.getPositionVector().distanceTo(pos);
		drawPoint(hovered ? Colors.YELLOW.toBuffer() : Colors.WHITE.toBuffer(), 1.3, (float) (distance / dynamicRange));
		drawPoint(Colors.BLACK.toBuffer(), 1, (float) (distance / dynamicRange));
		GlStateManager.popMatrix();
	}
	
	private static void drawPoint(int color, double scale, float closeness) {
		Tessellator tess = Tessellator.getInstance();
		BufferBuilder buff = tess.getBuffer();

		int r = color >> 16 & 255;
		int g = color >> 8 & 255;
		int b = color & 255;
		
		GlStateManager.color(r / 255f, g / 255f, b / 255f);
		
		if(closeness < 0.2) {
			closeness = 0.2F;
		} else if(closeness > 1) {
			closeness = 1;
		}
		double width = 1;
	
		double add = width * scale - width;
		
		buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
		
		buff.pos(-width / 2 - add, -width * 3 - add, 0).endVertex();
		buff.pos(-width / 2 - add, add, 0).endVertex();
		buff.pos(width / 2 + add, add, 0).endVertex();
		buff.pos(width / 2 + add, -width * 3 - add, 0).endVertex();
		
		buff.pos(-width / 2 - add, width * 2 - add, 0).endVertex();
		buff.pos(-width / 2 - add, width * 3 + add, 0).endVertex();
		buff.pos(width / 2 + add, width * 3 + add, 0).endVertex();
		buff.pos(width / 2 + add, width * 2 - add, 0).endVertex();

		tess.draw();
	}

	private void initializeGl(Entity renderer, Vec3d pos, boolean staticSize) { 
		initializeGl(renderer, pos, staticSize, true); 
	}
	
	private void initializeGl(Entity renderer, Vec3d pos, boolean staticSize, boolean rotated) {
		
		Vec3d render = renderer.getPositionVector().add(
				EntityUtils.getInterpolatedAmount(renderer, MC.getRenderPartialTicks()));
		
		double x = pos.x - render.x;
		double y = pos.y - render.y;
		double z = pos.z - render.z;
		
		double distance = render.distanceTo(pos);

		double maxDistance = MC.gameSettings.getOptionFloatValue(GameSettings.Options.RENDER_DISTANCE) * 12D;
		
		//As you get close, increase the size up to a certain point
		float size = 1;
		
		if(staticSize) {
			if(distance <= maxDistance) {
				size = (float) (1 - (maxDistance - distance) / maxDistance);
			}
		} else {
			double dynamicDistance = dynamicRange - distance;
			if(dynamicDistance < 0) {
				dynamicDistance = 0;
			}
			if(distance <= maxDistance) {
				float finalSize = 0.5F; //Based maths
				size = (float) (finalSize + 
						((dynamicDistance * 0.0003F + 0.5F - finalSize) / 
								(1 + Math.exp(0.05F * (maxDistance - distance - maxDistance / 2)))));
			} else {
				size = (float) dynamicDistance * 0.0003F + 0.5F;
			}
		}
		
		if (distance > maxDistance) {
			x = x / distance * maxDistance;
			y = y / distance * maxDistance;
			z = z / distance * maxDistance;
		}
		
		GlStateManager.translate(x + 0.5, y + 1.3, z + 0.5);
		GlStateManager.glNormal3f(0, 1, 0);
		GlStateManager.rotate(-MC.getRenderManager().playerViewY, 0, 1, 0);
		GlStateManager.rotate(MC.getRenderManager().playerViewX, 1, 0, 0);
    if (rotated) GL11.glRotated(180, 0, 0, 0);
    else GL11.glRotated(180, 0, 0, 1);
				
		GL11.glScalef(size, size, size);
		
		GlStateManager.disableLighting();
		GlStateManager.disableFog();
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
		
		GlStateManager.disableTexture2D();

		if(antialias.getAsBoolean()) {
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
		}

	}
	
	static class LocationInfoGui extends GuiScreen {
		
		private static final ResourceLocation ICON = new ResourceLocation("2b2tatlas","textures/2b2tatlas.png");
		private static final int iconHeight = 170;
		private static final int iconWidth = 512;
		
		private static final int scaledIconHeight = 42;
		private static final int scaledIconWidth = 128;
		
		private static final int guiheight = 200;
		private static final int guiwidth = 200;
		
		private static final int lines = 8;
		
		private List<String> desc;
		private List<ITextComponent> links;
		private float scrollf;
		private AtlasService.Location location;
		
		int factor;
		
		private int scrollbarHeight = 0;
		private int scrollbarWidth = 5;
		private boolean wasClicking;
		
		private int scrollStart;
		private int linkStart;
		
		private LocationInfoGui(AtlasService.Location l) {
			location = l;
		}
		
		@Override
		public void initGui() {	
			
			links = new ArrayList<>();
			
			factor = new ScaledResolution(MC).getScaleFactor();

		    ITextComponent info1 = new TextComponentString("These links are provided by 2b2t");
			ITextComponent info2 = new TextComponentString("Atlas; Click at your own risk.");
			ITextComponent wiki = new TextComponentString("Wiki Link");
			ITextComponent video = new TextComponentString("Video Link");
			
			//I know this is yucky but font renderers don't play well in constructors
			Style info1Style = new Style();
			//info1Style.setColor(TextFormatting.WHITE);
			info1.setStyle(info1Style);
			
			Style info2Style = new Style();
			//info2Style.setColor(TextFormatting.WHITE);
			info2.setStyle(info2Style);
			
			Style wikiStyle = new Style();
			wikiStyle.setClickEvent(
					new ClickEvent(
							ClickEvent.Action.OPEN_URL,
							location.getWikiAddress()));
			wikiStyle.setHoverEvent(
					new HoverEvent(
							HoverEvent.Action.SHOW_TEXT, 
							new TextComponentString(location.getWikiAddress())));
			wikiStyle.setBold(true);
			wiki.setStyle(wikiStyle);
			
			Style videoStyle = new Style();
			videoStyle.setClickEvent(
					new ClickEvent(
							ClickEvent.Action.OPEN_URL, 
							location.getVideoAddress()));
			videoStyle.setHoverEvent(
					new HoverEvent(
							HoverEvent.Action.SHOW_TEXT, 
							new TextComponentString(location.getVideoAddress())));
			videoStyle.setBold(true);
			video.setStyle(videoStyle);
			
			desc = fontRenderer.listFormattedStringToWidth(location.getDescription(), guiwidth - 20);
			
			if(!location.getWikiAddress().trim().isEmpty() || !location.getVideoAddress().trim().isEmpty()) {
				links.add(info1);
				links.add(info2);
				if(!location.getWikiAddress().trim().isEmpty()) {
					links.add(wiki);
				}
				if(!location.getVideoAddress().trim().isEmpty()) {
					links.add(video);
				}
			}
			
			scrollbarHeight = (int) (lines * fontRenderer.FONT_HEIGHT * (lines / Math.max((float)lines, desc.size())));
		    scrollbarHeight = MathHelper.clamp(scrollbarHeight, scrollbarWidth, lines * fontRenderer.FONT_HEIGHT);
		    scrollStart = (height / 2 - guiheight / 2 + scaledIconHeight + 3 * fontRenderer.FONT_HEIGHT + 10);
		    linkStart = scrollStart + (lines + 1) * fontRenderer.FONT_HEIGHT;
		    
		}
		
		@Override
		public void drawScreen(int mouseX, int mouseY, float ticks) {
			int i = width / 2;
			int j = height / 2;
			
			drawRect(i - guiwidth / 2, 
					j - guiheight / 2, 
					i + guiwidth / 2, 
					j + guiheight / 2, 
					Colors.BLACK.toBuffer());
			
			GlStateManager.color(1, 1, 1, 1);
			GlStateManager.disableLighting();
			GlStateManager.enableAlpha();
			GlStateManager.enableBlend();			
			MC.getTextureManager().bindTexture(ICON);
			Gui.drawScaledCustomSizeModalRect(
					i - scaledIconWidth / 2,
					j - guiheight / 2 + 5,
					0, 
					0,
					iconWidth, 
					iconHeight, 
					scaledIconWidth, 
					scaledIconHeight, 
					iconWidth, 
					iconHeight
					);
						
			fontRenderer.drawString(
					location.getName(),
					i - fontRenderer.getStringWidth(location.getName()) / 2, 
					j - guiheight / 2 + scaledIconHeight + 10, 
					Colors.WHITE.toBuffer()
					);
						
			String coords = "X:" + location.getX() + " Z:" + location.getZ();
			
			fontRenderer.drawString(
					coords,
					i - fontRenderer.getStringWidth(coords) / 2, 
					j - guiheight / 2 + scaledIconHeight + fontRenderer.FONT_HEIGHT + 10, 
					Colors.WHITE.toBuffer()
					);
			
			boolean clickFlag = Mouse.isButtonDown(0);
			
			boolean inBounds = 
					mouseX > width / 2 + guiwidth / 2 - 3 - scrollbarWidth &&
					mouseX < width / 2 + guiwidth / 2 - 3 &&
					mouseY > scrollStart &&
					mouseY < scrollStart + lines * fontRenderer.FONT_HEIGHT;
			
			if(wasClicking && clickFlag) {
				scrollf = (mouseY - scrollStart) / (float)(lines * fontRenderer.FONT_HEIGHT);
				scrollf = MathHelper.clamp(scrollf, 0, 1);
			}
			
			wasClicking = wasClicking ? clickFlag : clickFlag && inBounds;
			
			int scrollAmt = (int) (scrollf * fontRenderer.FONT_HEIGHT * Math.min((-desc.size() + lines), 0));
						
			//Scrolling here
			GlStateManager.pushMatrix();
			GL11.glScissor(
					factor * (i - guiwidth / 2),
					MC.displayHeight - factor * (scrollStart + fontRenderer.FONT_HEIGHT * lines), 
					factor * (guiwidth + 10), 
					factor * fontRenderer.FONT_HEIGHT * lines
					);
		    GL11.glEnable(GL11.GL_SCISSOR_TEST);
		    						
			for(int index = 0; index < desc.size(); index++) {
				fontRenderer.drawString(desc.get(index), 
						i - fontRenderer.getStringWidth(desc.get(index)) / 2 - 5, 
						scrollStart + scrollAmt + fontRenderer.FONT_HEIGHT * index, 
						Colors.WHITE.toBuffer());
			}
		    GL11.glDisable(GL11.GL_SCISSOR_TEST);
		    GlStateManager.popMatrix();
		    			
		    int barscroll = (int) (scrollf * (lines * fontRenderer.FONT_HEIGHT - scrollbarHeight));
		    
		    drawRect(
		    		i + guiwidth / 2 - 3 - scrollbarWidth,
		    		scrollStart,
		    		i + guiwidth / 2 - 3,
		    		scrollStart + lines * fontRenderer.FONT_HEIGHT,
		    		Colors.GRAY.toBuffer()
		    		);
		    
		    drawRect(
		    		i + guiwidth / 2 - 3 - scrollbarWidth,
		    		scrollStart + barscroll,
		    		i + guiwidth / 2 - 3,
		    		scrollStart + barscroll + scrollbarHeight,
		    		Colors.WHITE.toBuffer()
		    		);
		    
		    //Draw link text
		    for(int index = 0; index < links.size(); index++) {
		    	int color = Colors.WHITE.toBuffer();
		    	ITextComponent comp = links.get(index);
		    	int stringWidth = fontRenderer.getStringWidth(comp.getFormattedText());
		    	boolean hovered =
		    			mouseX > i - stringWidth / 2 &&
		    			mouseX < i - stringWidth / 2 + stringWidth &&
		    			mouseY > linkStart + index * fontRenderer.FONT_HEIGHT &&
		    			mouseY < linkStart + (index + 1) * fontRenderer.FONT_HEIGHT;
		    	
		    	if(index > 1 && hovered) {
		    		color = Colors.YELLOW.toBuffer();
		    	}
		    	
		    	fontRenderer.drawString(
		    			comp.getFormattedText(),
		    			i - stringWidth / 2,
		    			linkStart + index * fontRenderer.FONT_HEIGHT,
		    			color
		    			);
		    }
		    
		    //Handle link hover/click
		    for(int index = 2; index < links.size(); index++) {
		    	ITextComponent comp = links.get(index);
		    	int stringWidth = fontRenderer.getStringWidth(comp.getFormattedText());
		    	boolean hovered =
		    			mouseX > i - stringWidth / 2 &&
		    			mouseX < i - stringWidth / 2 + stringWidth &&
		    			mouseY > linkStart + index * fontRenderer.FONT_HEIGHT &&
		    			mouseY < linkStart + (index + 1) * fontRenderer.FONT_HEIGHT;
		    	
		    	if(hovered) {
		    		handleComponentHover(comp, mouseX, mouseY);
		    		if(Mouse.isButtonDown(0)) {
		    			handleComponentClick(comp);
		    		}
		    	}	
		    }
		}
		
		@Override
		public boolean doesGuiPauseGame() {
		    return false;
		}
		
		@Override
		public void handleMouseInput() throws IOException {
			super.handleMouseInput();
			int i = Mouse.getDWheel();
			if(i != 0) {
				i = MathHelper.clamp(i, -1, 1);
				scrollf = MathHelper.clamp(
						scrollf + -i * 1f / desc.size(),
						0,
						1
						);
			}
		}
		
	}

}
