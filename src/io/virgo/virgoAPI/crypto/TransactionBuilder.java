package io.virgo.virgoAPI.crypto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoAPI.data.AddressTxs;
import io.virgo.virgoAPI.data.TransactionState;
import io.virgo.virgoAPI.requestsResponses.GetAddressesTxsResponse;
import io.virgo.virgoAPI.requestsResponses.GetTipsResponse;
import io.virgo.virgoAPI.requestsResponses.GetTxsStateResponse;
import io.virgo.virgoCryptoLib.Converter;
import io.virgo.virgoCryptoLib.ECDSASignature;
import io.virgo.virgoCryptoLib.Sha256;
import io.virgo.virgoCryptoLib.Sha256Hash;
import io.virgo.geoWeb.Peer;
import io.virgo.geoWeb.ResponseCode;
import io.virgo.geoWeb.SyncMessageResponse;

/**
 * Builder to create a new transaction
 * 
 * 
 * <p>
 * Example:<br><br>
 * {@code TxOutput output = new TxOutput(recipient,amountToSend);//prepare an output}<br><br>
 * {@code JSONObject rawTransaction = new TransactionBuilder()}//Create a builder<br>
 * {@code .address(address)}//Specify the {@link Address} we send with<br>
 * {@code .output(output)}//Add the {@link TxOutput}, can add multiple<br>
 * {@code .send(privateKey);//Sign an broadcast transaction, must be the privateKey corresponding to the specified address.}
 * <p>
 */
//TODO: add an option to broadcast directly, add a method to just get the transaction JSON and return a status code instead of transaction JSON
public class TransactionBuilder {

	private Address address = null;
	private HashMap<String, TxOutput> outputs = new HashMap<String, TxOutput>();
	
	public TransactionBuilder() {
		
	}
	
	/**
	 * Specify the {@link Address} you want to send with 
	 * 
	 * @param address The {@link Address} you want to send with
	 * @return The current TransactionBuilder
	 */
	public TransactionBuilder address(Address address) {
		this.address = address;
		
		return this;
	}
	
	/**
	 * Add an output to this transaction
	 * 
	 * @param output The {@link TxOutput} you want to add
	 * @return The current TransactionBuilder
	 */
	public TransactionBuilder output(TxOutput output) {
		outputs.put(output.getAddress(), output);
		
		return this;
	}
	
	/**
	 * Sign and broadcast the transaction
	 * 
	 * @param privateKey The private key corresponding to the provided address
	 * @return the raw transaction in form of a {@link JSONObject}
	 * @throws IllegalStateException If no address or output has been defined
	 * @throws IOException If wrong private key or if we failed to retrieve necessary informations from peers
	 */
	public JSONObject send(byte[] privateKey) throws IOException {
		
		//First check if everything needed is there and valid
		if(address == null)
			throw new IllegalStateException("no address defined");
		
		if(outputs.size() <= 0)
			throw new IllegalStateException("no output defined");
		
		if(!address.checkAgainstPrivateKey(privateKey))
			throw new IllegalArgumentException("Given private key doesn't correspond to given address");
		
		//Get address transactions from peers
		String[] addr = {address.getAddress()};
		GetAddressesTxsResponse addrTxsResp = VirgoAPI.getInstance().getAddressesTxs(addr);
		
		if(addrTxsResp.getResponseCode() != ResponseCode.OK)
			throw new IOException("Unable to get address transactions from remote");
		
		AddressTxs addrTxs = addrTxsResp.getAddressTxs(address.getAddress());
		
		//Get retrieved transactions's states
		GetTxsStateResponse txsStateResp = VirgoAPI.getInstance().getTxsState(addrTxs.getInputs());
		
		if(txsStateResp.getResponseCode() != ResponseCode.OK)
			throw new IOException("Unable to get transactions states from remote");
		
		//From states, calculate how much is spendable
		ArrayList<String> unspentInputs = new ArrayList<String>();
		long inputsValue = 0;
		
		ArrayList<TransactionState> inputsState = txsStateResp.getStates();
		for(TransactionState state : inputsState) {
			try {
				if(state.hasBeenFound() && !state.isOutputSpent(address.getAddress())) {
					long amount = state.getOutput(address.getAddress()).getAmount();
					unspentInputs.add(state.getUid());
					inputsValue += amount;
				}				
			}catch(IllegalArgumentException e) {
				System.out.println(e.getMessage());
			}
				
		}
		
		//Calculate how much we try to spend
		long outputsValue = 0;
		for(TxOutput output : outputs.values())
			outputsValue += output.getAmount();
				
		
		//If we are trying to spend more than allowed throw an error
		if(inputsValue-outputsValue*VirgoAPI.FEES_RATE < outputsValue)
			throw new IllegalArgumentException("Trying to spend more than allowed ("+outputsValue+" / " + inputsValue +")");
		
		//If we are spending less than allowed create an output to get back whats remain
		else if(inputsValue > outputsValue)
			outputs.put(address.getAddress(), new TxOutput(address.getAddress(),inputsValue-outputsValue-(long)(outputsValue/200)));
			
		
		//Get the tips we will attach the transaction to from peers
		GetTipsResponse getTipsResp = VirgoAPI.getInstance().getTips();
		if(getTipsResp.getResponseCode() != ResponseCode.OK)
			throw new IOException("Unable to get last tips from remote");
		
		//Create the transaction JSON
		JSONObject transaction = new JSONObject();
		transaction.put("parents", new JSONArray(getTipsResp.getTips()));
		transaction.put("inputs", new JSONArray(unspentInputs));
		
		JSONArray outputsJSON = new JSONArray();
		for(TxOutput output : outputs.values()) {
			outputsJSON.put(output.toString());
		}
		transaction.put("outputs", outputsJSON);
		
		long date = System.currentTimeMillis();
		
		transaction.put("date", date);
		
		transaction.put("pubKey", Converter.bytesToHex(address.getPublicKey(privateKey)));
		
		Sha256Hash txHash = Sha256.getHash((transaction.getJSONArray("parents").toString() +
				transaction.getJSONArray("inputs").toString() +
				outputsJSON.toString() + 
				date)
				.getBytes());
		
		//Sign transaction
		ECDSASignature sig = address.sign(txHash, privateKey);
		transaction.put("sig", sig.toHexString());
		
		//Create tx message to send to peers
		JSONObject txMessage = new JSONObject();
		txMessage.put("command", "tx");
		txMessage.put("tx", transaction);
		txMessage.put("callback", true);
		
		//broadcast it and return raw transaction
		Peer bestPeer = VirgoAPI.getInstance().getPeersWatcher().getPeersByScore().get(0);
		
		SyncMessageResponse txSubmissionResp = bestPeer.sendSyncMessage(txMessage);
		if(txSubmissionResp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT && txSubmissionResp.getResponse().getBoolean("result") == true) {
			VirgoAPI.getInstance().broadCast(txMessage, Arrays.asList(new Peer[] {bestPeer}) );
			return transaction;
		}
		
		return null;
	}
	
}
