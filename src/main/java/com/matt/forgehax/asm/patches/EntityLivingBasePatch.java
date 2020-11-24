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

public class EntityLivingBasePatch extends ClassTransformer {
  
  public EntityLivingBasePatch() {
    super(Classes.EntityLivingBase);
  }
  
  @RegisterMethodTransformer
  public class Travel extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.EntityLivingBase_travel;
    }
    



    @Inject(description = "Add hook before first slippery motion calculation")
    public void injectFirst(MethodNode node) {
      // at first underState.getBlock().getSlipperiness(...)
      AbstractInsnNode first =
        ASMHelper.findPattern(
          node,
          INVOKEVIRTUAL,
          LDC,
          FMUL,
          FSTORE,
          NONE,
          NONE,
          NONE,
          LDC,
          FLOAD,
          FLOAD,
          FMUL,
          FLOAD,
          FMUL,
          FDIV,
          FSTORE,
          NONE,
          NONE);
      
      Objects.requireNonNull(first, "Could not find first slip motion node");
      
      InsnList list = new InsnList();
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(new VarInsnNode(ALOAD, 6));
      list.add(new InsnNode(ICONST_0));
      list.add(
        ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onEntityBlockSlipApply));
      // top of stack should be a modified or unmodified slippery float
      
      node.instructions.insert(first, list); // insert after
    }
    
    @Inject(description = "Add hook before second slippery motion calculation")
    public void injectSecond(MethodNode node) {
      // at second underState.getBlock().getSlipperiness(...)
      AbstractInsnNode second =
        ASMHelper.findPattern(
          node,
          INVOKEVIRTUAL,
          LDC,
          FMUL,
          FSTORE,
          NONE,
          NONE,
          NONE,
          ALOAD,
          INVOKEVIRTUAL,
          IFEQ,
          NONE,
          NONE,
          LDC,
          FSTORE,
          NONE,
          NONE,
          ALOAD,
          ALOAD,
          GETFIELD,
          LDC,
          LDC,
          INVOKESTATIC,
          PUTFIELD);
      
      Objects.requireNonNull(second, "Could not find second slip motion node");
      
      InsnList list = new InsnList();
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(new VarInsnNode(ALOAD, 8));
      list.add(new InsnNode(ICONST_1));
      list.add(
        ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onEntityBlockSlipApply));
      // top of stack should be a modified or unmodified slippery float
      
      node.instructions.insert(second, list); // insert after
    }

    @Inject(description = "Add hook before changing base speed if entity is not on ground")
    public void injectThird(MethodNode main) {
      // Between 2 getSlipperiness, the onGround check for AIMoveSpeed
      AbstractInsnNode isOnGroundNode =
      ASMHelper.findPattern(
        main.instructions.getFirst(),
        new int[]{ALOAD, GETFIELD, IFEQ, 0x00, 0x00, ALOAD, INVOKEVIRTUAL, FLOAD, FMUL, FSTORE},
        "xxx??xxxxx");

      Objects.requireNonNull(isOnGroundNode, "Could not find isOnGround check node");
      AbstractInsnNode after = isOnGroundNode.getNext().getNext();
      
      LabelNode jump = new LabelNode();
  
      InsnList list = new InsnList();
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onEntityGroundCheck));
      list.add(new JumpInsnNode(IFNE, jump));
      // top of stack should be a modified or unmodified slippery float
      
      main.instructions.insertBefore(isOnGroundNode, list);
      main.instructions.insert(after, jump); // insert after
    }

    @Inject(description = "Add hook before multiplying for slipperiness if entity is on ground")
    public void injectFourth(MethodNode main) {
      // Between 2 getSlipperiness, the onGround check for AIMoveSpeed
      AbstractInsnNode isOnGroundNode =
        ASMHelper.findPattern(
          main.instructions.getFirst(),
          new int[]{ALOAD, GETFIELD, IFEQ, 0x00, 0x00, ALOAD, GETFIELD, ALOAD, ALOAD,
            GETFIELD, ALOAD, INVOKEVIRTUAL, GETFIELD, DCONST_1, DSUB},
          "xxx??xxxxxxxxxx");

      Objects.requireNonNull(isOnGroundNode, "Could not find isOnGround check node");
      AbstractInsnNode after = isOnGroundNode.getNext().getNext();
      
      LabelNode jump = new LabelNode();
  
      InsnList list = new InsnList();
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onEntityGroundCheck));
      list.add(new JumpInsnNode(IFNE, jump));
      // top of stack should be a modified or unmodified slippery float
      
      main.instructions.insertBefore(isOnGroundNode, list);
      main.instructions.insert(after, jump); // insert after
      // for (AbstractInsnNode i : main.instructions.toArray())
      //   LOGGER.warn(insnToString(i));
    }

    @Inject(description = "Add hook before multiplying for slipperiness if entity is on ground the first time")
    public void injectFifth(MethodNode main) {
      // Between 2 getSlipperiness, the onGround check for AIMoveSpeed
      AbstractInsnNode isOnGroundNode =
      ASMHelper.findPattern(
        main.instructions.getFirst(),
        new int[]{ALOAD, GETFIELD, IFEQ, 0x00, 0x00, ALOAD, GETFIELD, ALOAD, INVOKEVIRTUAL,
          ASTORE, 0x00, 0x00, ALOAD, INVOKEINTERFACE, ALOAD, ALOAD, GETFIELD},
        "xxx??xxxxx??xxxxx");

      Objects.requireNonNull(isOnGroundNode, "Could not find isOnGround check node");
      AbstractInsnNode after = isOnGroundNode.getNext().getNext();
      
      LabelNode jump = new LabelNode();
  
      InsnList list = new InsnList();
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onEntityGroundCheck));
      list.add(new JumpInsnNode(IFNE, jump));
      // top of stack should be a modified or unmodified slippery float
      
      main.instructions.insertBefore(isOnGroundNode, list);
      main.instructions.insert(after, jump); // insert after
    }

    // Tonio
    @Inject(description = "Add hook before messing with player motions because he's elytra flying")
    public void injectSixth(MethodNode main) {
      AbstractInsnNode beforeMotionsNode =
      ASMHelper.findPattern(
        main.instructions.getFirst(),
        new int[]{ALOAD, DUP, GETFIELD, LDC, FLOAD, F2D, LDC, DMUL},
                    "xxxxxxxx");

      Objects.requireNonNull(beforeMotionsNode, "Could not find node before elytra drift motion");

      AbstractInsnNode afterNode =
      ASMHelper.findPattern(
        main.instructions.getFirst(),
        new int[]{ALOAD, GETSTATIC, ALOAD, GETFIELD, ALOAD, GETFIELD, ALOAD, GETFIELD, INVOKEVIRTUAL },
                    "xxxxxxxxx");

      Objects.requireNonNull(afterNode, "Could not find node after elytra drift motion");
      
      LabelNode jump = new LabelNode();
  
      InsnList list = new InsnList();
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onElytraFlying));
      list.add(new JumpInsnNode(IFNE, jump));
      // top of stack should be a modified or unmodified slippery float
      
      main.instructions.insertBefore(beforeMotionsNode, list);
      main.instructions.insertBefore(afterNode, jump); // insert after
    }
  }
}
