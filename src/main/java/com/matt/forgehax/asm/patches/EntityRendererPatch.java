package com.matt.forgehax.asm.patches;

import com.matt.forgehax.asm.TypesHook;
import com.matt.forgehax.asm.utils.ASMHelper;
import com.matt.forgehax.asm.utils.asmtype.ASMMethod;
import com.matt.forgehax.asm.utils.debug.AsmPrinter;
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

public class EntityRendererPatch extends ClassTransformer {

  public EntityRendererPatch() {
    super(Classes.EntityRenderer);
  }

  @RegisterMethodTransformer
  private class HurtCameraEffect extends MethodTransformer {

    @Override
    public ASMMethod getMethod() {
      return Methods.EntityRenderer_hurtCameraEffect;
    }

    @Inject(description = "Add hook that allows the method to be canceled")
    public void inject(MethodNode main) {
      AbstractInsnNode preNode = main.instructions.getFirst();
      AbstractInsnNode postNode =
        ASMHelper.findPattern(main.instructions.getFirst(), new int[]{RETURN}, "x");

      Objects.requireNonNull(preNode, "Find pattern failed for preNode");
      Objects.requireNonNull(postNode, "Find pattern failed for postNode");

      LabelNode endJump = new LabelNode();

      InsnList insnPre = new InsnList();
      insnPre.add(new VarInsnNode(FLOAD, 1));
      insnPre.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onHurtcamEffect));
      insnPre.add(new JumpInsnNode(IFNE, endJump));

      main.instructions.insertBefore(preNode, insnPre);
      main.instructions.insertBefore(postNode, endJump);
    }
  }

  @RegisterMethodTransformer
  private class PlaceThruEntities extends MethodTransformer {

    @Override
    public ASMMethod getMethod() {
      return Methods.EntityRenderer_getMouseOver;
    }

    @Inject(description = "Add hook that allows to set Entities in BB list size to 0, to place thru them")
    public void inject(MethodNode main) {
      AbstractInsnNode node =
        ASMHelper.findPattern(main.instructions.getFirst(),
                  new int[]{ILOAD, ALOAD, INVOKEINTERFACE, IF_ICMPGE},
                  "xxxx");

      Objects.requireNonNull(node, "Find pattern failed for list.size() node");

      AbstractInsnNode compareNode = node;
      for (int i = 0; i<2; i++) compareNode = compareNode.getNext();

      LabelNode skipJump = new LabelNode();
      LabelNode normalJump = new LabelNode();

      InsnList insnList = new InsnList();

      insnList.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_allowPlaceThroughEntities));
      insnList.add(new JumpInsnNode(IFEQ, normalJump));
      insnList.add(new InsnNode(ICONST_0));
      insnList.add(new InsnNode(ICONST_0));
      insnList.add(new JumpInsnNode(GOTO, skipJump));
      insnList.add(normalJump);

      main.instructions.insertBefore(node, insnList);
      main.instructions.insert(compareNode, skipJump);
    }
  }

  @RegisterMethodTransformer
  private class DrawBlockHighlightInWater extends MethodTransformer {

    @Override
    public ASMMethod getMethod() {
      return Methods.EntityRenderer_renderWorldPass;
    }

    @Inject(description = "Skip is-in-water check for BlockHighlight")
    public void inject(MethodNode main) {
      AbstractInsnNode node =
        ASMHelper.findPattern(main.instructions.getFirst(),
                  new int[]{ IFNULL, ALOAD, GETSTATIC, INVOKEVIRTUAL, IFNE },
                  "xxxxx");

      Objects.requireNonNull(node, "Find pattern failed for list.size() node");

      AbstractInsnNode afterNode = node;
      for (int i = 0; i<5; i++) afterNode = afterNode.getNext();

      LabelNode skipJump = new LabelNode();

      InsnList insnList = new InsnList();

      insnList.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_drawBlockHighlightInWater));
      insnList.add(new JumpInsnNode(IFNE, skipJump));

      main.instructions.insert(node, insnList);
      main.instructions.insert(afterNode, skipJump);
    }
  }

  @RegisterMethodTransformer
  private class loadEntityShader extends MethodTransformer {
    @Override
    public ASMMethod getMethod() {
      return Methods.EntityRenderer_loadEntityShader;
    }

    @Inject(description = "Add hook that allows the method to be canceled")
    public void inject(MethodNode main) {
      AbstractInsnNode node = main.instructions.getFirst();

      Objects.requireNonNull(node, "Find pattern failed for preNode");

      LabelNode jump = new LabelNode();

      InsnList insnList = new InsnList();
      insnList.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onLoadShader));
      insnList.add(new JumpInsnNode(IFEQ, jump)); // if false jump to our label and continue like normal
      insnList.add(new InsnNode(RETURN)); // if true add return
      insnList.add(jump); // add our label


      main.instructions.insert(node, insnList);
    }
  }

  @RegisterMethodTransformer
  private class orientCamera extends MethodTransformer {
    @Override
    public ASMMethod getMethod() {
      return Methods.EntityRenderer_orientCamera;
    }

    @Inject(description = "Add hook that allows 3rd person camera to clip")
    public void inject(MethodNode main) {
      AbstractInsnNode node =
        ASMHelper.findPattern(main.instructions.getFirst(),
                  new int[]{ ALOAD, GETFIELD, GETFIELD, NEW, DUP, DLOAD, FLOAD, F2D },
                  "xxxxxxxx");

      AbstractInsnNode postNode =
        ASMHelper.findPattern(main.instructions.getFirst(),
                  new int[]{ ASTORE, NONE, NONE, ALOAD, IFNULL, NONE, NONE, ALOAD, GETFIELD, NEW, DUP, DLOAD, DLOAD, DLOAD },
                  "x??xx??xxxxxxx");

      Objects.requireNonNull(node, "Find pattern failed for preNode");
      Objects.requireNonNull(postNode, "Find pattern failed for postNode");

      LabelNode jump = new LabelNode();
      LabelNode skip = new LabelNode();

      InsnList insnList = new InsnList();
      insnList.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_allowCameraClip));
      insnList.add(new JumpInsnNode(IFEQ, skip));
      insnList.add(new InsnNode(ACONST_NULL));
      insnList.add(new JumpInsnNode(GOTO, jump));
      insnList.add(skip);

      main.instructions.insertBefore(node, insnList);
      main.instructions.insertBefore(postNode, jump);
    }
  }
}
