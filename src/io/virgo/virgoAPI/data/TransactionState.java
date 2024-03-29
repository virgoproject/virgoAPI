package io.virgo.virgoAPI.data;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import io.virgo.virgoAPI.crypto.TxOutput;
import io.virgo.virgoCryptoLib.Sha256Hash;

/**
 * Object representing a transaction's state
 */
public class TransactionState {

	private Sha256Hash hash;
	private TxStatus status;
	private int confirmations;
	private Sha256Hash beacon;
	private HashMap<String, TxOutput> outputs;
	
	/**
	 * 
	 * @param uid Target transaction UID
	 * @param status Target transaction's status
	 * @param stability Target transaction's stability
	 * @param outputs Target transaction's outputs
	 * @param found Has the transaction been found
	 */
	public TransactionState(Sha256Hash hash, TxStatus status, Sha256Hash beacon, int confirmations, HashMap<String, TxOutput> outputs) {
		this.hash = hash;
		this.status = status;
		this.beacon = beacon;
		this.confirmations = confirmations;
		this.outputs = outputs;
	}
	
	/**
	 * @return The Id of the transaction this object is about
	 */
	public Sha256Hash getHash() {
		return hash;
	}
	
	/**
	 * @return The transaction's status
	 * Note: This doesn't update, if you want newer data request it again using the API
	 */
	public TxStatus getStatus() {
		return status;
	}

	/**
	 * @return The transaction's stability
	 * Note: This doesn't update, if you want newer data request it again using the API
	 */
	public int getConfirmations() {
		return confirmations;
	}
	
	/**
	 * @return ID of the beacon that is confirming this transaction
	 * Note: This doesn't update, if you want newer data request it again using the API
	 */
	public Sha256Hash getBeaconHash() {
		return beacon;
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
	 * Check if this transaction has an output targeting the desired address
	 * @param address The address to check an output for
	 * @return true if there is an output for the given address, false otherwise
	 */
	public boolean hasOutput(String address) {
		return outputs.containsKey(address);
	}
	
	/**
	 * Check if the output for the given transaction has been spent
	 * @param address The address to check for
	 * @return true if the output has been spent, false otherwise
	 * @throws {@link IllegalArgumentException} if no output has been found for desired address
	 */
	public boolean isOutputSpent(String address) {
		if(hasOutput(address))
			return outputs.get(address).isSpent();
		
		throw new IllegalArgumentException("Output address " + address + " not found for transaction " + hash.toString());
	}
	
	public JSONObject toJSONObject() {
		JSONObject JSONRepresentation = new JSONObject();
		
		JSONRepresentation.put("uid", hash.toString());
		JSONRepresentation.put("status", status.getCode());
		if(beacon != null)
			JSONRepresentation.put("beacon", beacon.toString());
		else
			JSONRepresentation.put("beacon", "");
		JSONRepresentation.put("confirmations", confirmations);

		JSONArray outputsJSON = new JSONArray();
		
		for(TxOutput out : outputs.values()) {
			outputsJSON.put(out.toJSONObject());
		}
		
		JSONRepresentation.put("outputs", outputsJSON);
		
		return JSONRepresentation;
	}
	
	public static TransactionState fromJSONObject(JSONObject JSONRepresentation) {
		JSONArray outputsJSON = JSONRepresentation.getJSONArray("outputs");
		
		HashMap<String, TxOutput> outputs = new HashMap<String, TxOutput>();
		for(int i = 0; i < outputsJSON.length(); i++) {
			TxOutput output = TxOutput.fromJSONObject(outputsJSON.getJSONObject(i));
			outputs.put(output.getAddress(), output);
		}
		
		Sha256Hash beacon = null;
		
		if(!JSONRepresentation.getString("beacon").equals(""))
			new Sha256Hash(JSONRepresentation.getString("beacon"));
		
		return new TransactionState(new Sha256Hash(JSONRepresentation.getString("uid")), TxStatus.fromCode(JSONRepresentation.getInt("status")),
				beacon, JSONRepresentation.getInt("confirmations"), outputs);
	}
	
}