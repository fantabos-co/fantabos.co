package com.matt.forgehax.mods;

import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.events.LoadShaderEvent;
import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.matt.forgehax.Helper.*;

/**
 * Added by OverFloyd
 * aug 19, 2020
 * Fuck Mojang and their no-fun-allowed shit
 */
@RegisterMod
public class SuperSecretSettings extends ToggleMod {

  public final Setting<Boolean> optionsButton =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("options-button")
      .description("Enables the original button in GuiOptions.")
      .defaultTo(true)
      .build();

  public final Setting<Shaders> shaderID =
    getCommandStub()
      .builders()
      .<Shaders>newSettingEnumBuilder()
      .name("shader")
      .description("The shader to be used.")
      .defaultTo(Shaders.SOBEL)
      .build();

  public static SuperSecretSettings INSTANCE;

  public SuperSecretSettings() {
    super(Category.RENDER, "SuperSecretSettings", false, "Time travel back to 2014.");
    INSTANCE = this;
  }

  /**
   * If the shader has been set correctly
   */
  boolean isSet;

  /**
   * Used to compare shaderID after it changed to allow shader updating
   */
  int oldIndex;

  /**
   * Used to save the original value for shaderIndex
   * Used onDisabled()
   */
  int originalIndex;

  /**
   * Super cool button in GuiOptions
   */
  GuiButton superSecretButton;

  /**
   * Current active shader
   */
  Shaders active;

  @Override
  public void onEnabled() {
    setShader();
    originalIndex = FastReflection.Fields.EntityRenderer_shaderIndex.get(MC.entityRenderer);
    isSet = true;
  }

  // Puts back whatever the default shader for viewEntity is
  @Override
  public void onDisabled() {
    // Fixes "No OpenGL context found in the current thread"
    MC.addScheduledTask(() -> MC.entityRenderer.loadEntityShader(getRenderEntity()));

    FastReflection.Fields.EntityRenderer_shaderIndex.set(MC.entityRenderer, originalIndex);
    isSet = false;
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if (getLocalPlayer() == null) {
      isSet = false; // If you join a new world the shader will be put on join
      return;
    }

    if (!isSet || oldIndex != shaderID.get().id) {
      setShader();

      oldIndex = FastReflection.Fields.EntityRenderer_shaderIndex.get(MC.entityRenderer);
      isSet = true;
    }
  }

  // Cancels Minecraft's bullshit when you change camera view or viewEntity
  @SubscribeEvent
  public void onLoadShaderEvent(LoadShaderEvent event) {
    if (shaderID.get().id != 24) {
      event.setCanceled(true); // always cancel except in default mode
    }
  }

  /**
   * For super epic button in GuiOption
   */
  @SubscribeEvent
  public void onScreenUpdated(GuiScreenEvent.InitGuiEvent.Post event) {
    if (getWorld() == null || !optionsButton.get()) return;

    if (event.getGui() instanceof GuiOptions) {
      GuiOptions gui = (GuiOptions) event.getGui();

      event
        .getButtonList()
        .add(
          superSecretButton =
            new GuiButton(
              420,
              gui.width / 2 + 5,
              gui.height / 6 + 144 - 6, 150, 20,
              "Super Secret Settings...") {
              public void playPressSound(SoundHandler soundHandlerIn) { // plays a random sound
                String[] allowedPrefixes = {"block", "entity", "ambient", "item"}; // ambient and item aren't original but who tf cares

                List<ResourceLocation> soundList = SoundEvent.REGISTRY.getKeys().stream().filter(
                  resource -> StringUtils.startsWithAny(resource.getResourcePath(), allowedPrefixes)).collect(Collectors.toList());

                SoundEvent soundEvent = null;
                if (!soundList.isEmpty()) {
                  soundEvent = SoundEvent.REGISTRY.getObject(soundList.get(new Random().nextInt(soundList.size())));
                }

                if (soundEvent != null) {
                  soundHandlerIn.playSound(PositionedSoundRecord.getMasterRecord(soundEvent, 0.5F));
                }

                setShader();
              }
            });
    }
  }

  @SubscribeEvent
  public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent event) {
    if (event.getButton() == superSecretButton && getCurrentScreen() instanceof GuiOptions) {
      active = shaderID.get();

      if (active.ordinal() == Shaders.values().length - 1) {
        shaderID.set(Shaders.values()[0], false);
      } else {
        shaderID.set(Shaders.values()[active.ordinal() + 1], false);
      }

      active = shaderID.get();
    }
  }

  public void setShader() {
    FastReflection.Fields.EntityRenderer_shaderIndex.set(MC.entityRenderer, shaderID.get().id);

    if (shaderID.get().id < EntityRenderer.SHADER_COUNT) {
      MC.entityRenderer.loadShader(FastReflection.Fields.EntityRenderer_SHADERS_TEXTURES.get(MC)[shaderID.get().id]);
    } else {
      MC.entityRenderer.loadEntityShader(getRenderEntity()); // If shaderID.get().id > SHADER_COUNT uses default mode
    }
  }

  private enum Shaders {
    NOTCH(0),
    FXAA(1),
    ART(2),
    BUMPY(3),
    BLOBS2(4),
    PENCIL(5),
    COLOR_CONVOLVE(6),
    DECONVERGE(7),
    FLIP(8),
    INVERT(9),
    NTSC(10),
    OUTLINE(11),
    PHOSPHOR(12),
    SCAN_PINCUSHION(13),
    SOBEL(14),
    BITS(15),
    DESATURATE(16),
    GREEN(17),
    BLUR(18),
    WOBBLE(19),
    BLOBS(20),
    ANTIALIAS(21),
    CREEPER(22),
    SPIDER(23),
    DEFAULT(24); // Passive mode

    public final int id;

    Shaders(final int shaderID) {
      this.id = shaderID;
    }
  }
}
