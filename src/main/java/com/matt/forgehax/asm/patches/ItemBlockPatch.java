package com.matt.forgehax.asm.patches;

import com.matt.forgehax.asm.TypesHook;
import com.matt.forgehax.asm.utils.ASMHelper;
import com.matt.forgehax.asm.utils.asmtype.ASMMethod;
import com.matt.forgehax.asm.utils.transforming.ClassTransformer;
import com.matt.forgehax.asm.utils.transforming.Inject;
import com.matt.forgehax.asm.utils.transforming.MethodTransformer;
import com.matt.forgehax.asm.utils.transforming.RegisterMethodTransformer;

import java.util.Objects;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

public class ItemBlockPatch extends ClassTransformer {
  
  public ItemBlockPatch() {
    super(Classes.ItemBlock);
  }
  
  @RegisterMethodTransformer
  private class PlaySound extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.ItemBlock_onItemUse;
    }
    
    @Inject(description = "Play sound regardless of placement success")
    public void injectFirst(MethodNode main) {    
      AbstractInsnNode ifSetBlockNode =
        ASMHelper.findPattern(
          main.instructions.getFirst(),
          new int[]{FLOAD, ALOAD},
          "xx");  
      
      Objects.requireNonNull(ifSetBlockNode, "Find pattern failed for ifSetBlockNode");

      for (int i=0; i<3; i++) {
        // LOGGER.warn(String.format("OPCode #%d : %d | %d", i, ifSetBlockNode.getOpcode(), ifSetBlockNode.getType()));
        ifSetBlockNode = ifSetBlockNode.getNext(); // This is shit but no matter what
                                                   //    it won't fucking find the pattern I need
      }
      LabelNode newLabelNode = new LabelNode();
      
      InsnList insnList = new InsnList();
      insnList.add(new IntInsnNode(ISTORE, 20));
      insnList.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_doPreventGhostBlocksPlace));
      insnList.add(new JumpInsnNode(IFNE, newLabelNode)); // if noGhostBlock enabled, jump straight after the IFEQ
      insnList.add(new IntInsnNode(ILOAD, 20));
      // Original IFEQ is here
      
      main.instructions.insertBefore(ifSetBlockNode, insnList);
      main.instructions.insert(ifSetBlockNode, newLabelNode);
    }

    @Inject(description = "Add hook to change block of which placement sound is played")
    public void injectSecond(MethodNode main) {
      AbstractInsnNode getBlockNode =
        ASMHelper.findPattern(
          main.instructions.getFirst(),
          new int[]{ALOAD, INVOKEINTERFACE, ALOAD},
          "xxx");
      
      Objects.requireNonNull(getBlockNode, "Find pattern failed for getBlockNode");
      for (int i = 0; i<6; i++)
        getBlockNode = getBlockNode.getNext();
    
      InsnList insnList = new InsnList();
    
      // insnList.add(new IntInsnNode(ALOAD, 14));
      insnList.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onGetBlockSound));
      // insnList.add(new IntInsnNode(ASTORE, 14));
    
      main.instructions.insert(getBlockNode, insnList); // This is super broken :(
    }
  }
}
