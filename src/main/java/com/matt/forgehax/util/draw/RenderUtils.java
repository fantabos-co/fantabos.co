package com.matt.forgehax.util.draw;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.Globals;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.entity.EntityUtils;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

public class RenderUtils implements Globals {
  
  public static Vec3d getRenderPos() {
    return new Vec3d(
        MC.player.lastTickPosX
            + (MC.player.posX - MC.player.lastTickPosX) * MC.getRenderPartialTicks(),
        MC.player.lastTickPosY
            + (MC.player.posY - MC.player.lastTickPosY) * MC.getRenderPartialTicks(),
        MC.player.lastTickPosZ
            + (MC.player.posZ - MC.player.lastTickPosZ) * MC.getRenderPartialTicks());
  }
  
  public static void drawLine(
      Vec3d startPos, Vec3d endPos, int color, boolean smooth, float width) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder BufferBuilder = tessellator.getBuffer();
    
    Vec3d endVecPos = endPos.subtract(startPos);
    
    float r = (float) (color >> 16 & 255) / 255.0F;
    float g = (float) (color >> 8 & 255) / 255.0F;
    float b = (float) (color & 255) / 255.0F;
    float a = (float) (color >> 24 & 255) / 255.0F;
    
    if (smooth) {
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
    }
    
    GL11.glLineWidth(width);
    
    GlStateManager.pushMatrix();
    GlStateManager.translate(startPos.x, startPos.y, startPos.z);
    GlStateManager.disableTexture2D();
    GlStateManager.enableBlend();
    GlStateManager.disableAlpha();
    GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
    GlStateManager.shadeModel(GL11.GL_SMOOTH);
    
    BufferBuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
    BufferBuilder.pos(0, 0, 0).color(r, g, b, a).endVertex();
    BufferBuilder.pos(endVecPos.x, endVecPos.y, endVecPos.z).color(r, g, b, a).endVertex();
    tessellator.draw();
    
    if (smooth) {
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }
    
