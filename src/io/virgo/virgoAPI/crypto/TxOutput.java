package io.virgo.virgoAPI.crypto;

import java.math.BigInteger;

import io.virgo.virgoAPI.BoltAPI;
import io.virgo.virgoAPI.utils.Miscellaneous;
import net.boltLabs.boltCryptoLib.Converter;

public class TxOutput {

	private String address;
	private long amount;
	private boolean isSpent;
	
	public TxOutput(String address, long amount) {
		if(!Miscellaneous.validateAddress(address, BoltAPI.ADDR_IDENTIFIER))
			throw new IllegalArgumentException(address + " is not a valid address");
		
		if(!Miscellaneous.validateAmount(amount))
			throw new IllegalArgumentException(amount + " is not a valid amount");
	
		this.address = address;
		this.amount = amount;
	}
	
	public TxOutput(String address, long amount, boolean isSpent) {
		this(address,amount);
		
		this.isSpent = isSpent;
	}
	
	/**
	 * Create a TxOutput from a string
	 * 
	 * @param inputString the string to convert to TxOutput, format: "address,amount" or "address,amount,claimedBy"
	 * @return a new TxOutput
	 * @throws NumberFormatException Given amount is not in hex format
	 * @throws ArithmeticException Given amount is out of range
	 * @throws IllegalArgumentException Can't build a TxOutput from this string
	 */
	public static TxOutput fromString(String inputString) throws ArithmeticException, IllegalArgumentException {
		
		String[] outArgs = inputString.split(",");
		
		switch(outArgs.length) {
		case 2:
			if(Miscellaneous.validateAddress(outArgs[0], BoltAPI.ADDR_IDENTIFIER))
				return new TxOutput(outArgs[0], Converter.hexToDec(outArgs[1]).longValueExact());
			break;
		case 3:
			if(Miscellaneous.validateAddress(outArgs[0], BoltAPI.ADDR_IDENTIFIER) && Miscellaneous.validateAddress(outArgs[2], BoltAPI.TX_IDENTIFIER))
				return new TxOutput(outArgs[0], Converter.hexToDec(outArgs[1]).longValueExact());
		}
		
		throw new IllegalArgumentException("Can't build a TxOutput from this string.");
	}
	
	public String getAddress() {
		return address;
	}
	
	public long getAmount() {
		return amount;
	}
	
	public String toString() {
		return address + "," + Converter.decToHex(BigInteger.valueOf(amount));
	}
	
	public boolean isSpent() {
		return isSpent;
	}
	
}
