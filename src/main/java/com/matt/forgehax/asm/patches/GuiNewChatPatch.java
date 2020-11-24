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
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class GuiNewChatPatch extends ClassTransformer {

  public GuiNewChatPatch() {
    super(Classes.GuiNewChat);
  }

  @RegisterMethodTransformer
  private class printChatLinePatch extends MethodTransformer {

    @Override
    public ASMMethod getMethod() {
      return Methods.GuiNewChat_setChatLine;
    }

    @Inject(description = "Add hook to log all messages added to chat")
    public void inject(MethodNode main) {
      InsnList insnList = new InsnList();

      insnList.add(new VarInsnNode(ALOAD, 1));
      insnList.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onPrintChatLine));
      insnList.add(new VarInsnNode(ASTORE, 1));
      
      main.instructions.insert(insnList); // inject at top of method
    }
  }

  @RegisterMethodTransformer
  private class setChatLinePatch extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.GuiNewChat_setChatLine;
    }
    
    @Inject(description = "Set hook to skip max size removals")
    public void inject(MethodNode main) {
      AbstractInsnNode node =
        ASMHelper.findPattern(main.instructions.getFirst(),
          new int[]{ ALOAD, GETFIELD, INVOKEINTERFACE },
          "xxx");
      
      Objects.requireNonNull(node, "Find pattern failed for node");

      AbstractInsnNode after = node;
      for (int i=0; i<17; i++)
        after = after.getNext();
      
      InsnList insnList = new InsnList();
      InsnList afterList = new InsnList();
      
      final LabelNode jmp = new LabelNode();
      insnList.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_doPreventMaxChatSize));
      insnList.add(new JumpInsnNode(IFNE, jmp));
      afterList.add(jmp);
      
      main.instructions.insertBefore(node, insnList);
      main.instructions.insertBefore(after, afterList);
    }
  }

  @RegisterMethodTransformer
  private class drawChatPatch extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.GuiNewChat_drawChat;
    }
    
    @Inject(description = "Set hook to skip chat background drawing")
    public void inject(MethodNode main) {
      AbstractInsnNode node =
        ASMHelper.findPattern(main.instructions.getFirst(),
          new int[]{ BIPUSH, ILOAD, BIPUSH, ISUB, ICONST_0 },
          "xxxxx");
      
      Objects.requireNonNull(node, "Find pattern failed for before drawBox 1");

      AbstractInsnNode after =
        ASMHelper.findPattern(main.instructions.getFirst(),
          new int[]{ ALOAD, INVOKEVIRTUAL, INVOKEINTERFACE, ASTORE },
          "xxxx");
      
      Objects.requireNonNull(after, "Find pattern failed for after drawBox 1");
      
      InsnList insnList = new InsnList();
      InsnList afterList = new InsnList();
      
      final LabelNode jmp = new LabelNode();
      insnList.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_doHideChatBackground));
      insnList.add(new JumpInsnNode(IFNE, jmp));
      afterList.add(jmp);
      
      main.instructions.insertBefore(node, insnList);
      main.instructions.insertBefore(after, afterList);

      // node =
      //   ASMHelper.findPattern(main.instructions.getFirst(),
      //     new int[]{ ICONST_0, ILOAD, INEG, ICONST_2, ILOAD, INEG },
      //     "xxxxxx");
      // 
      // Objects.requireNonNull(node, "Find pattern failed for before drawBox 2");
      // 
      // after =
      //   ASMHelper.findPattern(main.instructions.getFirst(),
      //     new int[]{ LDC, ILOAD, BIPUSH, ISHL, IADD },
      //     "xxxxx");
      // 
      // Objects.requireNonNull(after, "Find pattern failed for after drawBox 2");
      // 
      // for (int i = 0; i<7; i++)
      //   after = after.getNext();
      // 
      // insnList = new InsnList();
      // afterList = new InsnList();
      // 
      // final LabelNode jmp2 = new LabelNode();
      // insnList.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_doHideChatBackground));
      // insnList.add(new JumpInsnNode(IFNE, jmp2));
      // afterList.add(jmp2);
      // 
      // main.instructions.insertBefore(node, insnList);
      // main.instructions.insertBefore(after, afterList);
    }
  }
}