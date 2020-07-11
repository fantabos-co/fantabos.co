package com.matt.forgehax.asm.patches;

import com.matt.forgehax.asm.utils.asmtype.ASMMethod;

import java.util.Objects;

import com.matt.forgehax.asm.TypesHook;
import com.matt.forgehax.asm.utils.ASMHelper;
import com.matt.forgehax.asm.utils.transforming.ClassTransformer;
import com.matt.forgehax.asm.utils.transforming.Inject;
import com.matt.forgehax.asm.utils.transforming.MethodTransformer;
import com.matt.forgehax.asm.utils.transforming.RegisterMethodTransformer;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

public class LayerRenderPatch extends ClassTransformer {

  public LayerRenderPatch() {
    super(Classes.LayerArmorBase);
  }

  @RegisterMethodTransformer
  private class doRenderLayer extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.LayerArmorBase_doRenderLayer;
    }
    
    @Inject(description = "Set armor render events as cancellable")
    public void inject(MethodNode main) {
      AbstractInsnNode node =
        ASMHelper.findPattern(main.instructions.getFirst(),
          new int[]{ ALOAD, ALOAD, FLOAD, FLOAD },
          "xxxx");
      
      Objects.requireNonNull(node, "Find pattern failed for node");
      
      InsnList insnList = new InsnList();
      
      insnList.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_preventArmorRendering));
      final LabelNode jmp = new LabelNode();
      insnList.add(new JumpInsnNode(IFEQ, jmp));
      insnList.add(new InsnNode(RETURN));
      insnList.add(jmp);
      
      main.instructions.insertBefore(node, insnList);
    }
  }
}