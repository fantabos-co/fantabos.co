package com.matt.forgehax.asm.patches;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.matt.forgehax.asm.TypesMc;
import com.matt.forgehax.asm.TypesSpecial;
import com.matt.forgehax.asm.utils.ASMHelper;
import com.matt.forgehax.asm.utils.asmtype.ASMMethod;
import com.matt.forgehax.asm.utils.transforming.ClassTransformer;
import com.matt.forgehax.asm.utils.transforming.Inject;
import com.matt.forgehax.asm.utils.transforming.MethodTransformer;
import com.matt.forgehax.asm.utils.transforming.RegisterMethodTransformer;

/*
 * TheAlphaEpsilon
 * 25Oct2020
 */
public class AbstractClientPlayerPatch extends ClassTransformer {

	public AbstractClientPlayerPatch() {
		super(Classes.AbstractClientPlayer);
	}
	
	@RegisterMethodTransformer
	private class addCapeOverride extends MethodTransformer {

		@Override
		public ASMMethod getMethod() {
			return Methods.AbstractClientPlayer_getLocationCape;
		}
		
		@Inject(description = "Insert method to get FHTextures")
	    public void inject(MethodNode main) {			
			LabelNode jump = new LabelNode();
			InsnList insnList = new InsnList();
			
			insnList.add(new InsnNode(NOP)); // Fuck you Xaero
			insnList.add(new VarInsnNode(ALOAD, 0));
			insnList.add(ASMHelper.call(INVOKEVIRTUAL, TypesMc.Methods.Entity_getUniqueID));
			insnList.add(new InsnNode(ICONST_0));
			insnList.add(ASMHelper.call(INVOKESTATIC, TypesSpecial.Methods.FHTextures_getResource)); //UUID, type
			insnList.add(new InsnNode(DUP));
			insnList.add(new JumpInsnNode(IFNULL, jump));
			insnList.add(new InsnNode(ARETURN));
			insnList.add(jump);
			insnList.add(new InsnNode(POP));
			
			main.instructions.insert(insnList); // Insert at top of method
	    }
		
	}

}
