package com.matt.forgehax.mods.services;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.matt.forgehax.Helper;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.SafeConverter;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.util.Tuple;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;

@RegisterMod
public class AtlasService extends ServiceMod {

	/**
	 * TheAlphaEpsilon
	 * 6NOV2020
	 * Grab data from 2b2tatlas.com for use in epic way points
	 */
	public AtlasService() {
		super("AtlasService", "2b2tatlas.com");
	}
	
	private static final String url = "https://2b2tatlas.com/api/locations.php";
	private static final SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		
	//Thanks to TacticalFaceplant for keeping me from being stupid and just sticking with the basics
	private static final List<Location> owLocs = new ArrayList<>();
	private static final List<Location> endLocs = new ArrayList<>();
	
	//TODO: Put hash back in hashmap, need some kind of hash thats the same for substrings and
	//Parent strings. Or just iterate through all of the keys like with the lists... Probably
	//The latter
	private static final Map<String, Location> nameToLocation = new TreeMap<>();
	private static final Map<String, Set<Location>> tagToLocations = new TreeMap<>();
	
	@Override
	public void onLoad() {
		
		new Thread(() -> {

			try {
	        URLConnection con = new URL(url).openConnection();
	        con.setRequestProperty("User-Agent", ""); //Webserver required a useragent
				  con.connect();
				  InputStream is = con.getInputStream();
	        JsonParser jsonParser = new JsonParser();
	        JsonElement json = jsonParser.parse(new InputStreamReader(is, "UTF-8"));
	        is.close();
				
	        json.getAsJsonArray().forEach(jobj -> {
	        	if(!jobj.isJsonObject()) return;

	        	JsonObject jo = jobj.getAsJsonObject();
	        	Location l = new Location();
				  	
	        	JsonElement e;

	        	e = jo.get("location_uuid");
	        	l.uuid = UUIDTypeAdapter.fromString(e == null || e.isJsonNull() ? "0" : e.getAsString());
				
	        	e = jo.get("name");
	        	l.name = (e == null || e.isJsonNull() ? "" : e.getAsString());
	        	if(l.name.trim().isEmpty()) l.name = "[No Name]";

	        	e = jo.get("description");
	        	l.desc = (e == null || e.isJsonNull() ? "" : e.getAsString().replace("\r",  ""));
	        	if(l.desc.trim().isEmpty()) l.desc = "[No Description]";

	        	e = jo.get("tags");
	        	l.tags = (e == null || e.isJsonNull() ? "" : e.getAsString());
	        	if(l.tags.trim().isEmpty()) l.tags = "[No Tags]";

	        	e = jo.get("wiki");
	        	l.wikiURL = (e == null || e.isJsonNull() ? "" : e.getAsString());
	        	
	        	e = jo.get("video_url");
	        	l.videoURL = (e == null || e.isJsonNull() ? "" : e.getAsString());
	
	        	e = jo.get("x");
	        	l.xCoord = (e == null || e.isJsonNull() ? 0 : SafeConverter.toInteger(e.getAsString(), 0));
	        	
	        	e = jo.get("y");
	        	l.yCoord = (e == null || e.isJsonNull() ? 0 : SafeConverter.toInteger(e.getAsString(), 0));
	        	
	        	e = jo.get("z");
	        	l.zCoord = (e == null || e.isJsonNull() ? 0 : SafeConverter.toInteger(e.getAsString(), 0));
	        	
	        	l.location = new Vec3d(l.xCoord, l.yCoord, l.zCoord);
	        	
	        	e = jo.get("time_added");
	        	if (e == null || e.isJsonNull()) {
	        		l.dateAdded = Date.from(Instant.now());
	        	} else {
	        		try {
	        			l.dateAdded = dateParser.parse(e.getAsString());
	        		} catch (Exception ex) {
	        			// ignore
	        			ex.printStackTrace();
	        			l.dateAdded = Date.from(Instant.now());
	        		}
	        	}
				  	
	        	e = jo.get("end_dimension");
	        	l.isEnd = (e == null || e.isJsonNull() ? false : e.getAsString().equals("1"));
				  	
	        	if (l.isEnd) endLocs.add(l);
	        	else owLocs.add(l);
	        	
	        	nameToLocation.put(l.name.toLowerCase(), l);
	        	for(String tag : l.tags.split(", ")) {
	        		tag = tag.toLowerCase();
	        		tagToLocations.putIfAbsent(tag, new HashSet<Location>());
	        		tagToLocations.get(tag).add(l);
	        	}
	        	
	        });
	        
	        Helper.LOGGER.info("[ATLAS] Got " + (endLocs.size() + owLocs.size()) + " atlas locations");
				
			} catch (Exception e) {
				Helper.LOGGER.error("[ATLAS] Couldn't download Atlas locations: " + e.getMessage());
			}
	        
		}).start();
		
	}
	
