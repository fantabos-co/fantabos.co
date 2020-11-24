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
public class RenderEntityItemPatch extends ClassTransformer {

	public RenderEntityItemPatch() {
		super(Classes.RenderEntityItem);
	}
	
	@RegisterMethodTransformer
	private class RenderModel extends MethodTransformer {
		@Override
		public ASMMethod getMethod() {
			return Methods.RenderEntityItem_doRender;
		}
		
		@Inject(description = "Add hooks in the doRender method")
		public void inject(MethodNode main) {
			
			AbstractInsnNode preNode3d =
			        ASMHelper.findPattern(main.instructions.getFirst(), 
			        		ALOAD,
			        		GETFIELD,
			        		ALOAD,
			        		ALOAD,
			        		INVOKEVIRTUAL,
			        		NONE,
			        		NONE,
			        		INVOKESTATIC
			        		);
			        		
			AbstractInsnNode postNode3d =
			        ASMHelper.findPattern(main.instructions.getFirst(), 
			        		INVOKESTATIC,
			        		NONE,
			        		NONE,
			        		GOTO
			        		);
			
			Objects.requireNonNull(preNode3d, "3d pre node is null");
			Objects.requireNonNull(postNode3d, "3d post node is null");
			
			LabelNode jumpPast3d = new LabelNode();
			
			InsnList insnList3d = new InsnList();	
			
			insnList3d.add(new VarInsnNode(ALOAD, 0));
			
			insnList3d.add(
			        ASMHelper.call(GETFIELD, TypesMc.Fields.RenderEntityItem_itemRenderer));

			insnList3d.add(new VarInsnNode(ALOAD, 10));
			
			insnList3d.add(new VarInsnNode(ALOAD, 13));
			
			insnList3d.add(
			        ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onRenderEntityItem3d));
			insnList3d.add(new JumpInsnNode(IFNE, jumpPast3d));

			
			AbstractInsnNode preNode2d =
			        ASMHelper.findPattern(main.instructions.getFirst(), 
			        		ALOAD,
			        		GETFIELD,
			        		ALOAD,
			        		ALOAD,
			        		INVOKEVIRTUAL,
			        		NONE,
			        		NONE,
			        		INVOKESTATIC,
			        		NONE,
			        		NONE,
			        		FCONST_0,
			        		FCONST_0,
			        		LDC
			        		);
			        		
			AbstractInsnNode postNode2d =
			        ASMHelper.findPattern(main.instructions.getFirst(), 
			        		INVOKESTATIC,
			        		NONE,
			        		NONE,
			        		FCONST_0,
			        		FCONST_0,
			        		LDC,
			        		INVOKESTATIC
			        		);
			
			Objects.requireNonNull(preNode2d, "2d pre node is null");
			Objects.requireNonNull(postNode2d, "2d post node is null");
			
			LabelNode jumpPast2d = new LabelNode();
			
			InsnList insnList2d = new InsnList();	
			
			insnList2d.add(new VarInsnNode(ALOAD, 0));
			
			insnList2d.add(
			        ASMHelper.call(GETFIELD, TypesMc.Fields.RenderEntityItem_itemRenderer));

			insnList2d.add(new VarInsnNode(ALOAD, 10));
			
			insnList2d.add(new VarInsnNode(ALOAD, 13));
			
			insnList2d.add(
			        ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onRenderEntityItem2d));
			insnList2d.add(new JumpInsnNode(IFNE, jumpPast2d));

			main.instructions.insertBefore(preNode2d, insnList2d);
			main.instructions.insertBefore(postNode2d, jumpPast2d);
			
			main.instructions.insertBefore(preNode3d, insnList3d);
			main.instructions.insertBefore(postNode3d, jumpPast3d);
			
		}
	}
}
