package com.matt.forgehax.mods.services;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.FHTextures;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class FHTexturesMod extends ServiceMod {

	public FHTexturesMod() {
		super("FHTextures");
	}
	
	public static boolean isEnabled = true;

	private final Setting<Boolean> enabled =
		getCommandStub()
		      .builders()
		      .<Boolean>newSettingBuilder()
		      .name("enabled")
		      .description("Enables the mod")
		      .defaultTo(true)
		      .changed(x -> isEnabled = x.getTo())
		      .build();
	
	private Set<UUID> toRemove = new HashSet<>();
	
	@Override
	public void onLoad() {
		getCommandStub()
			.builders()
			.newCommandBuilder()
			.name("site")
			.description("where to upload your textures")
			.processor(c -> {
				Helper.printInform("%s", "upload.2b2t.it");
			})
			.build();
		
		isEnabled = enabled.get();
		FHTextures.updateUsers();
	}
	
	@SubscribeEvent
	public void PlayerJoinLeave(PacketEvent.Incoming.Pre event) {
		if(event.getPacket() instanceof SPacketPlayerListItem) {
			SPacketPlayerListItem packet = event.getPacket();
			for(SPacketPlayerListItem.AddPlayerData data : packet.getEntries()) {
		
				UUID uuid = data.getProfile().getId();
				
				if(!FHTextures.isRegisteredUser(uuid)) {
					continue;
				}
				
				if(packet.getAction() == SPacketPlayerListItem.Action.ADD_PLAYER) {
					FHTextures.addPlayer(uuid);
				} else if(packet.getAction() == SPacketPlayerListItem.Action.REMOVE_PLAYER) {
					synchronized(toRemove) {
						toRemove.add(uuid);
					}
				}
				
			}
		}
	}
	
	@SubscribeEvent
	public void onDraw(GuiScreenEvent.DrawScreenEvent event) { //Called here because it needs to be in the render thread
		synchronized(toRemove) {
			Iterator<UUID> iter = toRemove.iterator();
			while(iter.hasNext()) {
				UUID uuid = iter.next();
				iter.remove();
				FHTextures.removePlayer(uuid);
			}
		}
	}
	
	@SubscribeEvent
	public void onDisconnect(WorldEvent.Unload event) {
		
		FHTextures.updateUsers();
		
		if(MC.world == null) {
			synchronized(toRemove) {
				FHTextures.RegisteredUsers().forEach(x -> toRemove.add(x));
			}
		}
		
	}
}
