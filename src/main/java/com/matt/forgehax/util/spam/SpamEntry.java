package com.matt.forgehax.util.spam;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.matt.forgehax.util.serialization.ISerializableJson;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import joptsimple.internal.Strings;

/**
 * Created on 7/18/2017 by fr1kin
 */
public class SpamEntry implements ISerializableJson {
  
  /**
   * A unique name used to identify this entry
   */
  private final String name;
  
  /**
   * List of messages (no duplicates allowed)
   */
  private final List<String> messages = Lists.newCopyOnWriteArrayList();
  
  private boolean enabled = true;
  
  /**
   * Keyword that triggers this
   */
  private String keyword = Strings.EMPTY;
  
  /**
   * How the message should be selected from the list
   */
  private SpamType type = SpamType.RANDOM;
  
  /**
   * What should trigger a message from being outputted
   */
  private SpamTrigger trigger = SpamTrigger.SPAM;
  
  /**
   * Custom delay
   */
  private long delay = 0;
  
  public SpamEntry(String name) {
    this.name = name;
  }
  
  public void add(String msg) {
    if (!Strings.isNullOrEmpty(msg) && !messages.contains(msg)) {
      messages.add(msg);
    }
  }
  
  public void remove(String msg) {
    messages.remove(msg);
  }
  
  private int nextIndex = 0;
  
  public String next() {
    if (!messages.isEmpty()) {
      switch (type) {
        case RANDOM:
          return messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
        case SEQUENTIAL:
          return messages.get((nextIndex++ % messages.size()));
      }
    }
    return Strings.EMPTY;
  }
  
  public void reset() {
    nextIndex = 0;
  }
  
  public boolean isEnabled() {
    return enabled;
  }
  
  public String getName() {
    return name;
  }
  
  public String getKeyword() {
    return keyword;
  }
  
  public SpamType getType() {
    return type;
  }
  
  public SpamTrigger getTrigger() {
    return trigger;
  }
  
  public List<String> getMessages() {
    return Collections.unmodifiableList(messages);
  }
  
  public long getDelay() {
    return delay;
  }
  
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
  
  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }
  
  public void setType(SpamType type) {
    if (type != null) {
      this.type = type;
    }
  }
  
  public void setType(String type) {
    setType(SpamType.valueOf(type.toUpperCase()));
  }
  
  public void setTrigger(SpamTrigger trigger) {
    if (trigger != null) {
      this.trigger = trigger;
    }
  }
  
  public void setTrigger(String trigger) {
    setTrigger(SpamTrigger.valueOf(trigger.toUpperCase()));
  }
  
  public void setDelay(long delay) {
    this.delay = delay;
  }
  
  public boolean isEmpty() {
    return messages.isEmpty();
  }
  
  @Override
  public void serialize(JsonObject in) {
    JsonObject add = new JsonObject();

    add.addProperty("enabled", enabled);
    add.addProperty("keyword", keyword);
    add.addProperty("type", type.name());
    add.addProperty("trigger", trigger.name());
    add.addProperty("delay", getDelay());

    JsonArray msgs = new JsonArray();
    for (String msg : messages)
      msgs.add(msg);
    add.add("messages", msgs);

    in.add(name, add);
  }
  
  @Override
  public void deserialize(JsonObject in) {
    JsonObject from = in.getAsJsonObject(name);
    if (from == null) return;
    
    if (from.get("enabled") != null) setEnabled(from.get("enabled").getAsBoolean());
    if (from.get("keyword") != null) setKeyword(from.get("keyword").getAsString());
    if (from.get("type") != null) setType(from.get("type").getAsString());
    if (from.get("trigger") != null) setTrigger(from.get("trigger").getAsString());
    if (from.get("delay") != null) setDelay(from.get("delay").getAsLong());
    if (from.get("messages") != null) {
      for (JsonElement e : from.getAsJsonArray("messages"))
        add(e.getAsString());
    }
  }
  
  @Override
  public boolean equals(Object obj) {
    return (obj instanceof SpamEntry
        && String.CASE_INSENSITIVE_ORDER.compare(name, ((SpamEntry) obj).name) == 0)
        || (obj instanceof String
        && String.CASE_INSENSITIVE_ORDER.compare(name, (String) obj) == 0);
  }
  
  @Override
  public int hashCode() {
    return name.toLowerCase().hashCode();
  }
  
  @Override
  public String toString() {
    return name;
  }
}
