package com.matt.forgehax.mods.services;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.matt.forgehax.Helper;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.util.Session;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;


@RegisterMod
public class AccountService extends ServiceMod {
  
  public AccountService() {
    super("Accounts");
  }

  private Field session;
  private boolean enabled;
  private Authenticator auth = new Authenticator();

  private Session originalSession;

  // I deliberately don't want to serialize this, so sharing FH settings/instances will never be risky
  private Map<String, String> user_db = new HashMap<String, String>();
  private Map<String, String> pwd_db = new HashMap<String, String>();
  // I still may use an Options type, but 2 maps should do the trick for now

  @Override
  protected void onLoad() {
    super.onLoad();
    try {
      session = ObfuscationReflectionHelper.findField(MC.getClass(), "field_71449_j");
      enabled = true;
    } catch (NoSuchMethodError e) {
      LOGGER.warn("Could not find session field, cannot change session");
      enabled = false;
    }

    if (enabled) {
      try {
        originalSession = (Session) session.get(MC);
        getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("restore")
        .description("Switch back to original session")
        .processor(
            data -> {
              try {
                session.set(MC, originalSession);
                Helper.printInform("Back to original player");
              } catch (IllegalAccessException e) {
                Helper.printError("Could not restore original session");
                LOGGER.error(String.format("Failed Reflection access for restoring session : %s", e.getMessage()));
              }  
            })
        .build();
      } catch (IllegalAccessException e) {
        LOGGER.warn("Could not save the initial session");
      }
      getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("login")
        .description("Re-login with provided user and password")
        .processor(
            data -> {
              data.requiredArguments(2);
              final String username = data.getArgumentAsString(0);
              final String password = data.getArgumentAsString(1);
              try {
                auth.login(username, password);
                Helper.printError("Succesfully logged in");
              } catch (AuthenticationException e) {
                Helper.printError("Could not login as %s", username);
                LOGGER.error("Failed logging in, recheck credentials?");
              } catch (IllegalAccessException e) {
                Helper.printError("Failed to change active session");
                LOGGER.error(String.format("Failed setting new session : %s", e.getMessage()));
              }
            })
        .build();
      
      getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("save")
        .description("<alias> <user> <pwd> | save is per session")
        .processor(
            data -> {
              data.requiredArguments(3);
              final String alias = data.getArgumentAsString(0);
              final String username = data.getArgumentAsString(1);
              final String password = data.getArgumentAsString(2);
              user_db.put(alias, username);
              pwd_db.put(alias, password);
              Helper.printInform("Saved new alt: %s", alias);
            })
        .build();

      getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("switch")
        .description("Switch to a saved user")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String alias = data.getArgumentAsString(0);
              if (user_db.get(alias) == null) {
                Helper.printWarning("No account saved as %s", alias);
              } else {
                try {
                  auth.login(user_db.get(alias), pwd_db.get(alias));
                  Helper.printInform("Logged in as %s", alias);
                } catch (AuthenticationException e) {
                  Helper.printError("Could not login as %s", alias);
                  LOGGER.error(String.format("Could not login as %s : %s", alias, e.getMessage()));
                } catch (IllegalAccessException e) {
                  Helper.printError("Failed to change active session");
                  LOGGER.error(String.format("Failed setting new session as %s : %s", alias, e.getMessage()));
                }
              }
            })
        .build();
    }
  }

  private final class Authenticator extends YggdrasilUserAuthentication {

    /* Thanks ReAuth for showing how to do it https://github.com/TechnicianLP/ReAuth */

    public Authenticator() {
      super(new YggdrasilAuthenticationService(MC.getProxy(), null), Agent.MINECRAFT);
      alt_login = new YggdrasilAuthenticationService(MC.getProxy(), UUID.randomUUID().toString());
    }

    // I need to use a new ClientToken otherwise the MC Auth service won't know how to handle a UUID change
    private YggdrasilAuthenticationService alt_login = null;
    
    @Override
    public YggdrasilAuthenticationService getAuthenticationService() { return alt_login; }

    public void login(String usr, String pwd) throws AuthenticationException, IllegalAccessException {
      setUsername(usr);
      setPassword(pwd);
      logInWithPassword();
      Session sess = new Session(getSelectedProfile().getName(),
                            UUIDTypeAdapter.fromUUID(getSelectedProfile().getId()),
                            getAuthenticatedToken(), getUserType().getName());
      sess.setProperties(getUserProperties());
      logOut();
      session.set(MC, sess);
    }
  }
}
