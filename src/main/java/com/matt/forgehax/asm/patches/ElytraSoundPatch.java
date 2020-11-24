package com.matt.forgehax.asm.patches;

import com.matt.forgehax.asm.utils.asmtype.ASMMethod;

import com.matt.forgehax.asm.TypesHook;
import com.matt.forgehax.asm.utils.ASMHelper;
import com.matt.forgehax.asm.utils.transforming.ClassTransformer;
import com.matt.forgehax.asm.utils.transforming.Inject;
import com.matt.forgehax.asm.utils.transforming.MethodTransformer;
import com.matt.forgehax.asm.utils.transforming.RegisterMethodTransformer;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

public class ElytraSoundPatch extends ClassTransformer {

  public ElytraSoundPatch() {
    super(Classes.ElytraSound);
  }

  @RegisterMethodTransformer
  private class setChatLinePatch extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.ElytraSound_update;
    }
    
    @Inject(description = "Add check to prevent elytra sound update")
    public void inject(MethodNode main) {
      InsnList list = new InsnList();

      LabelNode skip = new LabelNode();

      list.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_preventElytraSoundUpdate));
      list.add(new JumpInsnNode(IFEQ, skip));
      list.add(new InsnNode(RETURN));
      list.add(skip);

      main.instructions.insert(list); // add to method start
    }
  }
}