	public static Collection<Location> searchFor(String s) {
		s = s.toLowerCase();
		Set<Location> toReturn = new LinkedHashSet<>(); //Keep da order
		for(String key : tagToLocations.keySet()) {
			if(key.contains(s)) {
				tagToLocations.get(key).forEach(x -> toReturn.add(x));
			}
		}
		for(String key : nameToLocation.keySet()) {
			if(key.contains(s)) {
				toReturn.add(nameToLocation.get(key));
			}
		}
		return toReturn;
	}
	
	/**
	 * 
	 * Returns a tuple of the list of locations sorted furthest -> nearest
	 * and the distance the player should move before updating the list again
	 * if there is no next furthest location, returns a arbitrarily large int
	 * TODO: Make the distance to travel better
	 */
	public static Tuple<List<Location>, Integer> getNearest(Vec3d location, int k, DimensionType type) {
		
		List<Location> list;
		switch(type) {
		case OVERWORLD:
			list = owLocs;
			break;
		case THE_END:
			list = endLocs;
			break;
		default:
			return new Tuple<>(Collections.EMPTY_LIST, Integer.MAX_VALUE);
		}
		Collections.sort(list, nearest(location));
		int max = Math.min(k, list.size());
		List<Location> toReturn = new ArrayList<>();
		for(int i = 0; i < max; i++) {
			toReturn.add(list.get(i));
		}
		Collections.sort(toReturn, Collections.reverseOrder(nearest(location)));
		
		if(toReturn.isEmpty()) { //IDK What i did here
			return new Tuple<>(toReturn, Integer.MAX_VALUE);
		}
		double distToNextFurthest = k + 1 >= list.size() ? Integer.MAX_VALUE : list.get(k + 1).location.distanceTo(location);
		double distToFurthest = toReturn.get(toReturn.size() - 1).location.distanceTo(location);
		return new Tuple<>(toReturn, (int)(Math.abs(distToNextFurthest - distToFurthest) / 2F));
	}
	
	private static Comparator<Location> nearest(Vec3d location) {
		return new Comparator<Location>() {
			private Vec3d loc = location;
			@Override
			public int compare(Location o1, Location o2) {
				return (int) (o1.location.distanceTo(loc) - o2.location.distanceTo(loc));
			}
		};
	}
	
	public static class Location {
		private UUID uuid;
		private String name;
		private int xCoord;
		private int yCoord;
		private int zCoord;
		private Vec3d location;
		private String desc;
		private String tags;
		private Date dateAdded;
		private String wikiURL;
		private String videoURL;
		private boolean isEnd;
		
		public String getName() {
			return name;
		}
		
		public int getX() {
			return xCoord;
		}
		
		public int getY() {
			return yCoord;
		}
		
		public int getZ() {
			return zCoord;
		}
		
		public String getDescription() {
			return desc;
		}
		
		public Date getDateAdded() {
			return (Date) dateAdded.clone();
		}
		
		public String getWikiAddress() {
			return wikiURL;
		}
		
		public String getVideoAddress() {
			return videoURL;
		}
		
		@Override
		public boolean equals(Object other) {
			if(other == this) {
				return true;
			} else if(!(other instanceof Location)) {
				return false;
			} else {
				Location o = (Location) other;
				return this.uuid.equals(o.uuid);
			}
		}
	}

}
