package com.matt.forgehax.util.entry;

import com.google.gson.JsonObject;
import com.matt.forgehax.util.serialization.ISerializableJson;

/*
 * Made by Fraaz on November 10th 2020.
 * This is used only in FancyChat right now, but I guess it could be used for something else
 */

public class ChatCommandEntry implements ISerializableJson {

    private final String name;

    private int arguments; //How many arguments is FancyChat supposed to ignore?

    public ChatCommandEntry(String name) {
        this.name = name;
    }

    public void setArguments(int arguments) {
        this.arguments = arguments;
    }

    @Override
    public void serialize(JsonObject in) {
        JsonObject add = new JsonObject();
        add.addProperty("arguments", arguments);
        in.add(name, add);
    }

    @Override
    public void deserialize(JsonObject in) {
        JsonObject from = in.getAsJsonObject(name);
        if (from == null) return;
        if (from.get("arguments") != null) arguments = from.get("arguments").getAsInt();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof String && name.equals(obj)); //No point in allowing duplicates
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    public int getArguments() {
        return arguments;
    }

}
