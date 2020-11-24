package com.matt.forgehax.mods;

import com.google.common.collect.Sets;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.command.Options;
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
import net.minecraft.launchwrapper.Launch;
import net.minecraft.network.Packet;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Created on 10/12/2017 by fr1kin
 */
@RegisterMod
public class PacketCancel extends ToggleMod {
  
  private final Options<ClassEntry> blacklist =
      getCommandStub()
          .builders()
          .<ClassEntry>newOptionsBuilder()
          .name("blacklist")
          .description("Packets to cancel")
          .factory(ClassEntry::new)
          .supplier(Sets::newConcurrentHashSet)
          .build();
  
  public PacketCancel() {
    super(Category.MISC, "PacketCancel", false, "Cancel specific packets");
  }

  private int count = 0;

  @Override
  public String getDisplayText() {
    return (getModName() + String.format(" [" + TextFormatting.BLACK + "%d" + TextFormatting.RESET + "]", count));
  }

  @Override
  protected void onLoad() {
    blacklist
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
                      .filter(Packet.class::isAssignableFrom)
                      .filter(clazz -> clazz.getCanonicalName().toLowerCase().contains(className))
                      .sorted(
                          (o1, o2) ->
                              String.CASE_INSENSITIVE_ORDER.compare(
                                  o1.getCanonicalName(), o2.getCanonicalName()))
                      .findFirst();
              
              if (match.isPresent()) {
                Class<?> clazz = match.get();
                blacklist.add(new ClassEntry(clazz));
                data.write(String.format("Added class \"%s\"", clazz.getName()));
                data.markSuccess();
              } else {
                data.write(
                    String.format("Could not find any class name matching \"%s\"", className));
                data.markFailed();
              }
            })
        // .success(cb -> blacklist.serialize())
        .build();
    
    blacklist
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
                  blacklist
                      .stream()
                      .filter(entry -> entry.getClassName().toLowerCase().contains(className.toLowerCase()))
                      .sorted(
                          (o1, o2) ->
                              String.CASE_INSENSITIVE_ORDER.compare(
                                  o1.getClassName(), o2.getClassName()))
                      .findFirst();
              
              if (match.isPresent()) {
                ClassEntry entry = match.get();
                if (blacklist.remove(entry)) {
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
        // .success(cb -> blacklist.serialize())
        .build();
    
    blacklist
        .builders()
        .newCommandBuilder()
        .name("list")
        .description("List current contents")
        .processor(
            data -> {
              StringBuilder builder = new StringBuilder();
              for (ClassEntry c : blacklist.contents()) {
                builder.append(c.getClassName() + "\n");
              }
              data.write(builder.toString());
              data.markSuccess();
            })
        .build();

    blacklist
        .builders()
        .newCommandBuilder()
        .name("clear")
        .description("Remove all blacklisted packets")
        .processor(
            data -> {
              data.write("Cleared " + blacklist.size() + " entries");
              blacklist.clear();
              data.markSuccess();
            })
        .build();
  }
  
  @Override
  protected void onEnabled() {
    count = 0;
  }
  
  @SubscribeEvent(priority = EventPriority.HIGH)
  public void onPacketInbound(PacketEvent.Incoming.Pre event) {
    if (blacklist.get(event.getPacket().getClass()) != null) {
      event.setCanceled(true);
      count++;
    }
  }
  
  @SubscribeEvent(priority = EventPriority.HIGH)
  public void onPacketOutbound(PacketEvent.Outgoing.Pre event) {
    if (blacklist.get(event.getPacket().getClass()) != null) {
      event.setCanceled(true);
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
