package com.matt.forgehax.util.irc;

import com.google.gson.JsonObject;
import com.matt.forgehax.util.serialization.ISerializableJson;

public class ChannelEntry implements ISerializableJson {
  private final String name;
  private String password;

  public ChannelEntry(String name) {
    this.name = name;
    this.password = "";
  }

  public ChannelEntry(String name, String password) {
    this.name = name;
    this.password = password;
  }

  public String getPassword() {
    return this.password;
  }

  @Override
  public void serialize(JsonObject in) {
    in.addProperty(name, password);
  }

  @Override
  public void deserialize(JsonObject in)  {
    if (in.get(name) != null)
      this.password = in.get(name).getAsString();
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