package com.matt.forgehax.util.blocks.properties;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Iterator;
import joptsimple.internal.Strings;

/**
 * Created on 5/23/2017 by fr1kin
 */
public class TagProperty implements IBlockProperty {
  
  private static final String HEADING = "tags";
  
  private final Collection<String> tags = Sets.newTreeSet(String.CASE_INSENSITIVE_ORDER);
  
  public boolean add(String tag) {
    return !Strings.isNullOrEmpty(tag) && tags.add(tag);
  }
  
  public boolean remove(String tag) {
    return !Strings.isNullOrEmpty(tag) && tags.remove(tag);
  }
  
  public boolean contains(String tag) {
    return !Strings.isNullOrEmpty(tag) && tags.contains(tag);
  }
  
  @Override
  public void serialize(JsonObject in) {
    JsonArray add = new JsonArray();
    for (String tag : tags)
      add.add(tag);

    in.add(HEADING, add);
  }
  
  @Override
  public void deserialize(JsonObject in) {
    JsonArray from = in.getAsJsonArray(HEADING);
    if (from == null) return;

    for (JsonElement e : from) {
      add(e.getAsString());
    }
  }
  
  @Override
  public boolean isNecessary() {
    return !tags.isEmpty();
  }
  
  @Override
  public String helpText() {
    final StringBuilder builder = new StringBuilder("{");
    Iterator<String> it = tags.iterator();
    while (it.hasNext()) {
      String tag = it.next();
      builder.append(tag);
      if (it.hasNext()) {
        builder.append(", ");
      }
    }
    builder.append("}");
    return builder.toString();
  }
  
  @Override
  public IBlockProperty newImmutableInstance() {
    return new ImmutableBlockTag();
  }
  
  @Override
  public String toString() {
    return HEADING;
  }
  
  private static class ImmutableBlockTag extends TagProperty {
    
    @Override
    public boolean add(String tag) {
      return false;
    }
    
    @Override
    public boolean remove(String tag) {
      return false;
    }
    
    @Override
    public boolean contains(String tag) {
      return false; // default to false, we don't want an entry saying it has every tag
    }
  }
}
