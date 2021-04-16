package io.virgo.virgoAPI.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoAPI.crypto.TxOutput;
import io.virgo.virgoCryptoLib.Converter;
import io.virgo.virgoCryptoLib.ECDSA;
import io.virgo.virgoCryptoLib.ECDSASignature;
import io.virgo.virgoCryptoLib.Sha256;
import io.virgo.virgoCryptoLib.Sha256Hash;
import io.virgo.virgoCryptoLib.Utils;

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
	
	private String parentBeacon;
	private long nonce;
	
	/**
	 * @param uid The transaction ID
	 * @param signature The transaction signature
	 * @param pubKey The transaction's address public key
	 * @param parents The transaction parents ids
	 * @param inputs The transaction inputs ids
	 * @param outputs The transaction outputs
	 * @param date The transaction date
	 */
	public Transaction(String uid, ECDSASignature signature, byte[] pubKey, String[] parents, String[] inputs, HashMap<String, TxOutput> outputs, String parentBeacon, long nonce, long date) {
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
	
	public String getParentBeacon() {
		return parentBeacon;
	}
	
	/**
	 * @return The transaction's emission date
	 */
	public long getDate() {
		return date;
	}
	
	public long getNonce() {
		return nonce;
	}
	
	/**
	 * @return A {@link JSONObject} representing this transaction
	 */
	public JSONObject toJSONObject() {
		
		JSONObject txJson = new JSONObject();
		
		if(getUid().equals("TXfxpq19sBUFgd8LRcUgjg1NdGK2ZGzBBdN")) {
			txJson.put("genesis", true);
			return txJson;
		}
		
		txJson.put("parents", new JSONArray(getParentsUids()));
		
		if(parentBeacon == null) {
			txJson.put("sig", getSignature().toHexString());
			txJson.put("pubKey", Converter.bytesToHex(getPublicKey()));
			txJson.put("inputs", new JSONArray(getInputsUids()));
		} else {
			txJson.put("parentBeacon", parentBeacon);
			txJson.put("nonce", getNonce());
		}
		
		JSONArray outputsJson = new JSONArray();
		for(Map.Entry<String, TxOutput> entry : getOutputsMap().entrySet())
		   outputsJson.put(entry.getValue().toString());
		txJson.put("outputs", outputsJson);
		
		txJson.put("date", getDate());
		
		return txJson;
	}
	
	public static Transaction fromJSONObject(JSONObject JSONRepresentation) {
		
		if(JSONRepresentation.has("genesis")) {
			HashMap<String, TxOutput> genesisOutputs = new HashMap<String, TxOutput>();
			genesisOutputs.put("V2N5tYdd1Cm1xqxQDsY15x9ED8kyAUvjbWv", new TxOutput("V2N5tYdd1Cm1xqxQDsY15x9ED8kyAUvjbWv",(long) (100000 * Math.pow(10, VirgoAPI.DECIMALS))));
			return new Transaction("TXfxpq19sBUFgd8LRcUgjg1NdGK2ZGzBBdN",null,null,new String[0],new String[0], genesisOutputs, "", 0, 0);
		}
		
		String uid = "";
		
		if(JSONRepresentation.has("parentBeacon"))
			uid = Converter.Addressify(Sha256.getDoubleHash((JSONRepresentation.getJSONArray("parents").toString()
					+ JSONRepresentation.getJSONArray("outputs").toString()
					+ JSONRepresentation.getString("parentBeacon")
					+ JSONRepresentation.getLong("date")
					+ JSONRepresentation.getLong("nonce")).getBytes()).toBytes(), VirgoAPI.TX_IDENTIFIER);
		else
			uid = Converter.Addressify(Converter.hexToBytes(JSONRepresentation.getString("sig")), VirgoAPI.TX_IDENTIFIER);
		
		ECDSASignature sig = null;
		byte[] pubKey = null;
		
		JSONArray parents = JSONRepresentation.getJSONArray("parents");
		
		ArrayList<String> inputsArray = new ArrayList<String>();
		JSONArray inputs = new JSONArray();
		if(!JSONRepresentation.has("parentBeacon"))
			inputs = JSONRepresentation.getJSONArray("inputs");
		
		JSONArray outputs = JSONRepresentation.getJSONArray("outputs");
		
		long date = JSONRepresentation.getLong("date");
		
		String parentBeacon = null;
		long nonce = 0;
		
		ECDSA signer = new ECDSA();
		
		//check if signature is good
		if(!JSONRepresentation.has("parentBeacon")) {
			 sig = ECDSASignature.fromByteArray(Converter.hexToBytes(JSONRepresentation.getString("sig")));
			 pubKey = Converter.hexToBytes(JSONRepresentation.getString("pubKey"));
			
			Sha256Hash TxHash = Sha256.getDoubleHash((parents.toString() + inputs.toString() + outputs.toString() + date).getBytes());
			if(!signer.Verify(TxHash, sig, pubKey))
				return null;
		
			//clean and verify inputs
			for(int i2 = 0; i2 < inputs.length(); i2++) {
				String inputTx = inputs.getString(i2);
				if(!Utils.validateAddress(inputTx, VirgoAPI.TX_IDENTIFIER))
					break;
				
				inputsArray.add(inputTx);
			}
		}else {
			parentBeacon = JSONRepresentation.getString("parentBeacon");
			nonce = JSONRepresentation.getLong("nonce");
		}

		//clean and verify parents
		ArrayList<String> parentsArray = new ArrayList<String>();
		for(int i2 = 0; i2 < parents.length(); i2++) {
			String parentTx = parents.getString(i2);
			if(!Utils.validateAddress(parentTx, VirgoAPI.TX_IDENTIFIER))
				break;
			
			parentsArray.add(parentTx);
		}							

		//clean and verify ouputs
		HashMap<String, TxOutput> outputsArray = new HashMap<String, TxOutput>();
		for(int i2 = 0; i2 < outputs.length(); i2++) {
			String outputString = outputs.getString(i2);
			try {
				TxOutput output = TxOutput.fromString(outputString);
				outputsArray.put(output.getAddress(), output);
			}catch(IllegalArgumentException e) {
				break;
			}
		}
		
		//If everything has been successfully verified add transaction, else goto next iteration
		if(inputsArray.size() == inputs.length() && parentsArray.size() == parents.length()
				&& outputsArray.size() == outputs.length()) {
			
			return new Transaction(uid, sig, pubKey,
					parentsArray.toArray(new String[0]), inputsArray.toArray(new String[0]), outputsArray, parentBeacon, nonce, date);
			
		}else {
			return null;
		}
	}
	
}
