package io.virgo.virgoAPI.crypto;

import java.math.BigInteger;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoAPI.data.TxStatus;
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
	private HashMap<String, TxStatus> claimers = new HashMap<String, TxStatus>();
	
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
	 * Create a new transaction output
	 * 
	 * @param address The recipient address (wich will receive the coins)
	 * @param amount The amount to send
	 * @param claimers  HashMap of claiming transactions and their status
	 */
	public TxOutput(String address, long amount, boolean isSpent, HashMap<String, TxStatus> claimers) {
		this(address,amount,isSpent);
		
		this.claimers.putAll(claimers);
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
	
	public JSONObject toJSONObject() {
		JSONObject JSONRepresentation = new JSONObject();

		JSONRepresentation.put("address", address);
		JSONRepresentation.put("amount", amount);
		JSONRepresentation.put("isSpent", isSpent);
		
		JSONArray claimersJSON = new JSONArray();
		
		for(String claimer : claimers.keySet()) {
			JSONObject claimerJSON = new JSONObject();
			claimerJSON.put("uid", claimer);
			claimerJSON.put("status", claimers.get(claimer).getCode());
		}
		
		JSONRepresentation.put("claimers", claimersJSON);
		
		return JSONRepresentation;
	}
	
	public static TxOutput fromJSONObject(JSONObject JSONRepresentation) {
		
		JSONArray claimersJSON = JSONRepresentation.getJSONArray("claimers");
		
		HashMap<String, TxStatus> claimers = new HashMap<String, TxStatus>();
		for(int i = 0; i < claimersJSON.length(); i++) {
			JSONObject claimerJSON = claimersJSON.getJSONObject(i);
			claimers.put(claimerJSON.getString("uid"), TxStatus.fromCode(claimerJSON.getInt("status")));
		}
		
		return new TxOutput(JSONRepresentation.getString("address"), JSONRepresentation.getLong("amount"), JSONRepresentation.getBoolean("isSpent"), claimers);
		
	}
	
	/**
	 * @return Has the output been spent by a transaction
	 */
	public boolean isSpent() {
		return isSpent;
	}
	
	public HashMap<String, TxStatus> getClaimers(){
		return new HashMap<String, TxStatus>(claimers);
	}
	
}
