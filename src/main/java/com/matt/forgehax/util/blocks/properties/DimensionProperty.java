package com.matt.forgehax.util.blocks.properties;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.DimensionManager;

/**
 * Created on 5/23/2017 by fr1kin
 */
public class DimensionProperty implements IBlockProperty {
  
  private static final String HEADING = "dimensions";
  
  private Collection<DimensionType> dimensions = Sets.newHashSet();
  
  private boolean add(DimensionType type) {
    return type != null && dimensions.add(type);
  }
  
  public boolean add(int id) {
    try {
      return add(DimensionManager.getProviderType(id));
    } catch (Exception e) {
      // will throw exception if id does not exist
      return false;
    }
  }
  
  private boolean remove(DimensionType type) {
    return type != null && dimensions.remove(type);
  }
  
  public boolean remove(int id) {
    try {
      return remove(DimensionManager.getProviderType(id));
    } catch (Exception e) {
      return false; // will throw exception if id does not exist
    }
  }
  
  public boolean contains(int id) {
    if (dimensions.isEmpty()) {
      return true; // true if none other
    } else {
      try {
        return dimensions.contains(DimensionManager.getProviderType(id));
      } catch (Exception e) {
        return false;
      }
    }
  }
  
  @Override
  public void serialize(JsonObject in) {
    JsonArray add = new JsonArray();

    for (DimensionType dimension : dimensions) {
      add.add(dimension.getName());
    }

    in.add(HEADING, add);
  }
  
  @Override
  public void deserialize(JsonObject in) {
    JsonArray from = in.getAsJsonArray(HEADING);
    if (from == null) return;

    for (JsonElement e : from) {
      String dim = e.getAsString();
      for (DimensionType type : DimensionType.values()) {
        if (Objects.equals(type.getName(), dim)) {
          add(type);
          break;
        }
      }
    }
  }
  
  @Override
  public boolean isNecessary() {
    return !dimensions.isEmpty();
  }
  
  @Override
  public String helpText() {
    final StringBuilder builder = new StringBuilder("{");
    Iterator<DimensionType> it = dimensions.iterator();
    while (it.hasNext()) {
      String name = it.next().getName();
      builder.append(name);
      if (it.hasNext()) {
        builder.append(", ");
      }
    }
    builder.append("}");
    return builder.toString();
  }
  
  @Override
  public IBlockProperty newImmutableInstance() {
    return new ImmutableDimension();
  }
  
  @Override
  public String toString() {
    return HEADING;
  }
  
  private static class ImmutableDimension extends DimensionProperty {
    
    @Override
    public boolean add(int id) {
      return false;
    }
    
    @Override
    public boolean remove(int id) {
      return false;
    }
    
    @Override
    public boolean contains(int id) {
      return true; // Allow ALL dimensions by default
    }
  }
}
