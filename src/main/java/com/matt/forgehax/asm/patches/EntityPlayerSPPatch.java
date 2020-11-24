package com.matt.forgehax.asm.patches;

import static com.matt.forgehax.asm.utils.AsmPattern.CODE_ONLY;

import com.matt.forgehax.asm.TypesHook;
import com.matt.forgehax.asm.utils.ASMHelper;
import com.matt.forgehax.asm.utils.AsmPattern;
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

/**
 * Created on 11/13/2016 by fr1kin
 */
public class EntityPlayerSPPatch extends ClassTransformer {
  
  public EntityPlayerSPPatch() {
    super(Classes.EntityPlayerSP);
  }
  
  @RegisterMethodTransformer
  private class ApplyLivingUpdate extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.EntityPlayerSP_onLivingUpdate;
    }
    
    @Inject(description = "Add hook to disable the use slowdown effect")
    public void inject(MethodNode main) {
      AbstractInsnNode applySlowdownSpeedNode =
        ASMHelper.findPattern(
          main.instructions.getFirst(),
          new int[]{IFNE, 0x00, 0x00, ALOAD, GETFIELD, DUP, GETFIELD, LDC, FMUL, PUTFIELD},
          "x??xxxxxxx");
      
      Objects.requireNonNull(
        applySlowdownSpeedNode, "Find pattern failed for applySlowdownSpeedNode");
      
      // get label it jumps to
      LabelNode jumpTo = ((JumpInsnNode) applySlowdownSpeedNode).label;
      
      InsnList insnList = new InsnList();
      insnList.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_isNoSlowDownActivated));
      insnList.add(new JumpInsnNode(IFNE, jumpTo));
      
      main.instructions.insert(applySlowdownSpeedNode, insnList);
    }
  }
  
  @RegisterMethodTransformer
  private class OnUpdate extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.EntityPlayerSP_onUpdate;
    }
    
    @Inject(description = "Add hooks at top and bottom of method")
    public void inject(MethodNode main) {
      // AbstractInsnNode top =
      //    ASMHelper.findPattern(main, INVOKESPECIAL, NONE, NONE, ALOAD, INVOKEVIRTUAL, IFEQ);
      AbstractInsnNode top =
        new AsmPattern.Builder(CODE_ONLY)
          .opcodes(INVOKESPECIAL, ALOAD, INVOKEVIRTUAL, IFEQ)
          .build()
          .test(main)
          .getFirst();
      
      AbstractInsnNode afterRiding = ASMHelper.findPattern(main, GOTO);
      AbstractInsnNode afterWalking =
        ASMHelper.findPattern(main, INVOKESPECIAL, NONE, NONE, NONE, RETURN);
      AbstractInsnNode ret = ASMHelper.findPattern(main, RETURN);
      
      Objects.requireNonNull(top, "Find pattern failed for top node");
      Objects.requireNonNull(afterRiding, "Find pattern failed for afterRiding node");
      Objects.requireNonNull(afterWalking, "Find pattern failed for afterWalking node");
      
      LabelNode jmp = new LabelNode();
      
      InsnList pre = new InsnList();
      pre.add(new VarInsnNode(ALOAD, 0));
      pre.add(
        ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onUpdateWalkingPlayerPre));
      pre.add(new JumpInsnNode(IFNE, jmp));
      
      InsnList postRiding = new InsnList();
      postRiding.add(new VarInsnNode(ALOAD, 0));
      postRiding.add(
        ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onUpdateWalkingPlayerPost));
      
      InsnList postWalking = new InsnList();
      postWalking.add(new VarInsnNode(ALOAD, 0));
      postWalking.add(
        ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onUpdateWalkingPlayerPost));
      
      main.instructions.insert(top, pre);
      main.instructions.insertBefore(afterRiding, postRiding);
      main.instructions.insert(afterWalking, postWalking);
      main.instructions.insertBefore(ret, jmp);
    }
  }
  
  @RegisterMethodTransformer
  private class pushOutOfBlocks extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.EntityPlayerSP_pushOutOfBlocks;
    }
    
    @Inject(description = "Add hook to disable pushing out of blocks")
    public void inject(MethodNode main) {
      AbstractInsnNode preNode = main.instructions.getFirst();
      AbstractInsnNode postNode =
        ASMHelper.findPattern(main.instructions.getFirst(), new int[]{ICONST_0, IRETURN}, "xx");
      
      Objects.requireNonNull(preNode, "Find pattern failed for pre node");
      Objects.requireNonNull(postNode, "Find pattern failed for post node");
      
      LabelNode endJump = new LabelNode();
      
      InsnList insnPre = new InsnList();
      insnPre.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onPushOutOfBlocks));
      insnPre.add(new JumpInsnNode(IFNE, endJump));
      
      main.instructions.insertBefore(preNode, insnPre);
      main.instructions.insertBefore(postNode, endJump);
    }
  }
  
  @RegisterMethodTransformer
  private class RowingBoat extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.EntityPlayerSP_isRowingBoat;
    }
    
    @Inject(description = "Add hook to override returned value of isRowingBoat")
    public void inject(MethodNode main) {
      AbstractInsnNode preNode = main.instructions.getFirst();
      
      Objects.requireNonNull(preNode, "Find pattern failed for pre node");
      
      LabelNode jump = new LabelNode();
      
      InsnList insnPre = new InsnList();
      // insnPre.add(ASMHelper.call(GETSTATIC,
      // TypesHook.Fields.ForgeHaxHooks_isNotRowingBoatActivated));
      // insnPre.add(new JumpInsnNode(IFEQ, jump));
      
      insnPre.add(new InsnNode(ICONST_0));
      insnPre.add(new InsnNode(IRETURN)); // return false
      // insnPre.add(jump);
      
      main.instructions.insert(insnPre);
    }
  }

  @RegisterMethodTransformer
  private class IsUser extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.EntityPlayerSP_isUser;
    }
    
    @Inject(description = "Add hook to allow player rendering in freecam")
    public void inject(MethodNode main) {
      AbstractInsnNode preNode = main.instructions.getFirst();
      
      Objects.requireNonNull(preNode, "Find pattern failed for isUser node");
      
      LabelNode jump = new LabelNode();
      
      InsnList insnList = new InsnList();
      insnList.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_allowDifferentUserForFreecam));
      insnList.add(new JumpInsnNode(IFEQ, jump));
      insnList.add(new InsnNode(ICONST_0));
      insnList.add(new InsnNode(IRETURN));
      insnList.add(jump);
      
      main.instructions.insert(insnList);
    }
  }

  @RegisterMethodTransformer
  private class IsCurrentViewEntity extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.EntityPlayerSP_isCurrentViewEntity;
    }
    
    @Inject(description = "Add hook to allow player movement in freecam")
    public void inject(MethodNode main) {
      AbstractInsnNode preNode = main.instructions.getFirst();
      
      Objects.requireNonNull(preNode, "Find pattern failed for isCurrentViewEntity node");
      
      LabelNode jump = new LabelNode();
      
      InsnList insnList = new InsnList();
      insnList.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_allowMovementInFreecam));
      insnList.add(new JumpInsnNode(IFEQ, jump));
      insnList.add(new InsnNode(ICONST_1));
      insnList.add(new InsnNode(IRETURN));
      insnList.add(jump);
      
      main.instructions.insert(insnList);
    }
  }
}


