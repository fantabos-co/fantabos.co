package com.matt.forgehax.mods.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.events.RenderTabNameEvent;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.ColorClamp;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Options;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.PlayerInfo;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.serialization.ISerializableJson;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@RegisterMod
public class FriendService extends ServiceMod {

  public final Options<FriendEntry> friendList =
    getCommandStub()
        .builders()
        .<FriendEntry>newOptionsBuilder()
        .name("players")
        .description("Contains all your friends")
        .supplier(GroupedFriendMap::new)
        .factory(FriendEntry::new)
        .build();
  public final Options<TagEntry> tagList =
    getCommandStub()
        .builders()
        .<TagEntry>newOptionsBuilder()
        .name("tags")
        .description("Contains all your groups")
        .supplier(TagColorMap::new)
        .factory(TagEntry::new)
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
  public final Setting<String> hostile =
    getCommandStub()
        .builders()
        .<String>newSettingBuilder()
        .name("hostile")
        .description("Friends with this tag won't be treated as friends for attacking but will remain registered")
        .defaultTo("kos")
        .build();

  public FriendService() {
    super("Friends");
  }

  public boolean isFriend(String name) {
    return ((GroupedFriendMap) friendList.contents()).isFriend(name);
  }

  public boolean isFriendly(String name) {
    return ((GroupedFriendMap) friendList.contents()).isFriend(name) && 
           !((GroupedFriendMap) friendList.contents()).get(name).getTag().equals(hostile.get());
  }

  public int getFriendColor(String name) {
    String tag = ((GroupedFriendMap) friendList.contents()).get(name).getTag();
    if (!((TagColorMap) tagList.contents()).contains(tag)) 
      ((TagColorMap) tagList.contents()).add(new TagEntry(tag)); // Register with default color
    return ((TagColorMap) tagList.contents()).get(tag).getColor();
  }

  private static boolean isActualUsername(String text, String user) {
    if (text.contains("<" + user + ">") ||
        text.contains(user + " ") ||
        text.contains(" " + user))
            return true;
    return false;
  }

  private boolean once;

  private ITextComponent replaceInComponent(ITextComponent source, String name) {
    LOGGER.warn(String.format("Entering recursion : \"%s\"", source.getUnformattedComponentText()));
    ITextComponent out;
    if (!once && source.getUnformattedComponentText().contains(name)) {
      out = isolateName(source, name);
      once = true;
    } else {
      out = new TextComponentString(source.getUnformattedComponentText()).setStyle(source.getStyle().createDeepCopy());
    }
    for (ITextComponent sibling : source.getSiblings()) {
      out.appendSibling(replaceInComponent(sibling, name));
    }
    return out;
  }

