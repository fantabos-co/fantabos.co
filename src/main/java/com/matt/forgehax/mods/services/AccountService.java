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
  // private Field token;
  private boolean enabled;
  private AuthHelper auth = new AuthHelper();

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
      // token = YggdrasilUserAuthentication.class.getDeclaredField("accessToken");
      enabled = true;
    } catch (NoSuchMethodError e) {
      LOGGER.warn("Could not find session field, cannot change session");
      enabled = false;
    /// } catch (NoSuchFieldException e) {
    ///   LOGGER.warn("Could not find token field, cannot change session");
    ///   enabled = false;
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
              setSession(originalSession);
              Helper.printInform("Back to original player");
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
                boolean res = auth.login(username, password);
                if (res) Helper.printError("Succesfully logged in");
                else Helper.printWarning("Login failed");
              } catch (AuthenticationException e) {
                Helper.printError("Could not login as %s", username);
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
              try {
                boolean res = auth.login(user_db.get(alias), pwd_db.get(alias));
                if (res) Helper.printInform("Logged in as %s", alias);
                else Helper.printWarning("Failed to login as %s", alias);
              } catch (AuthenticationException e) {
                Helper.printError("Could not login as %s", alias);
              }
            })
        .build();
    }
  }

  private void setSession(Session s) {
    try {
        session.set(MC, s);
    } catch (IllegalAccessException e) {
        throw new RuntimeException("Failed Reflective Access", e);
    }
}

  private final class AuthHelper extends YggdrasilUserAuthentication {

    /* Thanks ReAuth! https://github.com/TechnicianLP/ReAuth */

    private YggdrasilAuthenticationService loginService;
    private boolean returnLoginService = false;

    public AuthHelper() {
      super(new YggdrasilAuthenticationService(MC.getProxy(), null), Agent.MINECRAFT);
      loginService = new YggdrasilAuthenticationService(MC.getProxy(), UUID.randomUUID().toString());
    }

    @Override
    public YggdrasilAuthenticationService getAuthenticationService() {
        if (returnLoginService) {
            return loginService;
        }
        return super.getAuthenticationService();
    }

    public boolean login(String user, String password) throws AuthenticationException {
      boolean result = false;
      setUsername(user);
      setPassword(password);
      try {
        returnLoginService = true;
        logInWithPassword(); // Use a different loginService to provide a new client token!
        returnLoginService = false;
        Session session = new Session(getSelectedProfile().getName(),
                              UUIDTypeAdapter.fromUUID(getSelectedProfile().getId()),
                              getAuthenticatedToken(), getUserType().getName());
        session.setProperties(getUserProperties());
        logOut();
        setSession(session);
        result = true;
      } catch (AuthenticationException e) {
        LOGGER.warn("Login failed : " + e.getMessage());
      } finally {
        returnLoginService = false;
        logOut();
      }
      return result;
    }
  }
}
