package com.matt.forgehax.util.entry;

import com.google.gson.JsonObject;
import com.matt.forgehax.util.serialization.ISerializableJson;

/*
 * Made by Fraaz on November 5th 2020.
 * This is used only in FancyChat right now, but I guess it could be used for something else
 */

public class CommandPrefixEntry implements ISerializableJson {

    private final String prefix;

    public CommandPrefixEntry(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void serialize(JsonObject in) {
        in.addProperty(prefix, "N/A");
    }

    @Override
    public void deserialize(JsonObject in) {
        //nuffin
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof String && prefix.equals(obj));
    }

    @Override
    public int hashCode() {
        return prefix.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return prefix;
    }
}
