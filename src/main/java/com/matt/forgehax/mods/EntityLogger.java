package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.google.common.collect.Sets;
import com.matt.forgehax.Helper;
import com.matt.forgehax.util.command.Options;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.command.exception.CommandExecuteException;
import com.matt.forgehax.util.entry.ClassEntry;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;
import joptsimple.internal.Strings;
import net.minecraft.entity.Entity;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Created on 10/12/2017 by fr1kin
 * Adapted by Tonio for entities
 */
@RegisterMod
public class EntityLogger extends ToggleMod {

  private final Setting<Boolean> everything =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("everything")
          .description("Warn about any entity entering render distance")
          .defaultTo(false)
          .build();

  private final Setting<Boolean> relative_coords =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("relative-coords")
          .description("Embed relative coords in click events." +
                "They will only work when you first receive the" +
                "notification but won't leak your base :)")
          .defaultTo(true)
          .build();
  
  private final Options<ClassEntry> whitelist =
      getCommandStub()
          .builders()
          .<ClassEntry>newOptionsBuilder()
          .name("whitelist")
          .description("Classes to warn on entity spawn")
          .factory(ClassEntry::new)
          .supplier(Sets::newConcurrentHashSet)
          .build();
  
  public EntityLogger() {
    super(Category.WORLD, "EntityLogger", false, "Warn when certain entities come into render distance");
  }

  private int count = 0;

  @Override
  public String getDisplayText() {
    return (getModName() + String.format(" [" + TextFormatting.GREEN + "%d" + TextFormatting.RESET +"]", count));
  }

  @Override
  protected void onLoad() {
    whitelist
        .builders()
        .newCommandBuilder()
        .name("add")
        .description("Add class")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String className = data.getArgumentAsString(0).toLowerCase();
              
              if (Strings.isNullOrEmpty(className)) {
                throw new CommandExecuteException("Empty or null argument");
              }
              
              Optional<Class<?>> match =
                  getLoadedClasses(Launch.classLoader)
                      .stream()
                      .filter(Entity.class::isAssignableFrom)
                      .filter(clazz -> clazz.getCanonicalName().toLowerCase().contains(className))
                      .sorted(
                          (o1, o2) ->
                              String.CASE_INSENSITIVE_ORDER.compare(
                                  o1.getCanonicalName(), o2.getCanonicalName()))
                      .findFirst();
              
              if (match.isPresent()) {
                Class<?> clazz = match.get();
                whitelist.add(new ClassEntry(clazz));
                data.write(String.format("Added class \"%s\"", clazz.getName()));
                data.markSuccess();
              } else {
                data.write(
                    String.format("Could not find any class name matching \"%s\"", className));
                data.markFailed();
              }
            })
        // .success(cb -> whitelist.serialize())
        .build();
  
    whitelist
        .builders()
        .newCommandBuilder()
        .name("remove")
        .description("Remove class")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String className = data.getArgumentAsString(0).toLowerCase();
              
              if (Strings.isNullOrEmpty(className)) {
                throw new CommandExecuteException("Empty or null argument");
              }
              
              Optional<ClassEntry> match =
                  whitelist
                      .stream()
                      .filter(entry -> entry.getClassName().toLowerCase().contains(className.toLowerCase()))
                      .sorted(
                          (o1, o2) ->
                              String.CASE_INSENSITIVE_ORDER.compare(
                                  o1.getClassName(), o2.getClassName()))
                      .findFirst();
              
              if (match.isPresent()) {
                ClassEntry entry = match.get();
                if (whitelist.remove(entry)) {
                  data.write(String.format("Removed class \"%s\"", entry.getClassName()));
                  data.markSuccess();
                } else {
                  data.write(String.format("Could not remove class \"%s\"", entry.getClassName()));
                  data.markFailed();
                }
              } else {
                data.write(
                    String.format("Could not find any class name matching \"%s\"", className));
                data.markFailed();
              }
            })
        // .success(cb -> whitelist.serialize())
        .build();
    
    whitelist
        .builders()
        .newCommandBuilder()
        .name("list")
        .description("List current contents")
        .processor(
            data -> {
              StringBuilder builder = new StringBuilder();
              for (ClassEntry c : whitelist.contents()) {
                builder.append(c.getClassName() + "\n");
              }
              data.write(builder.toString());
              data.markSuccess();
            })
        .build();
  }
  
  @Override
  protected void onEnabled() {
    count = 0;
  }
  
  @Override
  protected void onDisabled() {
  }

  private String getGoToCommand(Entity in) {
    if (relative_coords.get()) {
      return String.format("#goto ~%+.0f ~%+.0f",
             in.posX - getLocalPlayer().posX, in.posZ - getLocalPlayer().posZ);
    } else {
      return String.format("#goto %.0f %.0f %.0f", in.posX, in.posY, in.posZ);
    }
  }

  private ITextComponent fancyEntityName(Entity in) {
    return Helper.getFormattedText("found entity ", TextFormatting.DARK_GRAY, false, false)
            .appendSibling(
              Helper.getFormattedText(in.getName(), TextFormatting.GRAY, true, false,
                    new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, getGoToCommand(in)),
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(
                      TextFormatting.BOLD + String.format("X %.1f Y %.0f Z %.1f\n", in.posX, in.posY, in.posZ) +
                      TextFormatting.RESET + TextFormatting.DARK_GRAY + in.getClass().toString())))
            );
  }

  @SubscribeEvent
  public void onEntityJoinWorld(EntityJoinWorldEvent event) {
    if (everything.get() || whitelist.get(event.getEntity().getClass()) != null) {
      Helper.printInform(fancyEntityName(event.getEntity()));
      count++;
    }
  }
  
  private static Collection<Class<?>> getLoadedClasses(ClassLoader loader) {
    try {
      Objects.requireNonNull(loader);
      Class<?> mclass = loader.getClass();
      while (mclass != ClassLoader.class) {
        mclass = mclass.getSuperclass();
      }
      Field classes = mclass.getDeclaredField("classes");
      classes.setAccessible(true);
      return (Vector<Class<?>>) classes.get(loader);
    } catch (Throwable t) {
      return Collections.emptyList();
    }
  }
}
