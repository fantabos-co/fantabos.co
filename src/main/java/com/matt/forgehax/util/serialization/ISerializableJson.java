package com.matt.forgehax.util.serialization;

import com.google.gson.JsonObject;
import java.io.IOException;

/**
 * Created on 5/20/2017 by fr1kin
 */
public interface ISerializableJson {
  
  /**
   * Serialize this object and all necessary data into json
   *
   * @param in json Object to write into
   * @throws IOException if you format the json incorrectly
   */
  void serialize(final JsonObject in);
  
  /**
   * Deserialize data from json.
   *
   * @param in json object to read from
   * @throws IOException if you read the json incorrectly
   */
  void deserialize(final JsonObject in);
  
  /**
   * A unique heading to identify this object.
   *
   * @return unique name
   */
  default String getUniqueHeader() {
    return toString();
  }
}
