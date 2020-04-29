package io.virgo.virgoAPI.utils;

import io.virgo.virgoAPI.BoltAPI;
import net.boltLabs.boltCryptoLib.Base58;
import net.boltLabs.boltCryptoLib.Exceptions.Base58FormatException;

public class Miscellaneous {

	public static boolean validateAddress(String hash, byte[] prefix) {
		try {
			byte[] decodedAddr = Base58.decodeChecked(hash);
			if(!byteArrayStartsWith(decodedAddr, 0, prefix))
				return false;
			return true;
		}catch(Base58FormatException e) {
			return false;
		}
	}
	
	public static boolean validateAmount(long amount) {
		if(amount <= 0 || amount > BoltAPI.TOTALUNITS)
			return false;
		
		return true;
	}
	
	public static boolean byteArrayStartsWith(byte[] source, int offset, byte[] match) {

		if(match.length > (source.length - offset))
			return false;

		for(int i = 0; i < match.length; i++)
	    	if(source[offset + i] != match[i])
	    		return false;
	    
		return true;
	}
	
}
