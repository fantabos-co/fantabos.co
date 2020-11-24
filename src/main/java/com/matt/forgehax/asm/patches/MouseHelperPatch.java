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
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Created on 01/10/2020 by tonio
 */
public class MouseHelperPatch extends ClassTransformer {
  
  public MouseHelperPatch() {
    super(Classes.MouseHelper);
  }
  
  @RegisterMethodTransformer
  private class FireEventWhenGettingInputs extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.MouseHelper_mouseXYChange;
    }
    
    @Inject(description = "Add hook to fire an event upon updating the mouse input")
    public void inject(MethodNode main) {
      AbstractInsnNode lastNode = main.instructions.getLast().getPrevious();
      
      Objects.requireNonNull(lastNode, "Find pattern failed for end of updatePlayerMoveState");
      
      InsnList insnList = new InsnList();
      insnList.add(new VarInsnNode(ALOAD, 0));
      insnList.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onUpdateMouseState));
      
      main.instructions.insertBefore(lastNode, insnList);
    }
  }
}


