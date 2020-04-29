package io.virgo.virgoAPI.data;

import java.util.HashMap;

import io.virgo.virgoAPI.crypto.TxOutput;

public class TransactionState {

	private boolean found;
	private String uid;
	private TxStatus status;
	private int stability;
	private HashMap<String, TxOutput> outputs;
	
	public TransactionState(String uid, TxStatus status, int stability, HashMap<String, TxOutput> outputs, boolean found) {
		this.uid = uid;
		this.status = status;
		this.stability = stability;
		this.outputs = outputs;
		this.found = found;
	}
	
	public boolean hasBeenFound() {
		return found;
	}
	
	public String getUid() {
		return uid;
	}
	
	public TxStatus getStatus() {
		return status;
	}

	public int getStability() {
		return stability;
	}
	
	public HashMap<String, TxOutput> getOutputsMap() {
		return outputs;
	}
	
	public TxOutput getOutput(String address) {
		return outputs.get(address);
	}
	
	public boolean hasOutput(String address) {
		return outputs.containsKey(address);
	}
	
	public boolean isOutputSpent(String address) {
		if(hasOutput(address))
			return outputs.get(address).isSpent();
		
		throw new IllegalArgumentException("Output address " + address + " not found for transaction " + uid);
	}
	
}