package com.matt.forgehax.mods.managers;

import static com.matt.forgehax.Helper.getFileManager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.events.RenderTabNameEvent;
import com.matt.forgehax.util.FileManager;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.ColorClamp;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.PlayerInfo;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.serialization.ISerializableJson;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RegisterMod
public class FriendManager extends ServiceMod {

  private static final Path BASE_PATH = getFileManager().getBaseResolve("friends");
  private static final File FRIENDS_FILE = BASE_PATH.resolve("friends.json").toFile();
  private static final File TAGS_FILE = BASE_PATH.resolve("tags.json").toFile();

  private static final Map<String, FriendEntry> friendList = new ConcurrentHashMap<String, FriendEntry>();
  private static final Map<String, TagEntry> tagList = new ConcurrentHashMap<String, TagEntry>();

  // TODO make friend searching not case sensitive!

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

  private static FriendManager INSTANCE;

  public FriendManager() {
    super("Friends", "Manage a friend list with tags and highlight them");
    INSTANCE = this;
  }

  public static boolean isFriend(String name) { return friendList.get(name) != null; }
  public static boolean isFriendly(String name) { // returns false for friends with hostile tag
    return isFriend(name) && !INSTANCE.hostile.get().equals(friendList.get(name).getTag());
  }

  public static Collection<FriendEntry> getAllFriends() {
    return friendList.entrySet().stream()
                      .map(e -> e.getValue())
                      .collect(Collectors.toList());
  }

  public static String getFriendTagColored(String name) {
    if (!isFriend(name)) return "";
    return ColorClamp.getClampedColor(getFriendColor(name)) + friendList.get(name).getTag() + TextFormatting.RESET;
  }

  public static String getFriendTag(String name) {
    if (!isFriend(name)) return "";
    return friendList.get(name).getTag();
  }

  public static int getFriendColor(String name) {
    if (!isFriend(name)) return 0;
    String tag = friendList.get(name).getTag();
    if (tagList.get(tag) == null) // make a new tag as soon as it's needed
      tagList.put(tag, new TagEntry(tag));
    return tagList.get(friendList.get(name).getTag()).getColor();
  }

  public static void addFriend(String name) { addFriend(name, "friend"); }
  public static void addFriend(String name, String tag) {
    if (isFriend(name)) {
      Helper.printInform("\"%s\" is already a friend", name);
      return;
    }

    AddFriendThread t = new AddFriendThread(name, tag);
    t.start();
  }

  private static class AddFriendThread extends Thread {

    private final String name;
    private final String tag;

    public AddFriendThread(String name, String tag) {
      super();
      this.name = name;
      this.tag = tag;
    }

    public void run() {
      try {
        PlayerInfo player = new PlayerInfo(name);
        FriendEntry friend = new FriendEntry(player);
        friend.setTag(tag);
        friendList.put(name, friend);
        Helper.printMessage("Added \"%s\" as %s", player.getName(), friend.getTag());
      } catch (Exception ex) {
        Helper.printError("No user found with that name");
      }
    }
  }

  private static boolean isActualUsername(String text, String user) {
    if (text.contains("<" + user + ">") ||
        text.contains(user + " ") ||
        text.contains(" " + user))
            return true;
    return false;
  }

  private ITextComponent replaceInComponent(ITextComponent source, FriendEntry friend) {
    ITextComponent out;
    if (source.getUnformattedComponentText().contains(friend.getName())) {
      out = isolateName(source, friend);
    } else {
      out = new TextComponentString(source.getUnformattedComponentText()).setStyle(source.getStyle().createDeepCopy());
    }
    for (ITextComponent sibling : source.getSiblings()) {
      out.appendSibling(replaceInComponent(sibling, friend));
    }
    return out;
  }

  private ITextComponent pimpName(Style source, String name, TextFormatting color, HoverEvent hover, ClickEvent click) {
    ITextComponent out = new TextComponentString(name);
    Style outStyle = source.createDeepCopy();
    outStyle.setColor(color);
    if (outStyle.getClickEvent() == null && click != null) outStyle.setClickEvent(click);
    if (outStyle.getHoverEvent() == null && hover != null) outStyle.setHoverEvent(hover);
    out.setStyle(outStyle);
    return out;
  }

  private HoverEvent getHover(String tag, TextFormatting color, String memo) {
    if(memo.equalsIgnoreCase("")) return new HoverEvent(HoverEvent.Action.SHOW_TEXT, Helper.getFormattedText(tag, color, true, false));
    return new HoverEvent(HoverEvent.Action.SHOW_TEXT,
          Helper.getFormattedText(tag + "\n", color, true, false)
                  .appendSibling(
                    Helper.getFormattedText(memo, TextFormatting.GRAY, false, false)
                  ));
  }

