package io.virgo.virgoAPI.data;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import io.virgo.virgoAPI.crypto.TxOutput;
import net.boltLabs.boltCryptoLib.Converter;
import net.boltLabs.boltCryptoLib.ECDSASignature;

public class Transaction {

	private String uid;
	
	private ECDSASignature signature;
	private byte[] pubKey;
	
	private String[] parents;
	private String[] inputs;
	
	private HashMap<String, TxOutput> outputs;
	
	private long date;
	
	public Transaction(String uid, ECDSASignature signature, byte[] pubKey, String[] parents, String[] inputs, HashMap<String, TxOutput> outputs, long date) {
		this.uid = uid;
		this.signature = signature;
		this.pubKey = pubKey;
		this.parents = parents;
		this.inputs = inputs;
		this.outputs = outputs;
		this.date = date;
	}
	
	public String getUid() {
		return uid;
	}
	
	public ECDSASignature getSignature() {
		return signature;
	}
	
	public byte[] getPublicKey() {
		return pubKey;
	}
	
	public String[] getParentsUids() {
		return parents;
	}
	
	public String[] getInputsUids() {
		return inputs;
	}
	
	public HashMap<String, TxOutput> getOutputsMap() {
		return outputs;
	}
	
	public TxOutput getOutput(String address) {
		return outputs.get(address);
	}
	
	public long getDate() {
		return date;
	}
	
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
