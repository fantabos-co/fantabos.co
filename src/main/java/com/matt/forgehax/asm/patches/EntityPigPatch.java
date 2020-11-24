package com.matt.forgehax.asm.patches;

import java.util.Objects;

import com.matt.forgehax.asm.TypesHook;
import com.matt.forgehax.asm.TypesMc;
import com.matt.forgehax.asm.utils.ASMHelper;
import com.matt.forgehax.asm.utils.asmtype.ASMMethod;
import com.matt.forgehax.asm.utils.debug.AsmPrinter;
import com.matt.forgehax.asm.utils.transforming.ClassTransformer;
import com.matt.forgehax.asm.utils.transforming.Inject;
import com.matt.forgehax.asm.utils.transforming.MethodTransformer;
import com.matt.forgehax.asm.utils.transforming.RegisterMethodTransformer;

import org.objectweb.asm.tree.*;

/*
 * Made by Fraaz on the 28th of October 2020.
 * Necessary to EntityControl. This is complementary to the similar patches for horses and llamas.
 */

public class EntityPigPatch extends ClassTransformer {
    public EntityPigPatch() {
        super(Classes.EntityPig);
    }

    @RegisterMethodTransformer
    private class steeringPigPatch extends MethodTransformer {

        @Override
        public ASMMethod getMethod() {
            return Methods.EntityPig_canBeSteered;
        }

        @Inject(description = "Alter canBeSteered for pigs so it always returns true if EntityControl.enabled")
        public void inject(MethodNode main) {
            final InsnList insnList = new InsnList();
            LabelNode jumpPoint = new LabelNode();

            insnList.add(ASMHelper.call(GETSTATIC, TypesHook.Fields.ForgeHaxHooks_forceControlEntity));
            /* Get the value from isEntityControlEnabled */
            insnList.add(new JumpInsnNode(IFEQ, jumpPoint));
            /* if(isEntityControlEnabled)*/
            insnList.add(new InsnNode(ICONST_1));
            insnList.add(new InsnNode(IRETURN));
            /* return true; */
            insnList.add(jumpPoint);

            main.instructions.insert(insnList);
        }
    }

    @RegisterMethodTransformer
    private class travelPigPatch extends MethodTransformer {

        @Override
        public ASMMethod getMethod() {
            return Methods.EntityPig_travel;
        }
        
        @Inject(description = "Prevent pigs from wandering on their own")
        public void inject(MethodNode main) {
            AsmPrinter.logAsmMethod(main, "pigTravel-pre");
            AbstractInsnNode node =
                ASMHelper.findPattern(main.instructions.getFirst(),
                    new int[] { ALOAD, FCONST_0, FCONST_0, FCONST_1, INVOKESPECIAL },
                    "xxxxx");

            Objects.requireNonNull(node, "Failed to find node before pig travel");

            for (int i = 0; i < 3; i++) {
                main.instructions.remove(node.getNext()); // remove FCONST
            }

            /* node now points to this.setAIMoveSpeed(f); */
            final InsnList insnList = new InsnList();
            LabelNode before = new LabelNode();
            LabelNode after = new LabelNode();

            final int resultVector =
            ASMHelper.addNewLocalVariable(
                main, "vec", TypesMc.Classes.Vec3d.getDescriptor(), before, after);
          

            insnList.add(before);
            insnList.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onPigTravel));
            insnList.add(new VarInsnNode(ASTORE, resultVector));
            insnList.add(new VarInsnNode(ALOAD, 0));
            insnList.add(new VarInsnNode(ALOAD, resultVector));
            insnList.add(ASMHelper.call(GETFIELD, TypesMc.Fields.Vec3d_x));
            insnList.add(new InsnNode(D2F));
            insnList.add(new VarInsnNode(ALOAD, resultVector));
            insnList.add(ASMHelper.call(GETFIELD, TypesMc.Fields.Vec3d_y));
            insnList.add(new InsnNode(D2F));
            insnList.add(new VarInsnNode(ALOAD, resultVector));
            insnList.add(ASMHelper.call(GETFIELD, TypesMc.Fields.Vec3d_z));
            insnList.add(new InsnNode(D2F));
            insnList.add(after);
            // now normal travel() is called

            main.instructions.insert(node, insnList);
            AsmPrinter.logAsmMethod(main, "pigTravel-post");
        }
    }
}