  private ITextComponent isolateName(ITextComponent source, FriendEntry friend) {
    String name = friend.getName();
    TextFormatting c = ColorClamp.getClampedColor(tagList.get(friend.getTag()).getColor());
    HoverEvent hover = getHover(friend.getTag(), c, friend.getMemo());
    ClickEvent click = new ClickEvent(Action.SUGGEST_COMMAND, "/w " + name + " ");

    if (source.getUnformattedComponentText().equals(name) ||
        source.getUnformattedComponentText().equals(name + " ") ||
        source.getUnformattedComponentText().equals(" " + name)) {
      return pimpName(source.getStyle(), source.getUnformattedComponentText(), c, hover, click);
    } else {
      ITextComponent out;
      String[] split_text = source.getUnformattedComponentText().split(name, 2);
      if (split_text.length <= 0) return new TextComponentString(""); // nothing to do!
      if (split_text.length > 1) { // before and after
        out = new TextComponentString(split_text[0]).setStyle(source.getStyle().createDeepCopy());
        out.appendSibling(pimpName(source.getStyle(), name, c, hover, click));
        out.appendSibling(new TextComponentString(split_text[1]).setStyle(source.getStyle().createDeepCopy()));
      } else if (source.getUnformattedComponentText().startsWith(name)) {
        out = pimpName(source.getStyle(), name, c, hover, click);
        out.appendSibling(new TextComponentString(split_text[0]).setStyle(source.getStyle().createDeepCopy()));
      } else {
        out = new TextComponentString(split_text[0]).setStyle(source.getStyle().createDeepCopy());
        out.appendSibling(pimpName(source.getStyle(), name, c, hover, click));
      }
      return out;
    }
  }