    GlStateManager.shadeModel(GL11.GL_FLAT);
    GlStateManager.disableBlend();
    GlStateManager.enableAlpha();
    GlStateManager.enableTexture2D();
    GlStateManager.enableDepth();
    GlStateManager.enableCull();
    GlStateManager.popMatrix();
  }
  
  // thanks again Gregor
  public static void drawBox(
      Vec3d startPos, Vec3d endPos, int color, float width, boolean ignoreZ) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder buffer = tessellator.getBuffer();
    
    Vec3d renderPos = EntityUtils.getInterpolatedPos(getLocalPlayer(), MC.getRenderPartialTicks());
    
    Vec3d min = startPos.subtract(renderPos);
    Vec3d max = endPos.subtract(renderPos);
    
    double minX = min.x, minY = min.y, minZ = min.z;
    double maxX = max.x, maxY = max.y, maxZ = max.z;
    
    float r = (float) (color >> 16 & 255) / 255.0F;
    float g = (float) (color >> 8 & 255) / 255.0F;
    float b = (float) (color & 255) / 255.0F;
    float a = (float) (color >> 24 & 255) / 255.0F;
    
    GlStateManager.pushMatrix();
    GlStateManager.disableTexture2D();
    GlStateManager.enableBlend();
    GlStateManager.disableAlpha();
    GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
    GlStateManager.shadeModel(GL11.GL_SMOOTH);
    GlStateManager.glLineWidth(width);
    
    if (ignoreZ) {
      GlStateManager.disableDepth();
    }
    
    GlStateManager.color(r, g, b, a);
    
    // GlStateManager.translate(startPos.xCoord, startPos.yCoord, startPos.zCoord);
    
    buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
    buffer.pos(minX, minY, minZ).endVertex();
    buffer.pos(maxX, minY, minZ).endVertex();
    buffer.pos(maxX, minY, maxZ).endVertex();
    buffer.pos(minX, minY, maxZ).endVertex();
    buffer.pos(minX, minY, minZ).endVertex();
    tessellator.draw();
    buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
    buffer.pos(minX, maxY, minZ).endVertex();
    buffer.pos(maxX, maxY, minZ).endVertex();
    buffer.pos(maxX, maxY, maxZ).endVertex();
    buffer.pos(minX, maxY, maxZ).endVertex();
    buffer.pos(minX, maxY, minZ).endVertex();
    tessellator.draw();
    buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
    buffer.pos(minX, minY, minZ).endVertex();
    buffer.pos(minX, maxY, minZ).endVertex();
    buffer.pos(maxX, minY, minZ).endVertex();
    buffer.pos(maxX, maxY, minZ).endVertex();
    buffer.pos(maxX, minY, maxZ).endVertex();
    buffer.pos(maxX, maxY, maxZ).endVertex();
    buffer.pos(minX, minY, maxZ).endVertex();
    buffer.pos(minX, maxY, maxZ).endVertex();
    tessellator.draw();
    
    GlStateManager.shadeModel(GL11.GL_FLAT);
    GlStateManager.disableBlend();
    GlStateManager.enableAlpha();
    GlStateManager.enableTexture2D();
    GlStateManager.enableDepth();
    GlStateManager.enableCull();
    GlStateManager.popMatrix();
  }
  
  public static void drawBox(
      BlockPos startPos, BlockPos endPos, int color, float width, boolean ignoreZ) {
    drawBox(
        new Vec3d(startPos.getX(), startPos.getY(), startPos.getZ()),
        new Vec3d(endPos.getX(), endPos.getY(), endPos.getZ()),
        color,
        width,
        ignoreZ);
  }
  
  //The following is taken from Phobos and it is sexy -TheAlphaEpsilon
  //TODO: Replace numerical values with GL11 literals
  public static void checkSetupFBO() {
      Framebuffer fbo = MC.getFramebuffer();
      if (fbo == null) return;
      if (fbo.depthBuffer <= -1) return;
      setupFBO(fbo);
      fbo.depthBuffer = -1;
  }

  private static void setupFBO(Framebuffer fbo) {
      EXTFramebufferObject.glDeleteRenderbuffersEXT(fbo.depthBuffer);
      int stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT();
      EXTFramebufferObject.glBindRenderbufferEXT(36161, stencilDepthBufferID);
      EXTFramebufferObject.glRenderbufferStorageEXT(36161, 34041, MC.displayWidth, MC.displayHeight);
      EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36128, 36161, stencilDepthBufferID);
      EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36096, 36161, stencilDepthBufferID);
  }
  
  public static void renderOne(float lineWidth) {
      checkSetupFBO();
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
      GL11.glDisable(GL11.GL_ALPHA_TEST);
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      GL11.glDisable(GL11.GL_LIGHTING);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glLineWidth(lineWidth);
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
      GL11.glEnable(GL11.GL_STENCIL_TEST);
      GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
      GL11.glClearStencil(15);
      GL11.glStencilFunc(GL11.GL_NEVER, GL11.GL_CURRENT_BIT, 15);
      GL11.glStencilOp(GL11.GL_REPLACE, GL11.GL_REPLACE, GL11.GL_REPLACE);
      GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
  }

  public static void renderTwo() {
      GL11.glStencilFunc(512, 0, 15);
      GL11.glStencilOp(7681, 7681, 7681);
      GL11.glPolygonMode(1032, 6914);
  }

  public static void renderThree() {
      GL11.glStencilFunc(514, 1, 15);
      GL11.glStencilOp(7680, 7680, 7680);
      GL11.glPolygonMode(1032, 6913);
  }

  public static void renderFour(Color color) {
      setColor(color);
      GL11.glDepthMask(false);
      GL11.glDisable(2929);
      GL11.glEnable(10754);
      GL11.glPolygonOffset(1.0f, -2000000.0f);
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0f, 240.0f);
  }

  public static void renderFive() {
      GL11.glPolygonOffset(1.0f, 2000000.0f);
      GL11.glDisable(10754);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(2960);
      GL11.glDisable(2848);
      GL11.glHint(3154, 4352);
      GL11.glEnable(3042);
      GL11.glEnable(2896);
      GL11.glEnable(3553);
      GL11.glEnable(3008);
      GL11.glPopAttrib();
  }
  
  public static void setColor(Color color) {
      GL11.glColor4d((double)((double)color.getRed() / 255.0), (double)((double)color.getGreen() / 255.0), (double)((double)color.getBlue() / 255.0), (double)((double)color.getAlpha() / 255.0));
  }
  
}
