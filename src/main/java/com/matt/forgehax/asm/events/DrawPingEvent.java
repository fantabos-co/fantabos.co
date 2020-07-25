package com.matt.forgehax.asm.events;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Created by Tonio on 19/07/2020.
 */
@Cancelable
public class DrawPingEvent extends Event {
  
  private NetworkPlayerInfo player;
  private int x1, x2, y;
  
  public DrawPingEvent(int x1, int x2, int y, NetworkPlayerInfo player) {
    this.x1 = x1;
    this.x2 = x2;
    this.y = y;
    this.player = player;
  }
  
  public int getX1() { return x1; }
  public int getX2() { return x2; }
  public int getY() { return y; }
  public NetworkPlayerInfo getPlayer() { return player; }
}
