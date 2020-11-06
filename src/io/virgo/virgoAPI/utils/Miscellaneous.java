package io.virgo.virgoAPI.utils;

import io.virgo.virgoAPI.VirgoAPI;

public class Miscellaneous {
	
	/**
	 * Check if an amount is a valid virgocoins amount (more than 0 and less or equal to VirgoAPI.TOTALUNITS)
	 * @param amount The amount to check
	 * @return true if given amount is valid, false otherwise
	 */
	public static boolean validateAmount(long amount) {
		if(amount <= 0 || amount > VirgoAPI.TOTALUNITS)
			return false;
		
		return true;
	}
	
}
