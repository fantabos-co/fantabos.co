package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getWorld;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.matt.forgehax.Helper;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.EntityFakePlayer;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.client.entity.EntityOtherPlayerMP;

@RegisterMod
public class FakePlayer extends ToggleMod {

  private final Setting<String> name =
    getCommandStub()
      .builders()
      .<String>newSettingBuilder()
      .name("name")
      .description("Name of the fake player")
      .defaultTo("Tonio_Cartonio")
      .build();
  
  public FakePlayer() {
    super(Category.MISC, "FakePlayer", false, "Spawn a fake player clientside");
  }

  private EntityOtherPlayerMP player;

  @Override
  public void onLoad() {
    this.disable(false); // there cannot be a player already spawned
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("move")
        .description("Move the fake player to (depending on args number) : [x y z | x z | y | player position]")
        .processor(
            data -> {
              double x, y, z;
              switch (data.getArgumentCount()) {
                case 3:
                  x = Double.parseDouble(data.getArgumentAsString(0));
                  y = Double.parseDouble(data.getArgumentAsString(1));
                  z = Double.parseDouble(data.getArgumentAsString(2));
                  player.setPosition(x, y, z);
                  break;
                case 2:
                  x = Double.parseDouble(data.getArgumentAsString(0));
                  z = Double.parseDouble(data.getArgumentAsString(1));
                  player.setPosition(x, player.posY, z);
                  break;
                case 1:
                  y = Double.parseDouble(data.getArgumentAsString(0));
                  player.setPosition(player.posX, y, player.posZ);
                  break;
                case 0:
                  player.setPosition(getLocalPlayer().posX, getLocalPlayer().posY, getLocalPlayer().posZ);
                  break;
                default:
                  Helper.printWarning("Wrong number of arguments!");
                  return;
              }
              Helper.printInform("Moved %s to [ %.0f | %.0f | %.0f ]", player.getName(), player.posX, player.posY, player.posZ);
            })
        .build();
  }

  @Override
  protected void onEnabled() {
    if (getWorld() == null) {
      this.disable(false);
      return;
    }

    String[] info = getInfoFromName(name.get());
    if(info == null) {
    	info = new String[]{UUIDTypeAdapter.fromUUID(UUID.randomUUID()), name.get()};
    }
    GameProfile profile = new GameProfile(UUIDTypeAdapter.fromString(info[0]), info[1]); //Need the real UUID
    player = new EntityFakePlayer(getWorld(), profile);
    player.setPosition(getLocalPlayer().posX, getLocalPlayer().posY, getLocalPlayer().posZ);
    getWorld().addEntityToWorld(-500, player);
  }

  @Override
  protected void onDisabled() {
    if (getWorld() == null) return;
    getWorld().removeEntity(player);
  }
  
  /*Brought over from my mod, cannot normally get this information in game 
   * LMK if you want me to move these somewhere else/if there's a better
   * way to get this information -TheAlphaEpsilon*/
  
  private static String[] getInfoFromName(String name) {
		try {
			String[] toReturn = new String[2];
			JsonParser jsonparse = new JsonParser();
			JsonObject json = (JsonObject) jsonparse.parse(IOUtils.toString(new URL("https://api.mojang.com/users/profiles/minecraft/"+name), Charset.defaultCharset()));
			toReturn[0] = json.get("id").getAsString();
			toReturn[1] = json.get("name").getAsString();
			return toReturn;
		}
		catch (Exception e) {
			Helper.LOGGER.error("FakePlayer: Cannot get player info: " + e.getMessage());
			return null;
		}
	}
  
  
}
