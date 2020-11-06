package io.virgo.virgoAPI.crypto;

import java.math.BigInteger;

import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoAPI.utils.Miscellaneous;
import io.virgo.virgoCryptoLib.Converter;
import io.virgo.virgoCryptoLib.Utils;

/**
 * Object representing a transaction output (or input)
 */
public class TxOutput {

	private String address;
	private long amount;
	private boolean isSpent;
	
	/**
	 * Create a new transaction output
	 * 
	 * @param address The recipient address (wich will receive the coins)
	 * @param amount The amount to send
	 */
	public TxOutput(String address, long amount) {
		if(!Utils.validateAddress(address, VirgoAPI.ADDR_IDENTIFIER))
			throw new IllegalArgumentException(address + " is not a valid address");
		
		if(!Miscellaneous.validateAmount(amount))
			throw new IllegalArgumentException(amount + " is not a valid amount");
	
		this.address = address;
		this.amount = amount;
	}
	
	/**
	 * Create a new transaction output
	 * 
	 * @param address The recipient address (wich will receive the coins)
	 * @param amount The amount to send
	 * @param isSpent Is the output already spent
	 */
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
			if(Utils.validateAddress(outArgs[0], VirgoAPI.ADDR_IDENTIFIER))
				return new TxOutput(outArgs[0], Converter.hexToDec(outArgs[1]).longValueExact());
			break;
		case 3:
			if(Utils.validateAddress(outArgs[0], VirgoAPI.ADDR_IDENTIFIER) && Utils.validateAddress(outArgs[2], VirgoAPI.TX_IDENTIFIER))
				return new TxOutput(outArgs[0], Converter.hexToDec(outArgs[1]).longValueExact());
		}
		
		throw new IllegalArgumentException("Can't build a TxOutput from this string.");
	}
	
	/**
	 * @return The recipient address
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * @return The output value
	 */
	public long getAmount() {
		return amount;
	}
	
	/**
	 * @return A string representation of this output
	 */
	public String toString() {
		return address + "," + Converter.decToHex(BigInteger.valueOf(amount));
	}
	
	/**
	 * @return Has the output been spent by a transaction
	 */
	public boolean isSpent() {
		return isSpent;
	}
	
}
