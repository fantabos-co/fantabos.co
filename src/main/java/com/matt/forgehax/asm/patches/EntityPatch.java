package com.matt.forgehax.asm.patches;

import com.matt.forgehax.asm.TypesHook;
import com.matt.forgehax.asm.utils.ASMHelper;
import com.matt.forgehax.asm.utils.asmtype.ASMMethod;
import com.matt.forgehax.asm.utils.transforming.ClassTransformer;
import com.matt.forgehax.asm.utils.transforming.Inject;
import com.matt.forgehax.asm.utils.transforming.MethodTransformer;
import com.matt.forgehax.asm.utils.transforming.RegisterMethodTransformer;
import org.objectweb.asm.tree.*;

import java.util.Objects;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import scala.tools.asm.Type;

public class EntityPatch extends ClassTransformer {
  
  public EntityPatch() {
    super(Classes.Entity);
  }
  
  
  @RegisterMethodTransformer
  private class ApplyEntityCollision extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.Entity_applyEntityCollision;
    }
    
    @Inject(description = "Add hook to disable push motion")
    private void inject(MethodNode main) {
      AbstractInsnNode thisEntityPreNode =
        ASMHelper.findPattern(
          main.instructions.getFirst(),
          new int[]{ALOAD, DLOAD, DNEG, DCONST_0, DLOAD, DNEG, INVOKEVIRTUAL},
          "xxxxxxx");
      // start at preNode, and scan for next INVOKEVIRTUAL sig
      AbstractInsnNode thisEntityPostNode =
        ASMHelper.findPattern(thisEntityPreNode, new int[]{INVOKEVIRTUAL}, "x");
      AbstractInsnNode otherEntityPreNode =
        ASMHelper.findPattern(
          thisEntityPostNode,
          new int[]{ALOAD, DLOAD, DCONST_0, DLOAD, INVOKEVIRTUAL},
          "xxxxx");
      // start at preNode, and scan for next INVOKEVIRTUAL sig
      AbstractInsnNode otherEntityPostNode =
        ASMHelper.findPattern(otherEntityPreNode, new int[]{INVOKEVIRTUAL}, "x");
      
      Objects.requireNonNull(thisEntityPreNode, "Find pattern failed for thisEntityPreNode");
      Objects.requireNonNull(thisEntityPostNode, "Find pattern failed for thisEntityPostNode");
      Objects.requireNonNull(otherEntityPreNode, "Find pattern failed for otherEntityPreNode");
      Objects.requireNonNull(otherEntityPostNode, "Find pattern failed for otherEntityPostNode");
      
      LabelNode endJumpForThis = new LabelNode();
      LabelNode endJumpForOther = new LabelNode();
      
      // first we handle this.addVelocity
      
      InsnList insnThisPre = new InsnList();
      insnThisPre.add(new VarInsnNode(ALOAD, 0)); // push THIS
      insnThisPre.add(new VarInsnNode(ALOAD, 1));
      insnThisPre.add(new VarInsnNode(DLOAD, 2));
      insnThisPre.add(new InsnNode(DNEG)); // push -X
      insnThisPre.add(new VarInsnNode(DLOAD, 4));
      insnThisPre.add(new InsnNode(DNEG)); // push -Z
      insnThisPre.add(
        ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onApplyCollisionMotion));
      insnThisPre.add(new JumpInsnNode(IFNE, endJumpForThis));
      
      InsnList insnOtherPre = new InsnList();
      insnOtherPre.add(new VarInsnNode(ALOAD, 1)); // push entityIn
      insnOtherPre.add(new VarInsnNode(ALOAD, 0)); // push THIS
      insnOtherPre.add(new VarInsnNode(DLOAD, 2)); // push X
      insnOtherPre.add(new VarInsnNode(DLOAD, 4)); // push Z
      insnOtherPre.add(
        ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onApplyCollisionMotion));
      insnOtherPre.add(new JumpInsnNode(IFNE, endJumpForOther));
      
      main.instructions.insertBefore(thisEntityPreNode, insnThisPre);
      main.instructions.insert(thisEntityPostNode, endJumpForThis);
      
      main.instructions.insertBefore(otherEntityPreNode, insnOtherPre);
      main.instructions.insert(otherEntityPostNode, endJumpForOther);
    }
  }


  @RegisterMethodTransformer
  private class MoveEntity extends MethodTransformer {

    @Override
    public ASMMethod getMethod() {
      return Methods.Entity_move;
    }

    @Inject(description = "Insert flag into statement that performs sneak movement")
    public void injectSecond(final MethodNode main) {
      /**
       *    OVERHAULED BY TONIO_CARTONIO
       * This is quite convoluted, but should look like this in the end:
       *     [START]
       * +      ALOAD 0
       * +      GETFIELD (stepHeight)
       * +      FSTORE <local_var>
       *        ...
       *        ...
       *        ...
       * +    --GOTO L268
       *     ->269
       *     || ALOAD 0
       * _   || GETFIELD (onGround)
       *     || ALOAD 0
       *     || INVOKEVIRTUAL (isSneaking())
       * +   || ALOAD 0
       * +   || INVOKESTATIC Hooks.onSneakEvent
       *     || IFEQ L50
       * +  ----GOTO L270
       *    ||>268
       *    ||  ALOAD 0
       *    ||  INSTANCEOF (EntityPlayer)
       *    ||  IFEQ L50
       * +  |---GOTO L269
       *    -->270
       *        ...
       *        ... (Sneak code)
       *        ...
       *       L50
       *        LINENUMBER L50
       *        FRAME SAME
       * +      ALOAD 0
       * +      FLOAD <local_var>     // you can set stepHeight to anything in
       * +      PUTFIELD (stepHeight) //  your event because it will be reverted
       */

      AbstractInsnNode groundCheckNode =
              ASMHelper.findPattern(
                      main.instructions.getFirst(),
                      new int[]{ALOAD, GETFIELD, IFEQ, ALOAD, INVOKEVIRTUAL, IFEQ, ALOAD, INSTANCEOF, IFEQ, 0x00, 0x00, LDC, DSTORE},
                      "xxxxxxxxx??xx");

      AbstractInsnNode afterSneak =
              ASMHelper.findPattern(
                      main.instructions.getFirst(),
                      new int[]{LDC, DADD, DSTORE, 0x00, 0x00, 0x00, DLOAD, DSTORE, GOTO, 0x00, 0x00, 0x00, ALOAD, GETFIELD, ALOAD, ALOAD },
                      "xxx???xxx???xxxx");
      
      for (int i=0; i<11; i++) afterSneak = afterSneak.getNext();

      Objects.requireNonNull(groundCheckNode, "Find pattern failed for groundCheckNode");
      Objects.requireNonNull(afterSneak, "Find pattern failed for afterSneakNode");

      AbstractInsnNode instanceofCheck = groundCheckNode;
      // AbstractInsnNode moverTypeCheck = groundCheckNode;
      // for (int i = 0; i < 9; i++)
      //   moverTypeCheck = moverTypeCheck.getPrevious();
      for (int i = 0; i < 3; i++)
        instanceofCheck = instanceofCheck.getNext();
      main.instructions.remove(instanceofCheck.getPrevious()); // remove 1st IFEQ node
      AbstractInsnNode invokeNode = instanceofCheck.getNext();
      for (int i = 0; i < 3; i++)
        instanceofCheck = instanceofCheck.getNext();

      AbstractInsnNode afterNode = instanceofCheck.getNext().getNext();

      // the checks now load those values for us
      LabelNode loadVarLabel = new LabelNode();
      // where to skip for checking if player
      LabelNode checkEntity = new LabelNode();
      // after here, we do sneak caclulation
      LabelNode doSneakLabel = new LabelNode();

      final int oldStepIndex =
        ASMHelper.addNewLocalVariable(main, "old_step_heighgt", Type.getDescriptor(float.class));

      InsnList startList = new InsnList();
      InsnList preList = new InsnList();
      InsnList midList = new InsnList();
      InsnList postList = new InsnList();
      InsnList hookList = new InsnList();
      InsnList revertList = new InsnList();

      preList.insert(loadVarLabel);
      preList.insert(new JumpInsnNode(GOTO, checkEntity));

      startList.insert(new VarInsnNode(FSTORE, oldStepIndex));
      startList.insert(ASMHelper.call(GETFIELD, Fields.Entity_stepHeight));
      startList.insert(new VarInsnNode(ALOAD, 0));

      midList.insert(checkEntity);
      midList.insert(new JumpInsnNode(GOTO, doSneakLabel));

      postList.insert(doSneakLabel);
      postList.insert(new JumpInsnNode(GOTO, loadVarLabel));

      hookList.insert(new VarInsnNode(ALOAD, 0));
      hookList.insert(invokeNode, // da hook
              ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onSneakEvent));

      revertList.insert(ASMHelper.call(PUTFIELD, Fields.Entity_stepHeight));
      revertList.insert(new VarInsnNode(FLOAD, oldStepIndex));
      revertList.insert(new VarInsnNode(ALOAD, 0));

      main.instructions.insertBefore(groundCheckNode, preList);
      main.instructions.insertBefore(instanceofCheck, midList);
      main.instructions.insert(afterNode, postList);
      main.instructions.insert(invokeNode, hookList);
      main.instructions.insert(afterSneak, revertList);
      main.instructions.insert(startList); // at top of method
    }
  }
  private static Printer printer = new Textifier();
  private static TraceMethodVisitor mp = new TraceMethodVisitor(printer);
  public static String insnToString(AbstractInsnNode insn){
    insn.accept(mp);
    StringWriter sw = new StringWriter();
    printer.print(new PrintWriter(sw));
    printer.getText().clear();
    return sw.toString();
  }
  
  @RegisterMethodTransformer
  private class DoBlockCollisions extends MethodTransformer {
    
    @Override
    public ASMMethod getMethod() {
      return Methods.Entity_doBlockCollisions;
    }
    
    @Inject(description = "Add hook to disable block motion effects")
    public void inject(MethodNode main) {
      AbstractInsnNode preNode =
        ASMHelper.findPattern(
          main.instructions.getFirst(),
          new int[]{
            ASTORE,
            0x00,
            0x00,
            ALOAD,
            INVOKEINTERFACE,
            ALOAD,
            GETFIELD,
            ALOAD,
            ALOAD,
            ALOAD,
            INVOKEVIRTUAL
          },
          "x??xxxxxxxx");
      AbstractInsnNode postNode = ASMHelper.findPattern(preNode, new int[]{GOTO}, "x");
      
      Objects.requireNonNull(preNode, "Find pattern failed for preNode");
      Objects.requireNonNull(postNode, "Find pattern failed for postNode");
      
      LabelNode endJump = new LabelNode();
      
      InsnList insnList = new InsnList();
      insnList.add(new VarInsnNode(ALOAD, 0)); // push entity
      insnList.add(new VarInsnNode(ALOAD, 8)); // push block state
      insnList.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_isBlockFiltered));
      insnList.add(new JumpInsnNode(IFNE, endJump));
      
      main.instructions.insertBefore(postNode, endJump);
      main.instructions.insert(preNode, insnList);
    }
  }
}