  private ITextComponent isolateName(ITextComponent source, String name) {
    TextFormatting c = ColorClamp.getClampedColor(getFriendColor(name));
    if (source.getUnformattedComponentText().equals(name) ||
        source.getUnformattedComponentText().equals(name + " ") ||
        source.getUnformattedComponentText().equals(" " + name)) {
      return new TextComponentString(source.getUnformattedComponentText())
                    .setStyle(source.getStyle().createDeepCopy().setColor(c));
    } else {
      ITextComponent out;
      String[] split_text = source.getUnformattedComponentText().split(name, 2);
      if (split_text.length <= 0) return new TextComponentString(""); // nothing to do!
      if (split_text.length > 1) { // before and after
        out = new TextComponentString(split_text[0]).setStyle(source.getStyle().createDeepCopy());
        out.appendSibling(new TextComponentString(name).setStyle(source.getStyle().createDeepCopy().setColor(c)));
        out.appendSibling(new TextComponentString(split_text[1]).setStyle(source.getStyle().createDeepCopy()));
      } else if (source.getUnformattedComponentText().startsWith(name)) {
        out = new TextComponentString(name).setStyle(source.getStyle().createDeepCopy().setColor(c));
        out.appendSibling(new TextComponentString(split_text[0]).setStyle(source.getStyle().createDeepCopy()));
      } else {
        out = new TextComponentString(split_text[0]).setStyle(source.getStyle().createDeepCopy());
        out.appendSibling(new TextComponentString(name).setStyle(source.getStyle().createDeepCopy().setColor(c)));
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
        once = false;
        event.setMessage(replaceInComponent(event.getMessage(), f.getName()));
        break;
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOW) // So that, if name is recolored somewhere else,
  public void onTabUpdate(RenderTabNameEvent event) {  //   it gets overridden as friend
    if (!color_tab.get()) return;

    if (isFriend(event.getName())) {
      event.setColor(getFriendColor(event.getName()));
    }
  }

  @Override
  protected void onLoad() {
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("add")
        .description("Add new friend (by name)")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String name = data.getArgumentAsString(0);
              if (isFriend(name)) {
                Helper.printInform("\"%s\" is already a friend", name);
                return;
              }
              try {
                PlayerInfo player = new PlayerInfo(name);
                FriendEntry friend = new FriendEntry(player);
                if (data.getArgumentCount() > 1) {
                  friend.setTag(data.getArgumentAsString(1));
                }
                this.friendList.add(friend);
                Helper.printMessage("Added \"%s\" as %s", player.getName(), friend.getTag());
              } catch (Exception ex) {
                Helper.printError("No user found with that name");
              }
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("tag")
        .description("Set tag for target player: .friend tag <player> <tag>")
        .processor(
            data -> {
              data.requiredArguments(2);
              final String name = data.getArgumentAsString(0);
              final String tag = data.getArgumentAsString(1);
              FriendEntry friend = ((GroupedFriendMap) friendList.contents()).get(name);
              if (friend != null) {
                friend.setTag(tag);
                this.friendList.add(friend); // Will replace previous since it's a map
                Helper.printInform("New tag for %s : %s", friend.getName(), friend.getTag());
              } else {
                Helper.printWarning("No friend named %s", name);
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
              try {
                PlayerInfo player = new PlayerInfo(name);
                friendList.remove(player.getName());
                Helper.printMessage("Removed \"%s\" from friends", name);
              } catch (Exception ex) {
                Helper.printError("No user found with that name");
              }
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("list")
        .description("List all friends")
        .processor(data -> {
          for (FriendEntry entry : friendList) {
            data.write(String.format("%s - %s", entry.getName(), entry.getTag()));
          }
          data.write(String.format("total: %d friends", friendList.size()));
        })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("groups")
        .description("List all tags")
        .processor(data -> {
          for (TagEntry entry : tagList) {
            Color c = Color.of(entry.getColor());
            data.write(String.format("%s [ %d %d %d ]", entry.getTag(), c.getRed(), c.getGreen(), c.getBlue()));
          }
          data.write(String.format("total: %d tags", tagList.size()));
        })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("color")
        .description("Set color for tag : .friends color <tag> <r> <g> <b>")
        .processor(
            data -> {
              data.requiredArguments(4);
              final String tag_str = data.getArgumentAsString(0);
              final int r = Integer.parseInt(data.getArgumentAsString(1));
              final int g = Integer.parseInt(data.getArgumentAsString(2));
              final int b = Integer.parseInt(data.getArgumentAsString(3));
              TagEntry tag = ((TagColorMap) tagList.contents()).get(tag_str);
              if (tag == null) tag = new TagEntry(tag_str);
              tag.setColor(Color.of(r, g, b).toBuffer());
              tagList.add(tag); // Will overwrite since it's a map
              data.write(String.format("Registered tag %s", tag_str));
            })
        .build();

      getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("refresh")
        .description("Retry to look up missing friend names")
        .processor(
            data -> {
              data.requiredArguments(0);
                  // This may be slow, make a thread for it!
              LoadNamesThread t = new LoadNamesThread();
              t.start();
            })
        .build();
  }

  private class LoadNamesThread extends Thread {
    public void run() {
      ((GroupedFriendMap) friendList.contents()).lookupNames();
      LOGGER.info("Finished looking up friends names");
      Helper.printInform("Refreshed friend list, now contains %d friends", friendList.size());
    }
  }

  public static class FriendEntry implements ISerializableJson {
    private final String uuid;
    private PlayerInfo player = null;
    private String tag = "Friend";
    private String name = null;

    FriendEntry(String uuid) {
      this.uuid = uuid;
    }

    FriendEntry(PlayerInfo player) {
      this.player = player;
      this.uuid = player.getId().toString();
      this.name = player.getName();
    }

    public void lookup() {
      try {
        PlayerInfo buf = new PlayerInfo(UUID.fromString(uuid));
        LOGGER.info(String.format("Looked up %s", buf.getName()));
        this.name = buf.getName();
        this.player = buf;
      } catch (IOException e) {
        LOGGER.error(String.format("Could not look up player with uuid \"%s\"", uuid));
      }
    }

    public void setTag(String tag) {
      this.tag = tag;
    }

    public String getName() {
      return this.name;
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
      JsonObject buf = new JsonObject();
      buf.addProperty("name", this.name);
      buf.addProperty("tag", this.tag);
      writer.jsonValue(buf.toString());
    }

    @Override
    public void deserialize(JsonReader reader) {
      JsonObject buf = new JsonParser().parse(reader).getAsJsonObject();
      this.tag = buf.get("tag").getAsString();
      this.name = buf.get("name").getAsString();
    }

    @Override
    public String getUniqueHeader() {
      return this.uuid;
    }

    public String getTag() {
      return this.tag;
    }

    @Override
    public String toString() {
      return getUniqueHeader();
    }
  }

  public static class TagEntry implements ISerializableJson {
    private final String tag;
    private int color;

    TagEntry(String tag) {
      this.tag = tag;
      this.color = Colors.BETTER_PINK.toBuffer();
    }

    TagEntry(String tag, int r, int g, int b) {
      this.tag = tag;
      this.color = Color.of(r, g, b).toBuffer();
    }

    public void setColor(int color) {
      this.color = color;
    }

    public String getTag() {
      return getUniqueHeader();
    }

    @Override
    public void serialize(JsonWriter writer) throws IOException {
      writer.value(this.color);
    }

    @Override
    public void deserialize(JsonReader reader)  {
      this.color  = new JsonParser().parse(reader).getAsInt();
    }

    @Override
    public String getUniqueHeader() {
      return this.tag;
    }

    public int getColor() {
      return this.color;
    }

    @Override
    public String toString() {
      return getUniqueHeader();
    }
  }

  private static class TagColorMap implements Collection<TagEntry> {
    private Map<String, TagEntry> map = new HashMap<>();

    public int getColor(String tag) {
      return this.get(tag).getColor();
    }

    public TagEntry get(String tag) {
      return map.get(tag);
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override
    public boolean isEmpty() {
      return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      return map.containsKey((String) o);
    }

    @Override
    public Iterator<TagEntry> iterator() {
      return map.values().iterator();
    }

    @Override
    public Object[] toArray() {
      return map.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return map.values().toArray(a);
    }

    @Override
    public boolean add(TagEntry tag) {
      return map.put(tag.getTag(), tag) != null;
    }

    @Override
    public boolean remove(Object o) {
      return map.remove((String) o) != null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return map.values().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends TagEntry> c) {
      boolean changed = false;
      for (TagEntry entry : c) {
        if (map.put(entry.getTag(), entry) != null) changed = true;
      }
      return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return map.values().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      return map.values().retainAll(c);
    }

    @Override
    public void clear() {
      map.clear();
    }
  }

  private static class GroupedFriendMap implements Collection<FriendEntry> {
    private Map<String, FriendEntry> map = new HashMap<>();

    public boolean isFriend(String name) {
      return this.get(name) != null;
    }

    public FriendEntry get(String name) {
      return map.get(name);
    }

    public void lookupNames() {
      for (FriendEntry f : map.values()) {
        f.lookup();
      }
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override
    public boolean isEmpty() {
      return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
      if (o instanceof String) {
        return map.containsKey((String) o);
      } else if (o instanceof FriendEntry) {
        return map.containsKey(((FriendEntry)o).getName());
      } else {
        return false;
      }
    }

    @Override
    public Iterator<FriendEntry> iterator() {
      return map.values().iterator();
    }

    @Override
    public Object[] toArray() {
      return map.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
      return map.values().toArray(a);
    }

    @Override
    public boolean add(FriendEntry friend) {
      return map.put(friend.getName(), friend) != null;
    }

    @Override
    public boolean remove(Object o) {
      if (o instanceof String) {
        return map.remove((String) o) != null;
      } else if (o instanceof FriendEntry) {
        return map.remove(((FriendEntry) o).getName()) != null;
      } else {
        return false;
      }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
      return map.values().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends FriendEntry> c) {
      boolean changed = false;
      for (FriendEntry entry : c) {
        if (map.put(entry.getName(), entry) != null) changed = true;
      }
      return changed;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      return map.values().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      return map.values().retainAll(c);
    }

    @Override
    public void clear() {
      map.clear();
    }
  }
}