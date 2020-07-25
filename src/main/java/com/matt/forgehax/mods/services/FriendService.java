package com.matt.forgehax.mods.services;

import com.google.common.collect.Sets;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.events.RenderTabNameEvent;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Options;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.serialization.ISerializableJson;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.IOException;
import java.util.UUID;

@RegisterMod
public class FriendService extends ServiceMod {

  public final Options<FriendEntry> friendList =
    getCommandStub()
        .builders()
        .<FriendEntry>newOptionsBuilder()
        .name("database")
        .description("Contains all your friends")
        .supplier(Sets::newConcurrentHashSet)
        .factory(FriendEntry::new)
        .build();
  public final Setting<Boolean> color_chat =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("chat-color")
        .description("Change friends name color in chat")
        .defaultTo(true)
        .build();
  public final Setting<Boolean> color_tab =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("tab-color")
        .description("Change friends name color in TabList")
        .defaultTo(true)
        .build();

  public FriendService() {
    super("Friends");
  }

  public boolean isFriend(String name) {
    return friendList.stream().anyMatch(item -> item.name.equals(name));
  }

  private static boolean isActualUsername(String text, String user) {
    if (text.contains("<" + user + ">") ||
        text.contains(user + " ") ||
        text.contains(" " + user))
            return true;
    return false;
  }


  private ITextComponent replaceInComponent(ITextComponent source, String name) {
    ITextComponent out;
    if (source.getUnformattedComponentText().contains(name)) {
      out = isolateName(source, name);
    } else {
      out = new TextComponentString(source.getUnformattedComponentText()).setStyle(source.getStyle());
    }
    for (ITextComponent sibling : source.getSiblings()) {
      out.appendSibling(replaceInComponent(sibling, name));
    }
    return out;
  }

  private ITextComponent isolateName(ITextComponent source, String name) {
    if (source.getUnformattedComponentText().equals(name) ||
        source.getUnformattedComponentText().equals(name + " ") ||
        source.getUnformattedComponentText().equals(" " + name)) {
      return new TextComponentString(source.getUnformattedComponentText())
                    .setStyle(source.getStyle().setColor(TextFormatting.LIGHT_PURPLE));
    } else {
      ITextComponent out;
      String[] split_text = source.getUnformattedComponentText().split(name);
      if (split_text.length <= 0) return new TextComponentString(""); // nothing to do!
      if (split_text.length > 1) { // before and after
        out = new TextComponentString(split_text[0]).setStyle(source.getStyle());
        out.appendSibling(new TextComponentString(name).setStyle(source.getStyle().createDeepCopy().setColor(TextFormatting.LIGHT_PURPLE)));
        out.appendSibling(new TextComponentString(split_text[1]).setStyle(source.getStyle()));
      } else if (source.getUnformattedComponentText().startsWith(name)) {
        out = new TextComponentString(name).setStyle(source.getStyle().createDeepCopy().setColor(TextFormatting.LIGHT_PURPLE));
        out.appendSibling(new TextComponentString(split_text[0]).setStyle(source.getStyle()));
      } else {
        out = new TextComponentString(split_text[0]).setStyle(source.getStyle());
        out.appendSibling(new TextComponentString(name).setStyle(source.getStyle().createDeepCopy().setColor(TextFormatting.LIGHT_PURPLE)));
      }
      return out;
    }
  }

  @SubscribeEvent
  public void onChat(ClientChatReceivedEvent event) {
    if (!color_chat.get()) return;
    final String message_raw = event.getMessage().getUnformattedText();
    for (FriendEntry f : friendList) {
      if (isActualUsername(message_raw, f.getName())) {
        event.setMessage(replaceInComponent(event.getMessage(), f.getName()));
        break;
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOW) // So that, if name is recolored somewhere else,
  public void onTabUpdate(RenderTabNameEvent event) {  //   it gets overridden as friend
    if (!color_tab.get()) return;

    FriendEntry f = friendList.stream()
          .filter(item -> item.getName().equals(event.getName()))
          .findFirst()
          .orElse(null);
    if (f != null)
      event.setColor(Colors.BETTER_PINK.toBuffer());
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    friendList.deserializeAll();
    
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("add")
        .description("Add new friend (by name)")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String name = data.getArgumentAsString(0);
              try {
                this.friendList.add(new FriendEntry(name));
                Helper.printMessage("Added \"%s\" as friend", name);
              } catch (Exception ex) {
                Helper.printError("Could not add a friend for some reason (:");
              }
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("remove")
        .description("Remove a friend (by Name)")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String name = data.getArgumentAsString(0);
              final boolean changed = friendList.removeIf(entry -> entry.name.equals(name));
              if (changed) {
                Helper.printMessage("Removed \"%s\" from friends", name);
              } else {
                Helper.printInform("No friend named \"%s\" was found", name);
              }
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("list")
        .description("List all friends")
        .processor(data -> {
          Helper.printMessage("%d friends", friendList.size());
          for (FriendEntry entry : friendList) {
            data.write(entry.name);
          }
        })
        .build();
  }

  @Override
  public void onUnload() {
    friendList.serialize();
  }


  public static class FriendEntry implements ISerializableJson {
    private String name;
    // TODO identify by UUID

    FriendEntry(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
      writer.value(this.name);
    }

    @Override
    public void deserialize(JsonReader reader)  {
      this.name  = new JsonParser().parse(reader).getAsString();
    }

    @Override
    public String getUniqueHeader() {
      return this.name;
    }

    @Override
    public String toString() {
      return getUniqueHeader();
    }
  }
}
