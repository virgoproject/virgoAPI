package io.virgo.virgoAPI.utils;

import io.virgo.virgoAPI.VirgoAPI;

public class Miscellaneous {
	
	public static boolean validateAmount(long amount) {
		if(amount <= 0 || amount > VirgoAPI.TOTALUNITS)
			return false;
		
		return true;
	}
	
}
