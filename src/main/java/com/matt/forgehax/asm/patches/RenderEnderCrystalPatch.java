package com.matt.forgehax.asm.patches;

import java.util.Objects;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.matt.forgehax.asm.TypesHook;
import com.matt.forgehax.asm.TypesMc;
import com.matt.forgehax.asm.utils.ASMHelper;
import com.matt.forgehax.asm.utils.asmtype.ASMMethod;
import com.matt.forgehax.asm.utils.transforming.ClassTransformer;
import com.matt.forgehax.asm.utils.transforming.Inject;
import com.matt.forgehax.asm.utils.transforming.MethodTransformer;
import com.matt.forgehax.asm.utils.transforming.RegisterMethodTransformer;

/*TheAlphaEpsilon*/
public class RenderEnderCrystalPatch extends ClassTransformer {

	public RenderEnderCrystalPatch() {
		super(Classes.RenderEnderCrystal);
	}

	@RegisterMethodTransformer
	private class RenderModel extends MethodTransformer {
		@Override
		public ASMMethod getMethod() {
			return Methods.RenderEnderCrystal_doRender;
		}
		
		@Inject(description = "Add hook before the doRender method")
		public void inject(MethodNode main) {

			
			AbstractInsnNode preNode =
			        ASMHelper.findPattern(main.instructions.getFirst(), 
			        		ALOAD,
			        		GETFIELD,
			        		IFEQ,//Two bytes
			        		NONE,
			        		NONE,
			        		INVOKESTATIC
			        		);
			        		
			AbstractInsnNode postNode =
			        ASMHelper.findPattern(main.instructions.getFirst(), 
			        		INVOKESTATIC,
			        		NONE,
			        		NONE,
			        		ALOAD,
			        		INVOKEVIRTUAL
			        		);
			
			Objects.requireNonNull(preNode, "pre node is null");
			Objects.requireNonNull(postNode, "post node is null");
			
			LabelNode jumpPast = new LabelNode();
			
			LabelNode yesBottom = new LabelNode();
			
			LabelNode afterModel = new LabelNode();
			
			InsnList insnList = new InsnList();	
			
			insnList.add(new VarInsnNode(ALOAD, 1)); // Get CrystalEntity instance
			
			insnList.add(new VarInsnNode(ALOAD, 1)); // Get CrystalEntity instance
						
			insnList.add(ASMHelper.call(INVOKEVIRTUAL, TypesMc.Methods.EntityEnderCrystal_shouldShowBottom)); //Should Show Bottom?
			
			insnList.add(new JumpInsnNode(IFNE, yesBottom)); //Runs if true
			
			insnList.add(new VarInsnNode(ALOAD, 0)); // Get RenderCrystalEntity instance
			
			insnList.add(ASMHelper.call(GETFIELD, TypesMc.Fields.RenderEnderCrystal_modelEnderCrystalNoBase)); //No base crystal?
			
			insnList.add(new JumpInsnNode(GOTO, afterModel));
			
			insnList.add(yesBottom);
			
			insnList.add(new VarInsnNode(ALOAD, 0)); // Get RenderCrystalEntity instance
			
			insnList.add(ASMHelper.call(GETFIELD, TypesMc.Fields.RenderEnderCrystal_modelEnderCrystal)); //Base crystal
			
			insnList.add(afterModel); //Now has the model instance
			
			insnList.add(new VarInsnNode(ALOAD, 1)); // Get CrystalEntity instance
			
			insnList.add(new VarInsnNode(FLOAD, 11)); // Get height float
			
			insnList.add(new VarInsnNode(FLOAD, 10)); // Get height rotation
			
			insnList.add(
			        ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onRenderEnderCrystal));
			insnList.add(new JumpInsnNode(IFNE, jumpPast));

			main.instructions.insertBefore(preNode, insnList);
			main.instructions.insertBefore(postNode, jumpPast);
			
			
			/*
			insnList.add(
			        ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_testingMethod));
			insnList.add(new InsnNode(RETURN));
			
			main.instructions.insertBefore(main.instructions.getFirst(), insnList);
			*/
			
		}
	}
	
}
