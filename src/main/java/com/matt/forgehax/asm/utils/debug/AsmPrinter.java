package com.matt.forgehax.asm.utils.debug;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import com.matt.forgehax.Globals;

public class AsmPrinter {

  private static Path getFilePath(String name) {
    try { // I don't use handy things from Helper because importing it may cause a Circular Import exception in dev env
      return Paths.get("forgehax/asm/" + name + ".asm");
    } catch (RuntimeException e) {
      e.printStackTrace();
      return Paths.get(name + ".asm");
    }
  }

  private static Printer printer = new Textifier();
  private static TraceMethodVisitor mp = new TraceMethodVisitor(printer); 
  
  public static void printAsmMethod(final MethodNode main) {
    for (AbstractInsnNode i : main.instructions.toArray())
      System.out.print(insnToString(i));
  }
  
  public static void logAsmMethod(final MethodNode main) {
	    for (AbstractInsnNode i : main.instructions.toArray())
	      Globals.LOGGER.debug(insnToString(i));
  }

  public static void logAsmMethod(final MethodNode main, String name) {
    StringBuilder out = new StringBuilder();
    for (AbstractInsnNode i : main.instructions.toArray())
      out.append(insnToString(i));
    try {
    	Files.write(getFilePath(name), out.toString().getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String insnToString(AbstractInsnNode insn) {
    insn.accept(mp);
    StringWriter sw = new StringWriter();
    printer.print(new PrintWriter(sw));
    printer.getText().clear();
    return sw.toString();
  }
}