package com.matt.forgehax.asm;

import java.util.UUID;

import com.matt.forgehax.asm.utils.asmtype.ASMClass;
import com.matt.forgehax.asm.utils.asmtype.ASMField;
import com.matt.forgehax.asm.utils.asmtype.ASMMethod;
import com.matt.forgehax.asm.utils.asmtype.builders.ASMBuilders;
import com.matt.forgehax.util.FHTextures;

/**
 * Created on 5/29/2017 by fr1kin
 */
public interface TypesSpecial {
  
  interface Classes {
    
    ASMClass SchematicPrinter =
      ASMBuilders.newClassBuilder()
        .setClassName("com/github/lunatrius/schematica/client/printer/SchematicPrinter")
        .build();
    
    ASMClass FHTextures =
    		ASMBuilders.newClassBuilder()
    		.setClassName("com/matt/forgehax/util/FHTextures")
    		.build();
    
  }
  
  interface Fields {
  
  }
  
  interface Methods {
    
	  ASMMethod FHTextures_getResource =
			  Classes.FHTextures.childMethod()
			  .setName("getResource")
			  .setReturnType(TypesMc.Classes.ResourceLocation)
			  .beginParameters()
			  .unobfuscated()
			  .add(UUID.class)
			  .add(int.class)
			  .finish()
			  .build();
			  
	  ASMMethod FHTextures_addPlayer =
			  Classes.FHTextures.childMethod()
			  .setName("addPlayer")
			  .setReturnType(void.class)
			  .beginParameters()
			  .unobfuscated()
			  .add(UUID.class)
			  .finish()
			  .build();
	  
	  ASMMethod FHTextures_removePlayer =
			  Classes.FHTextures.childMethod()
			  .setName("removePlayer")
			  .setReturnType(void.class)
			  .beginParameters()
			  .unobfuscated()
			  .add(UUID.class)
			  .finish()
			  .build();
	  
    ASMMethod SchematicPrinter_placeBlock =
      Classes.SchematicPrinter.childMethod()
        .setName("placeBlock")
        .setReturnType(boolean.class)
        .beginParameters()
        .unobfuscated()
        .add(TypesMc.Classes.WorldClient)
        .add(TypesMc.Classes.EntityPlayerSP)
        .add(TypesMc.Classes.ItemStack)
        .add(TypesMc.Classes.BlockPos)
        .add(TypesMc.Classes.EnumFacing)
        .add(TypesMc.Classes.Vec3d)
        .add(TypesMc.Classes.EnumHand)
        .finish()
        .build();
  }
}
