package com.matt.forgehax.mods;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.mods.managers.FriendManager;
import com.matt.forgehax.mods.services.RainbowService;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import java.util.HashMap;


//Ez jomfru/rusherhack skid
//(The code is actually kind of really bad so I'll have to clean it up later)
@RegisterMod
public class Nametags extends ToggleMod {
  public Nametags() {
    super(Category.RENDER, "Nametags", false, "Renders Nametags above players");
  }
  
  private final Setting<Color> backgroundColor =
		  getCommandStub()
		  	.builders()
		  	.newSettingColorBuilder()
		  	.name("backgroundColor")
		  	.description("The color of the background")
		  	.defaultTo(Colors.BLACK)
		  	.build();
  
  private final Setting<Integer> alpha =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("alpha")
          .description("alpha of background")
          .defaultTo(50)
          .min(0)
          .max(255)
          .build();
  
  private final Setting<Integer> alphaborder =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("alphaborder")
          .description("alpha of border")
          .defaultTo(100)
          .min(0)
          .max(255)
          .build();
  
  private final Setting<Double> widthborder =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("widthborder")
          .description("width of border")
          .defaultTo(0.5)
          .min(0.0)
          .max(5.0)
          .build();
  
  private final Setting<Boolean> ping =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("ping")
          .description("show players ping on nametag")
          .defaultTo(false)
          .build();
  
  private final Setting<Boolean> enchants =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("enchants")
          .description("show enchants on armor/tools")
          .defaultTo(true)
          .build();
  
  private final Setting<Boolean> items =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("items")
          .description("show items held and armor")
          .defaultTo(true)
          .build();
  
  private final Setting<Boolean> damagedisplay =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("damage")
          .description("shows the damage that players take")
          .defaultTo(true)
          .build();     
  
  private final Setting<Boolean> atheist =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("atheist")
          .description("be a cringe atheist and remove cross from friends")
          .defaultTo(false)
          .build();
  
  private final Setting<Float> scale =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("scale")
          .description("scale of border")
          .defaultTo(1F)
          .min(0F)
          .max(10F)
          .build();  
  
  private final Setting<BorderType> bordermode =
      getCommandStub()
          .builders()
          .<BorderType>newSettingEnumBuilder()
          .name("bordermode")
          .description("solid or rainbow or seizure")
          .defaultTo(BorderType.SOLID)
          .build();        

  int counter1 = 0;
  int counter2 = 0;
  int color1 = 0;  
  private HashMap<String, Integer> tagList = new HashMap<>();
  private HashMap<String, String> damageList = new HashMap<>();
          
