package com.matt.forgehax.asm.patches;

import com.matt.forgehax.asm.TypesHook;
import com.matt.forgehax.asm.utils.ASMHelper;
import com.matt.forgehax.asm.utils.asmtype.ASMMethod;
import com.matt.forgehax.asm.utils.transforming.ClassTransformer;
import com.matt.forgehax.asm.utils.transforming.Inject;
import com.matt.forgehax.asm.utils.transforming.MethodTransformer;
import com.matt.forgehax.asm.utils.transforming.RegisterMethodTransformer;
import org.objectweb.asm.tree.*;

/*
 * Made by Fraaz on the 27th of October 2020.
 * Necessary to EntityControl. This is complementary to the similar patches for pigs and llamas.
 */

public class AbstractHorsePatch extends ClassTransformer {
    public AbstractHorsePatch() {
        super(Classes.AbstractHorse);
    }

    @RegisterMethodTransformer
    private class inject extends MethodTransformer {

        @Override
        public ASMMethod getMethod() {
            return Methods.AbstractHorse_canBeSteered;
        }

        @Inject(description = "Alter canBeSteered for horses so it always returns true if EntityControl.enabled")
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
    private class saddledHorsePatch extends MethodTransformer {

        @Override
        public ASMMethod getMethod() {
            return Methods.AbstractHorse_isHorseSaddled;
        }

        @Inject(description = "Alter isHorseSaddled for horses so it always returns true if EntityControl.enabled")
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

}