package com.matt.forgehax.util;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.matt.forgehax.Helper;
import com.matt.forgehax.mods.services.FHTexturesMod;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import static com.matt.forgehax.Globals.MC;

/*
 * TheAlphaEpsilon
 * 24OCT2020
 * If a player's UUID is in the webserver, this class
 * stores player data when they join a mc server, and removes it when
 * they leave.
 */
public class FHTextures {

	/*LOCATION OF TEXTURES*/
	private static final String url = "http://data.2b2t.it/";
	private static final String users = "userdata";
	
	/*Set of registered users*/
	private static final Map<UUID, Set<Type>> uuids = new HashMap<>(); //Only edited in main thread
	
	/*Local Resources*/
	private static final Map<UUID, Map<Type, ResourceLocation>> players = new ConcurrentHashMap<>();
	private static final Map<UUID, Map<Type, BufferedImage>> rawImage = new ConcurrentHashMap<>();
	
	//Called to update/download the set of users to check textures for
	public static void updateUsers() {
		
		new Thread(() -> {
			
			String urlToGet = url + users;
			
	        try {
				
	        	InputStream is = new URL(urlToGet).openStream();
				JsonParser jsonParser = new JsonParser();
				JsonElement json = jsonParser.parse(new InputStreamReader(is, "UTF-8"));
				
				json.getAsJsonObject().entrySet().forEach(entry -> {
					Set<Type> types = new HashSet<>();
					entry.getValue().getAsJsonArray().forEach(element -> types.add(Type.valueOf(element.getAsString())));
					uuids.put(UUIDTypeAdapter.fromString(entry.getKey()), types);
				});
				
				Helper.LOGGER.info("FHTextures: downloaded user list, " + uuids.size() + " users.");
				
			} catch (Exception e) {
				Helper.LOGGER.error("Couldn't download FHTexture user list: " + e.getMessage());
			}
	        
		}).start();
		
	}
	
	public static boolean isRegisteredUser(UUID uuid) {
		return uuids.containsKey(uuid);
	}
	
	public static Set<UUID> registeredUsers() {
		return new HashSet<>(uuids.keySet());
	}
	
	public static Set<UUID> currentIds() {
		return new HashSet<>(players.keySet());
	}
	
	/**
	 * Retrieves data about uuid
	 */
	public static void addPlayer(UUID uuid) {
				
		if(players.containsKey(uuid)) {
			return;
		}
		
		players.put(uuid, new ConcurrentHashMap<>());
		
		Helper.LOGGER.info("Checking textures for " + uuid.toString());
		
		for(Type t : uuids.get(uuid)) {
			
			new Thread(() -> { //A thread because downloading stuff can take a bit
				
				String suuid = UUIDTypeAdapter.fromUUID(uuid);
				String urlToGet = url + t.webLocation() + suuid;
				
				try {
					
			        InputStream is = new URL(urlToGet).openStream();
			        BufferedImage texture = ImageIO.read(is); //OpenGL is only in the main MC thread, so we can only get images here.
			        is.close();
			        Helper.LOGGER.info("Got " + t.toString() + " of user " + uuid.toString());
			        
			        if(rawImage.containsKey(uuid)) {
						rawImage.get(uuid).put(t, texture);
					} else {
						Map<Type, BufferedImage> map = new ConcurrentHashMap<>();
						map.put(t, texture);
						rawImage.put(uuid, map);
					}
					
				} catch (Exception e) {
					Helper.LOGGER.info("No textures for " + uuid.toString());
				} 
				
			}).start();
			
		}
		
	}
	
	/**
	 * Removes a player's data
	 * Needs to be called in the render thread
	 */
	public static void removePlayer(UUID uuid) {
		
		Helper.LOGGER.info("Removed textures for user " + uuid.toString());
		
		if(players.containsKey(uuid)) {
			
			Map<Type, ResourceLocation> map = players.get(uuid);
			
			for(Type t : Type.values()) {
				
				ResourceLocation rl = map.get(t);
				if(rl != null) {
					
					MC.getTextureManager().deleteTexture(rl);
					
				}
				
			}
			
		}
		players.remove(uuid);
		
	}
	
	/**
	 * Is a player in the map
	 */
	public static boolean hasPlayer(UUID uuid) {
		return players.containsKey(uuid);
	}
	
	/**
	 * Returns a player's resource, null if none
	 */
	public static ResourceLocation getResource(UUID uuid, int type) {
		
		if(!FHTexturesMod.isEnabled) {
			return null;
		}
		
		//TODO: Check for other texture updates
		try {
			if(!players.get(uuid).isEmpty()) { //If the uuid of resources is not empty, return the resource
				return players.get(uuid).get(Type.values()[type]);
			} else if(rawImage.containsKey(uuid)) { //If the raw images exist, convert, set, and send
				Map<Type, BufferedImage> map = rawImage.remove(uuid);
				for(Type t : map.keySet()) {
					players.get(uuid).put(t, 
							MC.getTextureManager().getDynamicTextureLocation(
									uuid.toString()+"_"+t.toString(), 
									new DynamicTexture(map.get(t))));
				}
				return players.get(uuid).get(Type.values()[type]);
			}
			return null; //Nothing exists, return null
		} catch(Exception e) {
			return null;
		}
	}
	
	public static enum Type {
		CAPE;
		
		private String webLocation() {
			return this.toString().toLowerCase() + "s/";
		}
	}
	
}
