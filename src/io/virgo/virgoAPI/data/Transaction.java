package io.virgo.virgoAPI.data;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoAPI.crypto.TxOutput;
import io.virgo.virgoCryptoLib.Converter;
import io.virgo.virgoCryptoLib.ECDSASignature;

/**
 * Object representing a transaction
 */
public class Transaction {

	private String uid;
	
	private ECDSASignature signature;
	private byte[] pubKey;
	
	private String[] parents;
	private String[] inputs;
	
	private HashMap<String, TxOutput> outputs;
	
	private long date;
	
	/**
	 * @param uid The transaction ID
	 * @param signature The transaction signature
	 * @param pubKey The transaction's address public key
	 * @param parents The transaction parents ids
	 * @param inputs The transaction inputs ids
	 * @param outputs The transaction outputs
	 * @param date The transaction date
	 */
	public Transaction(String uid, ECDSASignature signature, byte[] pubKey, String[] parents, String[] inputs, HashMap<String, TxOutput> outputs, long date) {
		this.uid = uid;
		this.signature = signature;
		this.pubKey = pubKey;
		this.parents = parents;
		this.inputs = inputs;
		this.outputs = outputs;
		this.date = date;
	}
	
	/**
	 * @return The transaction ID
	 */
	public String getUid() {
		return uid;
	}
	
	/**
	 * @return The transaction signature
	 */
	public ECDSASignature getSignature() {
		return signature;
	}
	
	/**
	 * @return The transaction's address public key
	 */
	public byte[] getPublicKey() {
		return pubKey;
	}
	
	/**
	 * @return The transaction's address
	 */
	public String getAddress() {
		byte[] pubKey = getPublicKey();
		
		if(pubKey == null)
			return "";
		
		return Converter.Addressify(pubKey, VirgoAPI.ADDR_IDENTIFIER);
	}
	
	/**
	 * @return The parent transactions's ids
	 */
	public String[] getParentsUids() {
		return parents;
	}
	
	/**
	 * @return The input transactions's ids
	 */
	public String[] getInputsUids() {
		return inputs;
	}
	
	/**
	 * @return The transaction's outputs 
	 */
	public HashMap<String, TxOutput> getOutputsMap() {
		return outputs;
	}
	
	/**
	 * Get an output from this transaction for the given address
	 * 
	 * @param address The address you want to retrieve for
	 * @return The output targeting the given address if it exists, null otherwise
	 */
	public TxOutput getOutput(String address) {
		return outputs.get(address);
	}
	
	/**
	 * @return The transaction's emission date
	 */
	public long getDate() {
		return date;
	}
	
	/**
	 * @return A {@link JSONObject} representing this transaction
	 */
	public JSONObject toJSONObject() {
		JSONObject txJson = new JSONObject();
		txJson.put("sig", getSignature().toHexString());
		txJson.put("pubKey", Converter.bytesToHex(getPublicKey()));
		txJson.put("parents", new JSONArray(getParentsUids()));
		txJson.put("inputs", new JSONArray(getInputsUids()));
		
		JSONArray outputsJson = new JSONArray();
		for(TxOutput output : getOutputsMap().values())
		   outputsJson.put(output.toString());
		txJson.put("outputs", outputsJson);
		
		txJson.put("date", getDate());
		
		return txJson;
	}
	
}