  @SubscribeEvent
  public void onRenderPlayerNameTag(RenderLivingEvent.Specials.Pre<?> event) {
    if (EntityUtils.isPlayer(event.getEntity())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void onRender(RenderEvent event) {
    for (EntityPlayer p : MC.world.playerEntities) {
      if ((p != MC.getRenderViewEntity()) && (p.isEntityAlive())) {
      	if(damagedisplay.get()) {
      		if (!tagList.containsKey(p.getName())) {
				tagList.put(p.getName(), (int)p.getHealth());
				damageList.put(p.getName(), "");
			}
			if(p.isDead || p.getHealth() <= 0.0f) {
				tagList.remove(p.getName());  
				damageList.remove(p.getName());
			}
		}
      	
      	double pX = p.lastTickPosX + (p.posX - p.lastTickPosX) * event.getPartialTicks();
        double pY = p.lastTickPosY + (p.posY - p.lastTickPosY) * event.getPartialTicks();
        double pZ = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * event.getPartialTicks();
        
        Entity renderEntity = MC.getRenderManager().renderViewEntity;
        
        if(renderEntity == null) {
        	renderEntity = MC.player;
        }
        
        if(renderEntity == null) {
        	return;
        }
        
        double rX = renderEntity.lastTickPosX + (renderEntity.posX - renderEntity.lastTickPosX) * event.getPartialTicks();
        double rY = renderEntity.lastTickPosY + (renderEntity.posY - renderEntity.lastTickPosY) * event.getPartialTicks();
        double rZ = renderEntity.lastTickPosZ + (renderEntity.posZ - renderEntity.lastTickPosZ) * event.getPartialTicks();

        renderNametag(p, pX - rX, pY - rY, pZ - rZ);
    
      }
      if(counter2 == 601 && damagedisplay.get()) {
      	  tagList.remove(p.getName());
      	  damageList.remove(p.getName());
      }
    }
    if(counter1 == 361)
  		counter1 = 0;
  	if(counter2 == 601)
  		counter2 = 0;
  	counter1++;
  	counter2++;
  	
  	switch(bordermode.get()) {
  	
  	case RAINBOW:
  		color1 = RainbowService.getRainbowColor();
  		break;
  	case SEIZURE:
  		color1 = Color.of((int) (Math.random() * 255), 
  				(int) (Math.random() * 255), 
  				(int) (Math.random() * 255), 
  				(int) 255).toBuffer();
  		break;
  	case SOLID:
  		color1 = Color.of(backgroundColor.get().getRed(), 
  				backgroundColor.get().getGreen(), 
  				backgroundColor.get().getBlue(), 
  				alphaborder.get()).toBuffer();
  		break;
  	}
  
  }

  public void renderNametag(EntityPlayer player, double x, double y, double z) {  //warning extreme poz ahead
    GL11.glPushMatrix();
    FontRenderer var13 = MC.fontRenderer;
    TextFormatting clr = TextFormatting.WHITE;
    TextFormatting clrf = TextFormatting.WHITE;
    String cross = "";
    if(FriendManager.isFriend(player.getName())) {
    	clrf = TextFormatting.AQUA;
    	if(!atheist.get())
    		cross = "\u271D ";
    }
    int pingy = -1;
    try {
    	pingy = MC.getConnection().getPlayerInfo(player.getUniqueID()).getResponseTime();
  	} catch (NullPointerException np) {}
    String playerPing = String.valueOf(pingy) + "ms ";
    if (!ping.get()) playerPing = "";
    int health = MathHelper.ceil(player.getHealth() + player.getAbsorptionAmount());
    if (health > 16) clr = TextFormatting.GREEN;
    else if (health > 12) clr = TextFormatting.YELLOW;
    else if (health > 8) clr = TextFormatting.GOLD;
    else if (health > 5) clr = TextFormatting.RED;
    else if (health <= 5) clr = TextFormatting.DARK_RED;
    int lasthealth = tagList.get(player.getName()).intValue();
    if (player != MC.player && damagedisplay.get()) {
		if(lasthealth > health) {
			damageList.put(player.getName(), TextFormatting.RED + " -" + (lasthealth - health));
		}
		tagList.put(player.getName(), health);
	}
	String dmgtext = "";
	if(damagedisplay.get())
		dmgtext = damageList.get(player.getName());
    String name = cross + clrf + player.getName() + " " + playerPing + clr + health + dmgtext;
    name = name.replace(".0", "");
    float var14 = 0.016666668F * getNametagSize(player);
    GL11.glTranslated(x, y + 2.5 + var14 * 10, z);
    GL11.glNormal3f(0.0F, 1.0F, 0.0F);
    GL11.glRotatef(-MC.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
    GL11.glRotatef(MC.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
    GL11.glScalef(-var14, -var14, var14);
    GlStateManager.disableLighting();
    GlStateManager.depthMask(false);
    GL11.glDisable(2929);
    int width = var13.getStringWidth(name) / 2;
    drawBorderedRect(-width - 2, 10, width + 2, 20, widthborder.get(), 
    		Color.of(backgroundColor.get().getRed(), 
    				backgroundColor.get().getGreen(), 
    				backgroundColor.get().getBlue(), 
    				alpha.get()).toBuffer(), color1);
    MC.fontRenderer.drawString(name, -width, 11, -1);
    if(items.get()) {
      int xOffset = -8;
      for (ItemStack armourStack : player.inventory.armorInventory) {
        if (armourStack != null) {
          xOffset -= 8;
        }
      }

      if (player.getHeldItemMainhand() != null) {
        xOffset -= 8;
        ItemStack renderStack = player.getHeldItemMainhand().copy();
        renderItem(player, renderStack, xOffset, -10);
        xOffset += 16;
      }
      for (int index = 3; index >= 0; --index) {
        ItemStack armourStack = player.inventory.armorInventory.get(index);
        if (armourStack != null) {
          ItemStack renderStack1 = armourStack.copy();
          renderItem(player, renderStack1, xOffset, -10);
          xOffset += 16;
        }
      }

      Object renderOffhand;
      if (player.getHeldItemOffhand() != null) {
        xOffset -= 0;
        renderOffhand = player.getHeldItemOffhand().copy();
        renderItem(player, (ItemStack) renderOffhand, xOffset, -10);
        xOffset += 8;
      }
    }
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GL11.glEnable(GL11.GL_DEPTH_TEST);
    GL11.glDepthMask(true);
    GL11.glDisable(GL11.GL_BLEND);
    GL11.glPopMatrix();
    GL11.glColor4f(1f, 1f, 1f, 1f);
  }

  public float getNametagSize(EntityLivingBase player) {
    ScaledResolution scaledRes = new ScaledResolution(MC);
    double twoDscale = scaledRes.getScaleFactor() / Math.pow(scaledRes.getScaleFactor(), 2.0D);
    return scale.get() * ((float) twoDscale + (float)(player.getDistance(MC.getRenderViewEntity().posX, MC.getRenderViewEntity().posY, MC.getRenderViewEntity().posZ) / (0.7f * 10)));
  }

  public void drawBorderRect(float left, float top, float right, float bottom, int bcolor, int icolor, float f) {
    drawGuiRect(left + f, top + f, right - f, bottom - f, icolor);
    drawGuiRect(left, top, left + f, bottom, bcolor);
    drawGuiRect(left + f, top, right, top + f, bcolor);
    drawGuiRect(left + f, bottom - f, right, bottom, bcolor);
    drawGuiRect(right - f, top + f, right, bottom - f, bcolor);
  }

  public static void drawBorderedRect(double x, double y, double x1, double y1, double width, int internalColor, int borderColor) {
    enableGL2D();
    fakeGuiRect(x + width, y + width, x1 - width, y1 - width, internalColor);
    fakeGuiRect(x + width, y, x1 - width, y + width, borderColor);
    fakeGuiRect(x, y, x + width, y1, borderColor);
    fakeGuiRect(x1 - width, y, x1, y1, borderColor);
    fakeGuiRect(x + width, y1 - width, x1 - width, y1, borderColor);
    disableGL2D();
  }

  public void renderItem(EntityPlayer player, ItemStack stack, int x, int y) {
    GL11.glPushMatrix();
    GL11.glDepthMask(true);
    GlStateManager.clear(256);
    GlStateManager.disableDepth();
    GlStateManager.enableDepth();
    net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
    MC.getRenderItem().zLevel = -100.0F;
    GlStateManager.scale(1, 1, 0.01f);
    MC.getRenderItem().renderItemAndEffectIntoGUI(stack, x, (y / 2) - 12);
    MC.getRenderItem().renderItemOverlays(MC.fontRenderer, stack, x, (y / 2) - 12);
    MC.getRenderItem().zLevel = 0.0F;
    GlStateManager.scale(1, 1, 1);
    net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
    GlStateManager.enableAlpha();
    GlStateManager.disableBlend();
    GlStateManager.disableLighting();
    GlStateManager.scale(0.5D, 0.5D, 0.5D);
    GlStateManager.disableDepth();
    if(enchants.get()) renderEnchantText(player, stack, x, y - 18);
    GlStateManager.enableDepth();
    GlStateManager.scale(2.0F, 2.0F, 2.0F);
    GL11.glPopMatrix();
  }

  public void renderEnchantText(EntityPlayer player, ItemStack stack, int x, int y) {
    int encY = y + 10; //24
    int yCount = encY - -5;
    if (stack.getItem() instanceof ItemArmor || stack.getItem() instanceof ItemSword
        || stack.getItem() instanceof ItemTool) {
    	float dmgpercent = ((float)(stack.getMaxDamage() - (float)(stack.getItemDamage())) / stack.getMaxDamage()) * 100;
    	TextFormatting clr = TextFormatting.WHITE;
    	if (dmgpercent > 85) clr = TextFormatting.DARK_GREEN;
    	else if (dmgpercent > 70) clr = TextFormatting.GREEN;
    	else if (dmgpercent > 50) clr = TextFormatting.YELLOW;
    	else if (dmgpercent > 20) clr = TextFormatting.GOLD;
    	else if (dmgpercent > 10) clr = TextFormatting.RED;
    	else if (dmgpercent <= 10) clr = TextFormatting.DARK_RED;
    	GL11.glScalef(1f, 1f, 0);
        MC.fontRenderer.drawStringWithShadow(clr + "" + Math.round(dmgpercent) + "%", x * 2 + 6, y + 26, -1);
        GL11.glScalef(1f, 1f, 1);
    }
    NBTTagList enchants = stack.getEnchantmentTagList();
    if (enchants != null) {
      for (int index = 0; index < enchants.tagCount(); ++index) {
        short id = enchants.getCompoundTagAt(index).getShort("id");
        short level = enchants.getCompoundTagAt(index).getShort("lvl");
        Enchantment enc = Enchantment.getEnchantmentByID(id);
        if (enc != null) {
          String encName = enc.isCurse()
              ? TextFormatting.RED
              + enc.getTranslatedName(level).substring(11).substring(0, 1).toLowerCase()
              : enc.getTranslatedName(level).substring(0, 1).toLowerCase();
          encName = encName + level;
          GL11.glPushMatrix();
          GL11.glScalef(1f, 1f, 0);
          MC.fontRenderer.drawStringWithShadow(encName, x * 2 + 6, yCount, -1); //13
          GL11.glScalef(1f, 1f, 1);
          GL11.glPopMatrix();
          encY += 8;
          yCount -= 10;
        }
      }
    }
  }

  public static void enableGL2D() {
    GL11.glDisable(2929);
    GL11.glEnable(3042);
    GL11.glDisable(3553);
    GL11.glBlendFunc(770, 771);
    GL11.glDepthMask(true);
    GL11.glEnable(2848);
    GL11.glHint(3154, 4354);
    GL11.glHint(3155, 4354);
  }

  public static void disableGL2D() {
    GL11.glEnable(3553);
    GL11.glDisable(3042);
    GL11.glDisable(2848);
    GL11.glHint(3154, 4352);
    GL11.glHint(3155, 4352);
  }

  public static void fakeGuiRect(double left, double top, double right, double bottom, int color) {
    if (left < right) {
      double i = left;
      left = right;
      right = i;
    }
    if (top < bottom) {
      double j = top;
      top = bottom;
      bottom = j;
    }
    float f3 = (float)(color >> 24 & 255) / 255.0F;
    float f = (float)(color >> 16 & 255) / 255.0F;
    float f1 = (float)(color >> 8 & 255) / 255.0F;
    float f2 = (float)(color & 255) / 255.0F;
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder bufferbuilder = tessellator.getBuffer();
    GlStateManager.enableBlend();
    GlStateManager.disableTexture2D();
    GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    GlStateManager.color(f, f1, f2, f3);
    bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
    bufferbuilder.pos(left, bottom, 0.0D).endVertex();
    bufferbuilder.pos(right, bottom, 0.0D).endVertex();
    bufferbuilder.pos(right, top, 0.0D).endVertex();
    bufferbuilder.pos(left, top, 0.0D).endVertex();
    tessellator.draw();
    GlStateManager.enableTexture2D();
    GlStateManager.disableBlend();
  }

  public static void drawGuiRect(double x1, double y1, double x2, double y2, int color) {
    float red = (color>> 24 & 0xFF) / 255.0F;
    float green = (color>> 16 & 0xFF) / 255.0F;
    float blue = (color>> 8 & 0xFF) / 255.0F;
    float alpha = (color& 0xFF) / 255.0F;
    GL11.glEnable(3042);
    GL11.glDisable(3553);
    GL11.glBlendFunc(770, 771);
    GL11.glEnable(2848);
    GL11.glPushMatrix();
    GL11.glColor4f(green, blue, alpha, red);
    GL11.glBegin(7);
    GL11.glVertex2d(x2, y1);
    GL11.glVertex2d(x1, y1);
    GL11.glVertex2d(x1, y2);
    GL11.glVertex2d(x2, y2);
    GL11.glEnd();
    GL11.glPopMatrix();
    GL11.glEnable(3553);
    GL11.glDisable(3042);
    GL11.glDisable(2848);
  }
  
  enum BorderType {
	  SOLID, RAINBOW, SEIZURE;
  }

}