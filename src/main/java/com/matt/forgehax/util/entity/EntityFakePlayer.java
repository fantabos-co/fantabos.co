package com.matt.forgehax.util.entity;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.matt.forgehax.Helper;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/*
 * TheAlphaEpsilon
 * 4APR2020
 */

/*Just downloaded skin textures directly because I couldn't find any other way. TODO: Make better?*/
public class EntityFakePlayer extends EntityOtherPlayerMP {
	
	private static final ImageBufferDownload downloadedImageBuffer = new ImageBufferDownload();
	
	private ResourceLocation skin;
	private ResourceLocation cape;
	private String skinType;
	
	private BufferedImage skinImage;
	private BufferedImage capeImage;
	
	private boolean updatedSkinImage = false;
	private boolean updatedCapeImage = false;
	
    private NetworkPlayerInfo playerinfo;
	
	public EntityFakePlayer(World worldIn, GameProfile profile) {		
		super(worldIn, profile);
				
		skin = super.getLocationSkin();
		cape = super.getLocationCape();
		skinType = super.getSkinType();
		playerinfo = new NetworkPlayerInfo(profile);
		
		new Thread(() -> {
			
			//PlayerUtils.sendMessage("Start");

			int tryCounter = 0;
			
			String[] textures = null;
			while(textures == null) {
				//PlayerUtils.sendMessage("Try get textures");
				textures = getTexturesFromUUID(UUIDTypeAdapter.fromUUID(profile.getId()));
				if(++tryCounter > 5) {
					//PlayerUtils.sendMessage("No data, returning");
					return;
				}
			}
			
			//PlayerUtils.sendMessage("Got textures");
			
			boolean updateSkin = true;
			while(updateSkin && textures[0] != null) {
				try {
					skinImage = ImageIO.read(new URL(textures[0]));
					updateSkin = false;
				} catch (Exception e) {
					if(++tryCounter > 5) {
						//PlayerUtils.sendMessage("No skin, returning");
						return;
					}
				}
			}
			
			//PlayerUtils.sendMessage("Updated skin");
			
			boolean updateType = true;
			while(updateType && textures[1] != null) {
				try{
					skinType = textures[1];
					updateType = false;
				} catch (Exception e) {
					if(++tryCounter > 5) {
						//PlayerUtils.sendMessage("No type, returning");
						return;
					}
				}
			}
			
			//PlayerUtils.sendMessage("Updated type");
			
			boolean updateCape = true;
			while(updateCape && textures[2] != null) {
				try {
					capeImage = ImageIO.read(new URL(textures[2]));
					updateCape = false;
				} catch (Exception e) {
					if(++tryCounter > 5) {
						//PlayerUtils.sendMessage("No cape, returning");
						return;
					}
				}
			}
			
			//PlayerUtils.sendMessage("Updated cape");
			
			//PlayerUtils.sendMessage("Done");
			
		}).start();
		
	}
	
	@Override
	public ResourceLocation getLocationSkin() {
		if(!updatedSkinImage && skinImage != null) {
			updatedSkinImage = true;
			skin = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(getName(), new DynamicTexture(downloadedImageBuffer.parseUserSkin(skinImage)));
		}
		return skin;
	}
	
	@Override
	public ResourceLocation getLocationCape() {
		if(!updatedCapeImage && capeImage != null) {
			updatedCapeImage = true;
			cape = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(getName(), new DynamicTexture(capeImage));
		}
		return cape;
	}
	
	@Override
	public String getSkinType() {
		return skinType;
	}
	
	@Override
	public boolean isPlayerInfoSet() {
        return true;
    }
	
	@Override
	public boolean isWearing(EnumPlayerModelParts part) {
		return true;
	}
	
	@Override
	protected NetworkPlayerInfo getPlayerInfo() {
		return playerinfo;
    }
	
	/*I just copy pasted this from my mod. LMK If you want me to integrate it better with existing forge methods*/
	private static String[] getTexturesFromUUID(String uuid) {
		try {
			String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;			
						
			JsonParser jsonparse = new JsonParser();
						
			JsonObject data = (JsonObject) jsonparse.parse(IOUtils.toString(new URL(url), Charset.defaultCharset()));
			

			JsonArray properties = (JsonArray) data.get("properties");
			
			String value64 = ((JsonObject)(properties.get(0))).get("value").getAsString();			

			byte[] decodedbytes = Base64.getDecoder().decode(value64);
			

			JsonObject fromBytes = (JsonObject) jsonparse.parse(new String(decodedbytes));
			

			String[] toReturn = new String[3];
			
			JsonObject textures = (JsonObject) fromBytes.get("textures");
			
			JsonObject skin = (JsonObject) textures.get("SKIN");
			
			if(skin.has("metadata")) {
				toReturn[1] = "slim";
			} else {
				toReturn[1] = "default";
			}

			toReturn[0] = skin.get("url").getAsString();
			
			try{
				JsonObject cape = (JsonObject) textures.get("CAPE");
				toReturn[2] = cape.get("url").getAsString();
			} catch (Exception e) {
				
			}
			
			
			return toReturn;
		} catch (Exception e) {
			Helper.LOGGER.error("EntityFakePlayer: Cannot get skin data: " + e.getMessage());
			return null;
		}
	}
	
}