  @SubscribeEvent
  public void onChat(ClientChatReceivedEvent event) {
    if (!color_chat.get()) return;
    final String message_raw = event.getMessage().getUnformattedText();
    for (String fname : friendList.keySet()) {
      if (isActualUsername(message_raw, fname)) {
        FriendEntry f = friendList.get(fname);
        event.setMessage(replaceInComponent(event.getMessage(), f));
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
  protected void onUnload() {
    // Serialize
    //  friends are not part of the config file
    JsonObject friends_out = new JsonObject();
    for (String fname : friendList.keySet())
      friendList.get(fname).serialize(friends_out);
    FileManager.save(FRIENDS_FILE, friends_out);

    JsonObject tags_out = new JsonObject();
    for (String tname : tagList.keySet())
      tagList.get(tname).serialize(tags_out);
    FileManager.save(TAGS_FILE, tags_out);
  }

  @Override
  protected void onLoad() {
    // Deserialize
    //  friends are not part of the config file

    JsonObject friends = FileManager.load(FRIENDS_FILE);
    if (friends != null) {
      for (Map.Entry<String, JsonElement> e : friends.entrySet()) {
        FriendEntry newFriend = new FriendEntry(e.getKey());
        newFriend.deserialize(friends);
        friendList.put(newFriend.getName(), newFriend);
      }
    }

    JsonObject tags = FileManager.load(TAGS_FILE);
    if (tags != null) {
      for (Map.Entry<String, JsonElement> e : tags.entrySet()) {
        TagEntry newTag = new TagEntry(e.getKey());
        newTag.deserialize(tags);
        tagList.put(newTag.getTag(), newTag);
      }
    }

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("add")
        .description("Add new friend (by name)")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String name = data.getArgumentAsString(0);
              if (data.getArgumentCount() > 1) {
                addFriend(name, data.getArgumentAsString(1));
              } else {
                addFriend(name);
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
          for (String fname : friendList.keySet()) {
            FriendEntry f = friendList.get(fname);
            data.write(String.format("[%s] %s : %s",
                  getFriendTagColored(fname) + TextFormatting.GRAY, fname,
                  TextFormatting.DARK_GRAY + f.getMemo()));
          }
          data.write(String.format("total: %d friends", friendList.size()));
        })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("whois")
        .description("Show info about a player")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String name = data.getArgumentAsString(0);
              FriendEntry f = friendList.get(name);
              if (f == null) {
                Helper.printError("No friend named %s", name);
                return;
              }
              data.write(f.getUniqueHeader());
              data.write(f.getName() + " - " + getFriendTagColored(name));
              data.write(f.getMemo());
              try {
                PlayerInfo p = new PlayerInfo(UUID.fromString(f.getUniqueHeader()));
                data.write("Name history (newest-oldest) [ " + p.getNameHistoryAsString() + " ]");
              } catch (IOException e) {
                e.printStackTrace();
                // ignore, no name history 4 u
              }
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
        
    Command set = getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("set")
        .description("Set tag or memo for target player")
        .processor(data -> data.write("[ tag / memo ]"))
        .build();

    set.builders()
        .newCommandBuilder()
        .name("tag")
        .description("set tag : .friend set tag <friend> <tag>")
        .processor(
          data -> {
            data.requiredArguments(2);
            final String name = data.getArgumentAsString(0);
            final String tag = data.getArgumentAsString(1);
            FriendEntry friend = friendList.get(name);
            if (friend != null) {
              friend.setTag(tag);
              friendList.put(name, friend); // Will replace previous since it's a map
              Helper.printInform("New tag for %s : %s", friend.getName(), friend.getTag());
            } else {
              Helper.printWarning("No friend named %s", name);
            }
          })
        .build();

    set.builders()
        .newCommandBuilder()
        .name("memo")
        .description("set memo : .friend set memo <friend> <memo>")
        .processor(
          data -> {
            data.requiredArguments(2);
            final String name = data.getArgumentAsString(0);
            StringBuilder memo = new StringBuilder();
            FriendEntry friend = friendList.get(name);
            if (friend != null) {
              for (int i=1; i < data.getArgumentCount(); i++)
                memo.append(data.getArgumentAsString(i)).append(" ");
              friend.setMemo(memo.toString());
              friendList.put(name, friend); // Will replace previous since it's a map
              Helper.printInform("New memo for %s : \"%s\"", friend.getName(), friend.getMemo());
            } else {
              Helper.printWarning("No friend named %s", name);
            }
          })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("tags")
        .description("List all tags")
        .processor(data -> {
          for (String tag : tagList.keySet()) {
            Color c = Color.of(tagList.get(tag).getColor());
            data.write(String.format("%s %s[ %d %d %d ]%s", tag, ColorClamp.getClampedColor(c.toBuffer()),
                                                          c.getRed(), c.getGreen(), c.getBlue(), TextFormatting.RESET));
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
              TagEntry tag = tagList.get(tag_str);
              if (tag == null) tag = new TagEntry(tag_str);
              tag.setColor(Color.of(r, g, b).toBuffer());
              tagList.put(tag_str, tag); // Will overwrite since it's a map
              data.write(String.format("Registered tag %s", tag_str));
            })
        .build();

    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("clear-tag")
        .description("Remove a tag (by name)")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String name = data.getArgumentAsString(0);
              try {
                tagList.remove(name);
                Helper.printMessage("Removed \"%s\" from tags", name);
              } catch (Exception ex) {
                Helper.printError("No tag found with that name");
              }
            })
        .build();
  }

  private class LoadNamesThread extends Thread {
    public void run() {
      for (String fname : friendList.keySet())
        friendList.get(fname).lookup();
      LOGGER.info("Finished looking up friends names");
      Helper.printInform("Refreshed friend list, now contains %d friends", friendList.size());
    }
  }

  public static class FriendEntry implements ISerializableJson {
    private final String uuid;
    private PlayerInfo player = null;
    private String tag = "friend";
    private String name = null;
    private String memo = "";

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

    public void setMemo(String memo) {
      this.memo = memo;
    }

    public void setTag(String tag) {
      this.tag = tag;
    }

    public String getName() {
      if (this.name == null) return this.uuid.toString();
      return this.name;
    }

    @Override
    public void serialize(JsonObject in) {
      JsonObject add = new JsonObject();
      add.addProperty("name", this.name);
      add.addProperty("tag", this.tag);
      add.addProperty("memo", this.memo);
      
      in.add(uuid, add);
    }

    @Override
    public void deserialize(JsonObject in) {
      JsonObject from = in.getAsJsonObject(uuid);
      if (from == null) return;

      if (from.get("tag") != null) this.tag = from.get("tag").getAsString();
      if (from.get("name") != null) this.name = from.get("name").getAsString();
      if (from.get("memo") != null) this.memo = from.get("memo").getAsString();
    }

    @Override
    public String getUniqueHeader() {
      return this.uuid;
    }

    public String getTag() {
      return this.tag;
    }

    public String getMemo() {
      return this.memo;
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
    public void serialize(JsonObject in) {
      in.addProperty(tag, color);
    }

    @Override
    public void deserialize(JsonObject in)  {
      if (in.get(tag) != null) this.color = in.get(tag).getAsInt();
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
}
