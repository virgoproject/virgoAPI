package io.virgo.virgoAPI.crypto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.virgoAPI.data.TransactionState;
import io.virgo.virgoAPI.data.TxStatus;
import io.virgo.virgoAPI.network.Provider;
import io.virgo.virgoAPI.network.Response;
import io.virgo.virgoAPI.network.ResponseCode;
import io.virgo.virgoAPI.requestsResponses.GetAddressesTxsResponse;
import io.virgo.virgoAPI.requestsResponses.GetTipsResponse;
import io.virgo.virgoAPI.requestsResponses.GetTxsStateResponse;
import io.virgo.virgoCryptoLib.Converter;
import io.virgo.virgoCryptoLib.ECDSASignature;
import io.virgo.virgoCryptoLib.Sha256;
import io.virgo.virgoCryptoLib.Sha256Hash;
import io.virgo.virgoCryptoLib.Utils;

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
	private ArrayList<String> parents = new ArrayList<String>();
	private ArrayList<String> inputs = new ArrayList<String>();
	private HashMap<String, TxOutput> outputs = new HashMap<String, TxOutput>();
	
	private boolean validateAmounts = true;
	
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
	 * Add the provided Transaction to the new one's parents
	 * <br><br>
	 * Manually adding parents will disable automatic addition of valid ones.<br>
	 * If you just want to send basic transactions no need to use this method
	 * 
	 * @param txId The transaction to add as parent
	 * @return The current TransactionBuilder
	 */
	public TransactionBuilder parent(String txId) {
		assert(Utils.validateAddress(txId, VirgoAPI.TX_IDENTIFIER));
		
		parents.add(txId);
		
		return this;
	}
	
	/**
	 * Add the provided Transaction to the new one's inputs
	 * <br><br>
	 * Manually adding inputs will disable automatic addition of valid ones.<br>
	 * If you just want to send basic transactions no need to use this method
	 * 
	 * @param txId The transaction to add as input
	 * @return The current TransactionBuilder
	 */
	public TransactionBuilder input(String txId) {
		assert(Utils.validateAddress(txId, VirgoAPI.TX_IDENTIFIER));
		
		inputs.add(txId);
		
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
	 * Enable or not checking if inputs have enough value for this transaction
	 * Disabling this will prevent the builder from creating a return output
	 * True by default
	 * 
	 * @return The current TransactionBuilder
	 */
	public TransactionBuilder validateAmounts(boolean validateAmounts) {
		this.validateAmounts = validateAmounts;
		
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
		
		//Calculate how much we try to spend
		long outputsValue = 0;
		for(TxOutput output : outputs.values())
			outputsValue += output.getAmount();
				
		//From states, calculate how much is spendable
		long inputsValue = 0;
		
		ArrayList<String> unspentInputs = new ArrayList<String>();
		
		if(inputs.size() == 0) {
			
			int page = 1;
			
			while(inputsValue < outputsValue) {
				//Get address transactions from peers
				String[] addr = {address.getAddress()};
				GetAddressesTxsResponse addrTxsResp = VirgoAPI.getInstance().getAddressesInputs(addr, 10, page);
				
				if(addrTxsResp.getResponseCode() != ResponseCode.OK)
					throw new IOException("Unable to get address transactions from remote");
				
				GetTxsStateResponse txsStateResp = VirgoAPI.getInstance().getTxsState(addrTxsResp.getAddressTxs(address.getAddress()).getTransactions());
				
				if(txsStateResp.getResponseCode() != ResponseCode.OK)
					throw new IOException("Unable to get transactions states from remote");
				
				statesFor:
				for(TransactionState state : txsStateResp.getStates()) {
					
					if(state.getStatus().isRefused() || state.isOutputSpent(address.getAddress()))
						continue;
					
					for(TxStatus status : state.getOutput(address.getAddress()).getClaimers().values())
						if(status.isPending())
							continue statesFor;
					
					inputsValue += state.getOutput(address.getAddress()).getAmount();
					
					if(!unspentInputs.contains(state.getUid()))
						unspentInputs.add(state.getUid());
					
					if(inputsValue >= outputsValue)
						break;
				}
				
				page++;
				
				if(addrTxsResp.getAddressTxs(address.getAddress()).getTransactions().length < 10)
					break;

			}
			
			if(inputsValue < outputsValue)
				throw new IllegalArgumentException("Trying to spend more than allowed ("+outputsValue+" / " + inputsValue +")");

			if(outputsValue < inputsValue)
				outputs.put(address.getAddress(), new TxOutput(address.getAddress(),inputsValue-outputsValue));
			
			validateAmounts = false;
		}
		
		//amounts verification process, make sure we don't try to spend more than allowed and that we are not paying more fees than needed
		if(validateAmounts) {
			//Get inputs states
			GetTxsStateResponse txsStateResp = VirgoAPI.getInstance().getTxsState(inputs.toArray(new String[inputs.size()]));
			
			if(txsStateResp.getResponseCode() != ResponseCode.OK)
				throw new IOException("Unable to get transactions states from remote");
			
			ArrayList<TransactionState> inputsState = txsStateResp.getStates();
			statesFor:
			for(TransactionState state : inputsState) {
				try {
					if(!state.getStatus().isRefused() && !state.isOutputSpent(address.getAddress())) {
						
						TxOutput output = state.getOutput(address.getAddress());
						
						//if there is pending claimers then ignore this input
						for(TxStatus status : output.getClaimers().values())
							if(status.isPending())
								continue statesFor;
						
						unspentInputs.add(state.getUid());
						inputsValue += output.getAmount();
					}				
				}catch(IllegalArgumentException e) {
					System.out.println(e.getMessage());
				}
					
			}
					
			//If we are trying to spend more than allowed throw an error
			if(outputsValue > inputsValue)
					throw new IllegalArgumentException("Trying to spend more than allowed ("+outputsValue+" / " + inputsValue +")");
			
			//If we are spending less than allowed create an output to get back whats remain
			else if(outputsValue < inputsValue)
				outputs.put(address.getAddress(), new TxOutput(address.getAddress(),inputsValue-outputsValue));
		}else unspentInputs.addAll(inputs);
		
		if(unspentInputs.size() == 0)
			throw new IllegalStateException("No input transaction found");
		
		if(parents.size() == 0) {
			//Get the tips we will attach the transaction to from peers
			GetTipsResponse getTipsResp = VirgoAPI.getInstance().getTips();
			if(getTipsResp.getResponseCode() != ResponseCode.OK)
				throw new IOException("Unable to get last tips from remote");
			
			parents.addAll(getTipsResp.getTips());
		}
		
		//Create the transaction JSON
		JSONObject transaction = new JSONObject();
		transaction.put("parents", new JSONArray(parents));
		transaction.put("inputs", new JSONArray(unspentInputs));
		
		JSONArray outputsJSON = new JSONArray();
		for(TxOutput output : outputs.values()) {
			outputsJSON.put(output.toString());
		}
		transaction.put("outputs", outputsJSON);
		
		long date = System.currentTimeMillis();
		
		transaction.put("date", date);
		
		transaction.put("pubKey", Converter.bytesToHex(address.getPublicKey(privateKey)));
		
		Sha256Hash txHash = Sha256.getDoubleHash((transaction.getJSONArray("parents").toString() +
				transaction.getJSONArray("inputs").toString() +
				outputsJSON.toString() + 
				date)
				.getBytes());
		
		//Sign transaction
		ECDSASignature sig = address.sign(txHash, privateKey);
		transaction.put("sig", sig.toHexString());
		
		boolean ok = false;
		//broadcast it and return raw transaction
		for(Provider provider : VirgoAPI.getInstance().getProvidersWatcher().getProvidersByScore()) {
			Response resp = provider.post("/tx", transaction.toString());
			if(!ok && resp.getResponseCode() == ResponseCode.OK)
				ok = true;
		}
		
		if(ok) return transaction;
		
		return null;
	}
	
}
