package com.matt.forgehax.asm.patches;

import com.matt.forgehax.asm.TypesHook;
import com.matt.forgehax.asm.TypesMc.Methods;
import com.matt.forgehax.asm.utils.ASMHelper;
import com.matt.forgehax.asm.utils.asmtype.ASMMethod;
import com.matt.forgehax.asm.utils.transforming.ClassTransformer;
import com.matt.forgehax.asm.utils.transforming.Inject;
import com.matt.forgehax.asm.utils.transforming.MethodTransformer;
import com.matt.forgehax.asm.utils.transforming.RegisterMethodTransformer;
import java.util.Objects;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class MinecraftPatch extends ClassTransformer {
  
  public MinecraftPatch() {
    super(Classes.Minecraft);
  }
  
  @RegisterMethodTransformer
  public class SetIngameFocus extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.Minecraft_setIngameFocus;
    }
    
    @Inject(description = "Add callback before setting leftclick timer")
    public void inject(MethodNode method) {
      AbstractInsnNode node =
        ASMHelper.findPattern(
          method.instructions.getFirst(),
          new int[]{SIPUSH, PUTFIELD, 0, 0, 0, RETURN},
          "xx???x");
      Objects.requireNonNull(node, "Failed to find SIPUSH node");
      
      InsnList list = new InsnList();
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onLeftClickCounterSet));
      
      method.instructions.insert(node, list);
    }
  }
  
  @RegisterMethodTransformer
  public class RunTick extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.Minecraft_runTick;
    }
    
    @Inject(description = "Add callback before setting leftclick timer")
    public void inject(MethodNode method) {
      AbstractInsnNode node =
        ASMHelper.findPattern(
          method.instructions.getFirst(),
          new int[]{
            SIPUSH,
            PUTFIELD,
            0,
            0,
            0,
            ALOAD,
            GETFIELD,
            IFNULL,
            0,
            0,
            ALOAD,
            GETFIELD,
            INVOKEVIRTUAL
          },
          "xx???xxx??xxx");
      Objects.requireNonNull(node, "Failed to find SIPUSH node");
      
      InsnList list = new InsnList();
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onLeftClickCounterSet));
      
      method.instructions.insert(node, list);
    }
  }
  
  @RegisterMethodTransformer
  public class SendClickBlockToController extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.Minecraft_sendClickBlockToController;
    }
    
    @Inject(description = "Add hook to set left click")
    public void injectFirst(MethodNode method) {
      InsnList list = new InsnList();
      list.add(new VarInsnNode(ALOAD, 0));
      list.add(new VarInsnNode(ILOAD, 1));
      list.add(
        ASMHelper.call(
          INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onSendClickBlockToController));
      list.add(new VarInsnNode(ISTORE, 1));
      
      method.instructions.insert(list);
    }

    @Inject(description = "Add hook to set hand as inactive")
    public void injectSecond(MethodNode method) {
      AbstractInsnNode node =
      ASMHelper.findPattern(
        method.instructions.getFirst(),
        new int[]{
          ALOAD, GETFIELD, INVOKEVIRTUAL, IFNE
        },
        "xxxx");

      Objects.requireNonNull(node, "Failed to find isHandActive() node");
      AbstractInsnNode after = node.getNext().getNext().getNext();
      
      InsnList list = new InsnList();

      LabelNode jump = new LabelNode();

      list.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_makeHandAlwaysInactive));
      list.add(new JumpInsnNode(IFNE, jump));
      
      method.instructions.insertBefore(node, list);
      method.instructions.insert(after, jump);
    }
  }
  
//TheAlphaEpsilon
  @RegisterMethodTransformer
  public class RightClickMouse extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.Minecraft_rightClickMouse;
    }
    
    @Inject(description = "Add hook after checking if entitycontroller is hitting block")
    public void inject(MethodNode method) {
      
    	AbstractInsnNode returnNode =
    			ASMHelper.findPattern(method.instructions.getFirst(),
    					ALOAD,
    					ICONST_4,
    					PUTFIELD
    					);
    	
    	Objects.requireNonNull(returnNode, "Return node is null");
    	
    	LabelNode jump = new LabelNode();
    	
    	InsnList list = new InsnList();

    	list.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_makeIsHittingBlockAlwaysFalse));
		list.add(new JumpInsnNode(IFNE, jump));
    	
    	method.instructions.insert(list);
    	method.instructions.insertBefore(returnNode, jump); //Insert the jump after the first return
    	
    }
  }
}
