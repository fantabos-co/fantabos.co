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
public class RenderLivingBasePatch extends ClassTransformer {

	public RenderLivingBasePatch() {
		super(Classes.RenderLivingBase);
	}

	@RegisterMethodTransformer
	private class RenderModel extends MethodTransformer {
		@Override
		public ASMMethod getMethod() {
			return Methods.RenderLivingBase_renderModel;
		}
		
		@Inject(description = "Add hook before the modelbase render method")
		public void injectFirst(MethodNode main) {

			
			AbstractInsnNode preNode =
			        ASMHelper.findPattern(main.instructions.getFirst(), 
			        		new int[]{
			        				ALOAD,
			        				GETFIELD,
			        				}, 
			        		"xx"); //Before Modelbase render method
			
			AbstractInsnNode postNode =
			        ASMHelper.findPattern(main.instructions.getFirst(), 
			        		new int[]{
			        				INVOKEVIRTUAL,
			        				0x00,
			        				0x00,
			        				ILOAD,
			        				IFEQ
			        				}, 
			        		"x??xx");
			
			Objects.requireNonNull(preNode, "pre node is null");
			Objects.requireNonNull(postNode, "post node is null");

			LabelNode jumpPast = new LabelNode();
			
			InsnList insnList = new InsnList();	
			
			insnList.add(new VarInsnNode(ALOAD, 0)); // Get RenderLivingBase instance
			insnList.add(ASMHelper.call(GETFIELD, TypesMc.Fields.RenderLivingBase_mainModel)); //Get main model
			insnList.add(new VarInsnNode(ALOAD, 1)); // Get entity
			insnList.add(new VarInsnNode(FLOAD, 2));
			insnList.add(new VarInsnNode(FLOAD, 3));
			insnList.add(new VarInsnNode(FLOAD, 4));
			insnList.add(new VarInsnNode(FLOAD, 5));
			insnList.add(new VarInsnNode(FLOAD, 6));
			insnList.add(new VarInsnNode(FLOAD, 7));
			insnList.add(
			        ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onRenderModel));
			insnList.add(new JumpInsnNode(IFNE, jumpPast));

			main.instructions.insertBefore(preNode, insnList);
			main.instructions.insert(postNode, jumpPast);
		}

		@Inject(description = "Add hook to change head pitch")
		public void injectSecond(MethodNode main) {
			InsnList insnList = new InsnList();

			insnList.add(new VarInsnNode(ALOAD, 1)); // Get entity
			insnList.add(new VarInsnNode(FLOAD, 6)); // Get Pitch
			insnList.add(
				ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onRenderModelHead));
			insnList.add(new VarInsnNode(FSTORE, 6));

			main.instructions.insert(insnList);
		}
	}

	@RegisterMethodTransformer
	private class ApplyRotations extends MethodTransformer {
		@Override
		public ASMMethod getMethod() {
			return Methods.RenderLivingBase_applyRotations;
		}

		@Inject(description = "Add hook before the modelbase is rotated to change it")
		public void inject(MethodNode main) {
			InsnList insnList = new InsnList();

			insnList.add(new VarInsnNode(ALOAD, 1)); // Get entity
			insnList.add(new VarInsnNode(FLOAD, 3)); // Get Yaw
			insnList.add(
				ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onApplyRotations));
			insnList.add(new VarInsnNode(FSTORE, 3));

			main.instructions.insert(insnList);
		}
	}

	@RegisterMethodTransformer
	private class RenderLayers extends MethodTransformer {
		@Override
		public ASMMethod getMethod() {
			return Methods.RenderLivingBase_renderLayers;
		}

		@Inject(description = "Add hook before layers are rendered to make helmet turn with head")
		public void inject(MethodNode main) {
			InsnList insnList = new InsnList();

			insnList.add(new VarInsnNode(ALOAD, 1)); // Get entity
			insnList.add(new VarInsnNode(FLOAD, 7)); // Get Pitch
			insnList.add(
				ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onRenderModelHead));
			insnList.add(new VarInsnNode(FSTORE, 7));

			main.instructions.insert(insnList);
		}
	}
	
}
