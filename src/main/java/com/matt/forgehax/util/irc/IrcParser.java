package com.matt.forgehax.util.irc;

public class IrcParser {
  
  public static String parseIRCjoin(String msgIn) {
    try {
      String channel = "#" + msgIn.split(":#")[1];
      return msgIn.split("!", 2)[0].substring(1) + " joined " + channel;
    } catch (Exception e) {
      e.printStackTrace();
      return msgIn;
    }
  }
  
  public static String parseIRCleave(String msgIn) {
    try {
      String buf;
    
      if (msgIn.contains("QUIT")) {
        buf = msgIn.split("!", 2)[0].substring(1) + " disconnected: ";
        buf += msgIn.split("QUIT", 2)[1].substring(2);
      } else {
        String channel = "#" + msgIn.split("#")[1];
        buf = msgIn.split("!", 2)[0].substring(1) + " left " + channel;
      }
    
      return buf;
    } catch (Exception e) {
      e.printStackTrace();
      return msgIn;
    }
  }
  
  public static String parseIRCnickChange(String msgIn) {
    try {
      return msgIn.replace(":", "").replace("NICK", "changed their nick to");
    } catch (RuntimeException e) {
      e.printStackTrace();
      return msgIn;
    }
  }
}