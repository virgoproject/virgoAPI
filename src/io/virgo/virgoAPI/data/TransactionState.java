package io.virgo.virgoAPI.data;

import java.util.HashMap;

import io.virgo.virgoAPI.crypto.TxOutput;

/**
 * Object representing a transaction's state
 */
public class TransactionState {

	private boolean found;
	private String uid;
	private TxStatus status;
	private int stability;
	private HashMap<String, TxOutput> outputs;
	
	/**
	 * 
	 * @param uid Target transaction UID
	 * @param status Target transaction's status
	 * @param stability Target transaction's stability
	 * @param outputs Target transaction's outputs
	 * @param found Has the transaction been found
	 */
	public TransactionState(String uid, TxStatus status, int stability, HashMap<String, TxOutput> outputs, boolean found) {
		this.uid = uid;
		this.status = status;
		this.stability = stability;
		this.outputs = outputs;
		this.found = found;
	}
	
	/**
	 * @return If the transaction has been found on network, check this before reading any other data
	 */
	public boolean hasBeenFound() {
		return found;
	}
	
	/**
	 * @return The Id of the transaction this object is about
	 */
	public String getUid() {
		return uid;
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
	public int getStability() {
		return stability;
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
		
		throw new IllegalArgumentException("Output address " + address + " not found for transaction " + uid);
	}
	
}