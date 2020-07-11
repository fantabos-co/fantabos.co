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
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class PlayerControllerMCPatch extends ClassTransformer {
  
  public PlayerControllerMCPatch() {
    super(Classes.PlayerControllerMP);
  }
  
  @RegisterMethodTransformer
  public class SyncCurrentPlayItem extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.PlayerControllerMC_syncCurrentPlayItem;
    }
    
    @Inject(description = "Add callback at top of method")
    public void inject(MethodNode node) {
      InsnList list = new InsnList();
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onPlayerItemSync));
      
      node.instructions.insert(list);
    }
  }
  
  @RegisterMethodTransformer
  public class AttackEntity extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.PlayerControllerMC_attackEntity;
    }
    
    @Inject(description = "Add callback at top of method")
    public void inject(MethodNode node) {
      InsnList list = new InsnList();
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(new VarInsnNode(ALOAD, 1));
      list.add(new VarInsnNode(ALOAD, 2));
      list.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onPlayerAttackEntity));
      
      node.instructions.insert(list);
    }
  }
  
  @RegisterMethodTransformer
  public class OnPlayerDamageBlock extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.PlayerControllerMC_onPlayerDamageBlock;
    }
    
    @Inject(description = "Add callback at top of method")
    public void inject(MethodNode node) {
      InsnList list = new InsnList();
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(new VarInsnNode(ALOAD, 1));
      list.add(new VarInsnNode(ALOAD, 2));
      list.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onPlayerBreakingBlock));
      
      node.instructions.insert(list);
    }
  }
  
  @RegisterMethodTransformer
  public class OnStoppedUsingItem extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.PlayerControllerMC_onStoppedUsingItem;
    }
    
    @Inject(description = "Add callback at top of method")
    public void inject(MethodNode node) {
      AbstractInsnNode last =
        ASMHelper.findPattern(node.instructions.getFirst(), new int[]{RETURN}, "x");
      
      Objects.requireNonNull(last, "Could not find RET opcode");
      
      LabelNode label = new LabelNode();
      
      InsnList list = new InsnList();
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(new VarInsnNode(ALOAD, 1));
      list.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onPlayerStopUse));
      list.add(new JumpInsnNode(IFNE, label));
      
      node.instructions.insert(list);
      node.instructions.insertBefore(last, label);
    }
  }

  // Tonio
  @RegisterMethodTransformer
  private class onPlayerDestroyBlock extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.PlayerControllerMC_onPlayerDestroyBlock;
    }
    
    @Inject(description = "Add patch to cancel local player destry block events")
    public void inject(MethodNode main) {
      AbstractInsnNode node =
        ASMHelper.findPattern(main.instructions.getFirst(),
          new int[]{ ALOAD, GETFIELD, INVOKEVIRTUAL },
          "xxx");
      
      Objects.requireNonNull(node, "Find pattern failed for node");
      
      InsnList insnList = new InsnList();
      
      insnList.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_doPreventGhostBlocksBreak));
      final LabelNode jmp = new LabelNode();
      insnList.add(new JumpInsnNode(IFEQ, jmp));
      insnList.add(new InsnNode(ICONST_0)); // return false
      insnList.add(new InsnNode(IRETURN));
      insnList.add(jmp);
      
      main.instructions.insertBefore(node, insnList);
    }
  }
}
