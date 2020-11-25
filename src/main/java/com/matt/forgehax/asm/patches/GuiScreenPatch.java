package com.matt.forgehax.asm.patches;

import java.util.Objects;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.matt.forgehax.asm.TypesHook;
import com.matt.forgehax.asm.utils.ASMHelper;
import com.matt.forgehax.asm.utils.asmtype.ASMMethod;
import com.matt.forgehax.asm.utils.transforming.ClassTransformer;
import com.matt.forgehax.asm.utils.transforming.Inject;
import com.matt.forgehax.asm.utils.transforming.MethodTransformer;
import com.matt.forgehax.asm.utils.transforming.RegisterMethodTransformer;

/**
 * TheAlphaEpsilon
 * 24NOV2020
 */
public class GuiScreenPatch extends ClassTransformer {

	public GuiScreenPatch() {
		super(Classes.GuiScreen);
	}
	
	@RegisterMethodTransformer
	private class injectClickEvent extends MethodTransformer {

		@Override
		public ASMMethod getMethod() {
			return Methods.GuiScreen_handleComponentClick;
		}
		
		@Inject(description = "Insert method to get textcomponent click")
	    public void inject(MethodNode main) {			
						
			AbstractInsnNode node =
	                ASMHelper.findPattern(main.instructions.getFirst(),
	                    new int[] { INVOKESTATIC },
	                    "x");
			
			Objects.requireNonNull(node, "node is null");
			
			InsnList insnList = new InsnList();
            LabelNode after = new LabelNode();
            
            insnList.add(new VarInsnNode(ALOAD, 0));
            insnList.add(new VarInsnNode(ALOAD, 1));
            insnList.add(new VarInsnNode(ALOAD, 2));
            insnList.add(ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onHandleComponentClick));
            insnList.add(new JumpInsnNode(IFEQ, after)); //If event not canceled, don't return
            insnList.add(new InsnNode(ICONST_0));
            insnList.add(new InsnNode(IRETURN));
            insnList.add(after);
            
            main.instructions.insertBefore(node, insnList);
            
	    }
		
	}
	
}
