package com.matt.forgehax.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import static com.matt.forgehax.Helper.printError;
import static com.matt.forgehax.Helper.printInform;

/**
 * Created on 5/30/2017 by fr1kin
 */
public class FileManager {
  
  private static final FileManager INSTANCE = new FileManager();
  
  public static FileManager getInstance() {
    return INSTANCE;
  }
  
  private static String[] expandPath(String fullPath) {
    return fullPath.split(":?\\\\\\\\|\\/");
  }
  
  private static Stream<String> expandPaths(String... paths) {
    return Arrays.stream(paths).map(FileManager::expandPath).flatMap(Arrays::stream);
  }
  
  private static Path lookupPath(Path root, String... paths) {
    return Paths.get(root.toString(), paths);
  }
  
  private static Path getRoot() {
    return Paths.get("");
  }
  
  private static void createDirectory(Path dir) {
    try {
      if (!Files.isDirectory(dir)) {
        if (Files.exists(dir)) {
          Files.delete(dir); // delete if it exists but isn't a directory
        }
        
        Files.createDirectories(dir);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private static Path getMkDirectory(Path parent, String... paths) {
    if (paths.length < 1) {
      return parent;
    }
    
    Path dir = lookupPath(parent, paths);
    createDirectory(dir);
    return dir;
  }
  
  private final Path base;
  
  private FileManager() {
    base = getMkDirectory(getRoot(), "forgehax");
    // create directories for these common
    getMkDirectory(base, "config");
    getMkDirectory(base, "cache");
    getMkDirectory(base, "asm");
    getMkDirectory(base, "friends");
    getMkDirectory(base, "irc");
  }
  
  public Path getBasePath() {
    return base;
  }
  
  public Path getBaseResolve(String... paths) {
    String[] names = expandPaths(paths).toArray(String[]::new);
    if (names.length < 1) {
      throw new IllegalArgumentException("missing path");
    }
    
    return lookupPath(getBasePath(), names);
  }
  
  public Path getMkBaseResolve(String... paths) {
    Path path = getBaseResolve(paths);
    createDirectory(path.getParent());
    return path;
  }
  
  public Path getConfig() {
    return getBasePath().resolve("config");
  }
  
  public Path getCache() {
    return getBasePath().resolve("cache");
  }

  public Path getAsm() {
    return getBasePath().resolve("asm");
  }
  
  public Path getMkBaseDirectory(String... names) {
    return getMkDirectory(
        getBasePath(), expandPaths(names).collect(Collectors.joining(File.separator)));
  }
  
  public Path getMkConfigDirectory(String... names) {
    return getMkDirectory(
        getConfig(), expandPaths(names).collect(Collectors.joining(File.separator)));
  }

  public Path getMkAsmDirectory(String... names) {
    return getMkDirectory(
        getAsm(), expandPaths(names).collect(Collectors.joining(File.separator)));
  }

  @Nullable
  public JsonObject getConfigObject(String fname) {
    File from = getConfig().resolve(fname).toFile();
    return load(from);
  }

  public void saveConfigObject(String fname, JsonObject in) {
    File to = getConfig().resolve(fname).toFile();
    save(to, in);
  }

  @Nullable
  public static JsonObject load(final File file) {
    try (Reader reader = new InputStreamReader(
              new FileInputStream(file.getAbsolutePath()), "UTF-8")) {
      JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonObject();
      return jsonObject;
    } catch (IOException e) {
      e.printStackTrace();
      printError("Failed to load the requested file.");
      printInform("The exception is: " + e.getMessage());
    }
    return null;
  }

  public static void save(final File file, final JsonObject jsonObject) {
    try (Writer writer = new OutputStreamWriter(
                  new FileOutputStream(file.getAbsolutePath()), "UTF-8")) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      gson.toJson(jsonObject, writer);
    } catch (IOException e) {
      e.printStackTrace();
      printError("Failed to save the submitted data.");
      printInform("The exception is: " + e.getMessage());
    }
  }
}
