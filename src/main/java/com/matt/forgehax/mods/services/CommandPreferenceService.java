package com.matt.forgehax.mods.services;

import java.util.Comparator;

import com.matt.forgehax.Helper;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.ModManager;
import com.matt.forgehax.util.mod.loader.RegisterMod;

/*TheAlphaEpsilon
 * 
 */

@RegisterMod
public class CommandPreferenceService extends ServiceMod {
		
	public CommandPreferenceService() {
		super("CommandPreference", "Choose how you want commands listed");
	}
	
	enum COMMAND_SORT {
		
		ALPHABETE, TYPE, NONE;

		private static Comparator<Command> noneComp = null; // if comp is null in set it doesnt sort but uses insert order
		  
		private static Comparator<Command> alphabeteComp = new Comparator<Command>() {
			@Override
			public int compare(Command o1, Command o2) {
				return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
			}
		};
		  
		private static Comparator<Command> typeComp = new Comparator<Command>() {
			
			private int value(Command c) {
				
				if(c instanceof Setting<?>) {
					
					Setting<?> s = (Setting<?>)c;
					
					Object type = s.getDefault();
					
					if (type instanceof Boolean)
			            return 0;
					else if (type instanceof Float ||
							type instanceof Integer ||
							type instanceof Long ||
							type instanceof Double)
						return 1;
					else if (type instanceof Enum)
						return 2;
					else if (type instanceof Color)
			        	return 3;
					else
						return 4;
				} else {
					return 999;
				}
				
			}
			
			@Override
			public int compare(Command o1, Command o2) {
				int value1 = value(o1);
				int value2 = value(o2);
				
				if(value1 == value2)
					return alphabeteComp.compare(o1, o2);
				
				return value1 - value2;
			}
			  
		  };
		  
		  public Comparator<Command> comparator() {
			  switch(this) {
			  case ALPHABETE:
				  return alphabeteComp;
			  case TYPE:
				  return typeComp;
			  default:
				  return noneComp;
			  }
		  }
		  
	}
	 
	private final Setting<COMMAND_SORT> sort = getCommandStub()
	.builders()
	.<COMMAND_SORT>newSettingEnumBuilder()
	.name("sortType")
	.description("NONE, ALPHABETE, or TYPE")
	.defaultTo(COMMAND_SORT.NONE)
	.changed(data -> {
		
		COMMAND_SORT sort = data.getTo();
								
		ModManager.getInstance().forEach(mod -> mod.getCommandStub().reOrder(sort.comparator()));

	})
	.build();
	
	@Override
	protected void onLoad() {
	
		ModManager.getInstance().forEach(mod -> mod.getCommandStub().reOrder(sort.get().comparator()));
			  	
	}		  	
	
	
}
