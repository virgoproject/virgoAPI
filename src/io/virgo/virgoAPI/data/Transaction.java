package io.virgo.virgoAPI.data;

import java.nio.ByteBuffer;
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

/**
 * Object representing a transaction
 */
public class Transaction {

	private Sha256Hash hash;
	
	private ECDSASignature signature;
	private byte[] pubKey;
	
	private Sha256Hash[] parents;
	private Sha256Hash[] inputs;
	
	private HashMap<String, TxOutput> outputs;
	
	private long date;
	
	private Sha256Hash parentBeacon;
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
	public Transaction(Sha256Hash hash, ECDSASignature signature, byte[] pubKey, Sha256Hash[] parents, Sha256Hash[] inputs, HashMap<String, TxOutput> outputs, Sha256Hash parentBeacon, long nonce, long date) {
		this.hash = hash;
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
	public Sha256Hash getHash() {
		return hash;
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
	public Sha256Hash[] getParentsHashes() {
		return parents;
	}
	
	public ArrayList<String> getParentsHashesStrings() {
		ArrayList<String> hashes = new ArrayList<String>();
		for(Sha256Hash parentHash : parents)
			hashes.add(parentHash.toString());
		
		return hashes;
	}
	
	/**
	 * @return The input transactions's ids
	 */
	public Sha256Hash[] getInputsHashes() {
		return inputs;
	}
	
	public ArrayList<String> getInputsHashesStrings() {
		ArrayList<String> hashes = new ArrayList<String>();
		for(Sha256Hash inputHash : inputs)
			hashes.add(inputHash.toString());
		
		return hashes;
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
	
	public Sha256Hash getParentBeacon() {
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
		
		if(getHash().equals(new Sha256Hash("025a6f04e7047b713aaba7fc5003c8266302918c25d1526507becad795b01f3a"))) {
			txJson.put("genesis", true);
			return txJson;
		}
		
		txJson.put("parents", new JSONArray(getParentsHashesStrings()));
		
		if(parentBeacon == null) {
			txJson.put("sig", getSignature().toHexString());
			txJson.put("pubKey", Converter.bytesToHex(getPublicKey()));
			txJson.put("inputs", new JSONArray(getInputsHashesStrings()));
		} else {
			txJson.put("parentBeacon", parentBeacon.toString());
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
			return new Transaction(new Sha256Hash("025a6f04e7047b713aaba7fc5003c8266302918c25d1526507becad795b01f3a"),null,null,new Sha256Hash[0],new Sha256Hash[0], genesisOutputs, null, 0, 0);
		}
		
		Sha256Hash txHash;
		
		if(JSONRepresentation.has("parentBeacon"))
			txHash = Sha256.getDoubleHash((JSONRepresentation.getJSONArray("parents").toString()
					+ JSONRepresentation.getJSONArray("outputs").toString()
					+ JSONRepresentation.getString("parentBeacon")
					+ JSONRepresentation.getLong("date")
					+ JSONRepresentation.getLong("nonce")).getBytes());
		else
			txHash = Sha256.getDoubleHash(Converter.concatByteArrays(
					(JSONRepresentation.getJSONArray("parents").toString() + JSONRepresentation.getJSONArray("inputs").toString() + JSONRepresentation.getJSONArray("outputs").toString()).getBytes(),
					Converter.hexToBytes(JSONRepresentation.getString("sig")), Converter.hexToBytes(JSONRepresentation.getString("pubKey")), longToBytes(JSONRepresentation.getLong("date"))));
		
		ECDSASignature sig = null;
		byte[] pubKey = null;
		
		JSONArray parents = JSONRepresentation.getJSONArray("parents");
		
		ArrayList<Sha256Hash> inputsArray = new ArrayList<Sha256Hash>();
		JSONArray inputs = new JSONArray();
		
		if(!JSONRepresentation.has("parentBeacon"))
			inputs = JSONRepresentation.getJSONArray("inputs");
		
		JSONArray outputs = JSONRepresentation.getJSONArray("outputs");
		
		long date = JSONRepresentation.getLong("date");
		
		Sha256Hash parentBeacon = null;
		long nonce = 0;
		
		ECDSA signer = new ECDSA();
		
		//check if signature is good
		if(!JSONRepresentation.has("parentBeacon")) {
			 sig = ECDSASignature.fromByteArray(Converter.hexToBytes(JSONRepresentation.getString("sig")));
			 pubKey = Converter.hexToBytes(JSONRepresentation.getString("pubKey"));
			
			 if(!signer.Verify(txHash, sig, pubKey))
				return null;
		
			//clean and verify inputs
			for(int i2 = 0; i2 < inputs.length(); i2++) {
				try {
					inputsArray.add(new Sha256Hash(inputs.getString(i2)));
				}catch(IllegalArgumentException e) {
					break;
				}
			}
		}else {
			parentBeacon = new Sha256Hash(JSONRepresentation.getString("parentBeacon"));
			nonce = JSONRepresentation.getLong("nonce");
		}

		//clean and verify parents
		ArrayList<Sha256Hash> parentsArray = new ArrayList<Sha256Hash>();
		for(int i2 = 0; i2 < parents.length(); i2++) {
			try {
				parentsArray.add(new Sha256Hash(parents.getString(i2)));
			}catch(IllegalArgumentException e) {
				break;
			}
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
			
			return new Transaction(txHash, sig, pubKey,
					parentsArray.toArray(new Sha256Hash[0]), inputsArray.toArray(new Sha256Hash[0]), outputsArray, parentBeacon, nonce, date);
			
		}else {
			return null;
		}
	}
	
	public static byte[] longToBytes(long x) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.putLong(x);
	    return buffer.array();
	}
	
}
