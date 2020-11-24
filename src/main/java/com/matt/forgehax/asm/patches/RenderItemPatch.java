package com.matt.forgehax.asm.patches;

import java.util.Objects;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
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

/*By TheAlphaEpsilon*/
public class RenderItemPatch extends ClassTransformer {

	public RenderItemPatch() {
		super(Classes.RenderItem);
	}
	
	@RegisterMethodTransformer
	private class RenderModel extends MethodTransformer {
		@Override
		public ASMMethod getMethod() {
			return Methods.RenderItem_renderItemAndEffectIntoGUI;
		}
		
		@Inject(description = "Add hook before actually rendering the item")
		public void inject(MethodNode main) {

			
			AbstractInsnNode preNode = //right after inc zLevel
			        ASMHelper.findPattern(main.instructions.getFirst(), 
			        		new int[]{
			        				ALOAD,
			        				ALOAD,
			        				ILOAD,
			        				ILOAD,
			        				ALOAD,
			        				ALOAD,
			        				ACONST_NULL
			        				}, 
			        		"xxxxxxx"); //Before getItemModelWithOverrides and render method and loading vars
			
			AbstractInsnNode postNode = //right before dec zLevel
					ASMHelper.findPattern(main.instructions.getFirst(),
							new int[] {
									ATHROW
								},
							"x");
			
			Objects.requireNonNull(preNode, "pre node is null");
			Objects.requireNonNull(postNode, "post node is null");
			
			LabelNode jumpPast = new LabelNode();
			
			InsnList insnList = new InsnList();	
			
			insnList.add(new VarInsnNode(ALOAD, 2)); //ItemStack
			insnList.add(new VarInsnNode(ILOAD, 3)); //x Coord
			insnList.add(new VarInsnNode(ILOAD, 4)); //y Coord
			insnList.add(
			        ASMHelper.call(INVOKESTATIC, TypesHook.Methods.ForgeHaxHooks_onRenderItemAndEffectIntoGui));
			insnList.add(new JumpInsnNode(IFNE, jumpPast));
			
			main.instructions.insertBefore(preNode, insnList);
			main.instructions.insert(postNode, jumpPast);
			
		}
	}

}
