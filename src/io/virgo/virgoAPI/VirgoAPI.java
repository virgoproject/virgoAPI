package io.virgo.virgoAPI;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.virgo.virgoAPI.crypto.TxOutput;
import io.virgo.virgoAPI.data.AddressBalance;
import io.virgo.virgoAPI.data.AddressTxs;
import io.virgo.virgoAPI.data.Transaction;
import io.virgo.virgoAPI.data.TransactionState;
import io.virgo.virgoAPI.data.TxStatus;
import io.virgo.virgoAPI.network.ProvidersWatcher;
import io.virgo.virgoAPI.network.Provider;
import io.virgo.virgoAPI.network.Response;
import io.virgo.virgoAPI.network.ResponseCode;
import io.virgo.virgoAPI.requestsResponses.GetAddressesTxsResponse;
import io.virgo.virgoAPI.requestsResponses.GetBalancesResponse;
import io.virgo.virgoAPI.requestsResponses.GetPoWInformationsResponse;
import io.virgo.virgoAPI.requestsResponses.GetTipsResponse;
import io.virgo.virgoAPI.requestsResponses.GetTransactionsResponse;
import io.virgo.virgoAPI.requestsResponses.GetTxsStateResponse;
import io.virgo.virgoCryptoLib.Converter;
import io.virgo.virgoCryptoLib.ECDSA;
import io.virgo.virgoCryptoLib.ECDSASignature;
import io.virgo.virgoCryptoLib.Sha256;
import io.virgo.virgoCryptoLib.Sha256Hash;
import io.virgo.virgoCryptoLib.Utils;

/**
 * Java library to interact with the Virgo network
 */
public class VirgoAPI {

	private static VirgoAPI instance;
	private ProvidersWatcher providersWatcher;
	
	public static final int DECIMALS = 8;
	public static final byte[] ADDR_IDENTIFIER = new BigInteger("4039").toByteArray();
	public static final byte[] TX_IDENTIFIER = new BigInteger("3823").toByteArray();
	
	/**
	 * Create a new virgoAPI instance from builder
	 */
	private VirgoAPI(Builder builder) throws IOException {
		instance = this;
		
		providersWatcher = new ProvidersWatcher();
	}
	
	/**
	 * Get tips transactions from peers<br>
	 * Will only return result from the most up-to-date peer
	 * 
	 * @return {@link GetTipsResponse} containing the request result
	 */
	public GetTipsResponse getTips() {
		Iterator<Provider> providers = getProvidersWatcher().getProvidersByScore().iterator();
		
		//Loop through all peers, starting from the one with highest score (probably most up-to-date)
		while(providers.hasNext()) {
			Provider provider = providers.next();
			
			//send request
			Response resp = provider.get("/tips");
			if(resp.getResponseCode() == ResponseCode.OK) {
				
				//if got a response check for data validity
				try {
					ArrayList<String> responseTips = new ArrayList<String>();
					JSONArray tipsJSON = new JSONArray(resp.getResponse());
					
					for(int i = 0; i < tipsJSON.length(); i++) {
						String tip = tipsJSON.getString(i);
						
						if(Utils.validateAddress(tip, TX_IDENTIFIER))
							responseTips.add(tip);
						else break;
						
					}
					
					//if everything is good return peer response, else goto next iteration
					if(!responseTips.isEmpty())
						return new GetTipsResponse(ResponseCode.OK, responseTips);
					
				}catch(JSONException e) { }
				
			}
			
		}
		
		//If nothing has been returned yet return 404 NOT FOUND error
		return new GetTipsResponse(ResponseCode.NOT_FOUND, null);
	}
	
	/**
	 * Get a raw transaction from its identifier
	 * 
	 * @param txId The ID of the wanted transaction
	 * @return {@link GetTransactionResponse} containing the request result
	 */
	public GetTransactionsResponse getTransactions(Collection<String> txsIds) {
		
		//remove duplicate entries from wanted transactions
	    txsIds = new ArrayList<String>(new HashSet<String>(txsIds));
		
		//First check if given ids are valid, if not return 400 BAD_REQUEST
		for(String txId : txsIds)
			if(!Utils.validateAddress(txId, VirgoAPI.TX_IDENTIFIER))
			return new GetTransactionsResponse(ResponseCode.BAD_REQUEST, new HashMap<String, Transaction>());
		
		Iterator<Provider> providers = getProvidersWatcher().getProvidersByScore().iterator();
		
		HashMap<String, Transaction> foundTransactions = new HashMap<String, Transaction>();
		
		//if one wanted transaction is genesis get it without consulting peers as it's hardcoded
		if(txsIds.contains("TXfxpq19sBUFgd8LRcUgjg1NdGK2ZGzBBdN")) {
			HashMap<String, TxOutput> genesisOutputs = new HashMap<String, TxOutput>();
			genesisOutputs.put("V2N5tYdd1Cm1xqxQDsY15x9ED8kyAUvjbWv", new TxOutput("V2N5tYdd1Cm1xqxQDsY15x9ED8kyAUvjbWv",(long) (100000 * Math.pow(10, DECIMALS))));
			
			foundTransactions.put("TXfxpq19sBUFgd8LRcUgjg1NdGK2ZGzBBdN", new Transaction("TXfxpq19sBUFgd8LRcUgjg1NdGK2ZGzBBdN",null,null,new String[0],new String[0], genesisOutputs, "", 0, 0));
			txsIds.remove("TXfxpq19sBUFgd8LRcUgjg1NdGK2ZGzBBdN");
		}
		
		
		//Loop through all peers, starting from the one with highest score (probably most up-to-date)
		while(providers.hasNext() && !foundTransactions.keySet().containsAll(txsIds)) {
			Provider provider = providers.next();
			
			for(String txId : txsIds) {
				if(foundTransactions.containsKey(txId))
					continue;
				
				Response resp = provider.get("/tx/"+txId);
				
				if(resp.getResponseCode().equals(ResponseCode.OK)) {
					
					try {
						JSONObject txJson = new JSONObject(resp.getResponse());
						
						String receivedTxUid;
						
						if(txJson.has("parentBeacon"))
							receivedTxUid = Converter.Addressify(Sha256.getDoubleHash((txJson.getJSONArray("parents").toString()
									+ txJson.getJSONArray("outputs").toString()
									+ txJson.getString("parentBeacon")
									+ txJson.getLong("date")
									+ txJson.getLong("nonce")).getBytes()).toBytes(), TX_IDENTIFIER);
						else
							receivedTxUid = Converter.Addressify(Converter.hexToBytes(txJson.getString("sig")), VirgoAPI.TX_IDENTIFIER);
						
						//check if given transaction is desired
						if(txsIds.contains(receivedTxUid) && !foundTransactions.containsKey(txId)) {

							ECDSASignature sig = null;
							byte[] pubKey = null;
							
							JSONArray parents = txJson.getJSONArray("parents");
							
							ArrayList<String> inputsArray = new ArrayList<String>();
							JSONArray inputs = new JSONArray();
							if(!txJson.has("parentBeacon"))
								inputs = txJson.getJSONArray("inputs");
							
							JSONArray outputs = txJson.getJSONArray("outputs");
							
							long date = txJson.getLong("date");
							
							String parentBeacon = "";
							long nonce = 0;
							
							ECDSA signer = new ECDSA();
							
							//check if signature is good
							if(!txJson.has("parentBeacon")) {
								 sig = ECDSASignature.fromByteArray(Converter.hexToBytes(txJson.getString("sig")));
								 pubKey = Converter.hexToBytes(txJson.getString("pubKey"));
								
								Sha256Hash TxHash = Sha256.getDoubleHash((parents.toString() + inputs.toString() + outputs.toString() + date).getBytes());
								if(!signer.Verify(TxHash, sig, pubKey))
									continue;
							
								//clean and verify inputs
								for(int i2 = 0; i2 < inputs.length(); i2++) {
									String inputTx = inputs.getString(i2);
									if(!Utils.validateAddress(inputTx, VirgoAPI.TX_IDENTIFIER))
										break;
									
									inputsArray.add(inputTx);
								}
							}else {
								parentBeacon = txJson.getString("parentBeacon");
								nonce = txJson.getLong("nonce");
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
								
								Transaction tx = new Transaction(receivedTxUid, sig, pubKey,
										parentsArray.toArray(new String[0]), inputsArray.toArray(new String[0]), outputsArray, parentBeacon, nonce, date);
																
								foundTransactions.put(receivedTxUid, tx);
								
							}
							
						}
					}catch(JSONException e) {}
					
				}
			}
		}
		
		if(foundTransactions.size() != 0)
			return new GetTransactionsResponse(ResponseCode.OK, foundTransactions);
		
		//If nothing has been returned yet return 404 NOT FOUND error
		return new GetTransactionsResponse(ResponseCode.NOT_FOUND, new HashMap<String, Transaction>());
		
	}
	
	
	//TODO: Send the request to all peers and concatenate data to get more complete responses
	/**
	 * Get all transactions relative to given addresses
	 * 
	 * @param addresses An array of the addresses to fetch
	 * @return {@link GetAddressesTxsResponse} Containing the transactions IDs corresponding to each addresses
	 */
	public GetAddressesTxsResponse getAddressesTxs(String[] addresses) {
		Iterator<Provider> providers = getProvidersWatcher().getProvidersByScore().iterator();
		
		//Check if every given address is valid, if not throw an illegalArgumentException
		for(String address : addresses) {
			if(!Utils.validateAddress(address, ADDR_IDENTIFIER))
				throw new IllegalArgumentException(address + " is not a valid address");
		}
		
		//valid transactions container
		HashMap<String, AddressTxs> addressesTxsMap = new HashMap<String, AddressTxs>();
		
		//Loop through all peers, starting from the one with highest score (probably most up-to-date)
		while(providers.hasNext()) {
			Provider provider = providers.next();
			
			for(String address : addresses) {
				if(addressesTxsMap.containsKey(address))
					continue;
				
				Response resp = provider.get("/address/"+address+"/txs/100");
				
				if(resp.getResponseCode() == ResponseCode.OK) {
					
					try {
						JSONArray transactionsJSON = new JSONArray(resp.getResponse());
						
						//verify txs
						ArrayList<String> transactions = new ArrayList<String>();
						for(int i = 0; i < transactionsJSON.length(); i++) {
							String tx = transactionsJSON.getString(i);
							if(!Utils.validateAddress(tx, VirgoAPI.TX_IDENTIFIER))
								break;
							
							transactions.add(tx);
						}
						
						if(transactions.size() == transactionsJSON.length())
							addressesTxsMap.put(address, new AddressTxs(address, transactions));
						else break;
							
					}catch(JSONException e) {}
					
				}
				
			}
			
		}
		
		if(addressesTxsMap.size() != 0)
			return new GetAddressesTxsResponse(ResponseCode.OK, addressesTxsMap);
		
		//If nothing has been returned yet return 404 NOT FOUND error
		return new GetAddressesTxsResponse(ResponseCode.NOT_FOUND, null);
	}
	
	
	/**
	 * Get given addresses balances (sent and received)
	 * Will only return result from the most up-to-date peer
	 * 
	 * @param addresses the addresses you want the balance of
	 * @return {@link GetBalancesResponse} Containing the balances of each addresses
	 */
	public GetBalancesResponse getBalances(String[] addresses) {
		Iterator<Provider> providers = getProvidersWatcher().getProvidersByScore().iterator();
		
		ArrayList<String> addrs = new ArrayList<String>();
		
		//Check if every given address is valid, if not throw an illegalArgumentException
		for(String address : addresses) {
			if(!Utils.validateAddress(address, ADDR_IDENTIFIER))
				throw new IllegalArgumentException(address + " is not a valid address");
			
			addrs.add(address);
		}
		
		//Loop through all peers, starting from the one with highest score (probably most up-to-date)
		while(providers.hasNext()) {
			Provider provider = providers.next();
			
			HashMap<String, AddressBalance> balancesMap = new HashMap<String, AddressBalance>();
			for(String address : addrs) {
				
				Response resp = provider.get("/address/"+address+"/balance");
				
				if(resp.getResponseCode() == ResponseCode.OK) {
					try {
						JSONObject balance = new JSONObject(resp.getResponse());
						
						if(addrs.contains(balance.getString("address")))
							balancesMap.put(balance.getString("address"), new AddressBalance(
									balance.getString("address"),
									balance.getLong("received"),
									balance.getLong("sent")) );
						
					}catch(JSONException e) {}
				}else break;
				
			}
			
			if(balancesMap.size() == addrs.size())
				return new GetBalancesResponse(ResponseCode.OK, balancesMap);
			
		}
		
		//If nothing has been returned yet return 404 NOT FOUND error
		return new GetBalancesResponse(ResponseCode.NOT_FOUND, null);
	}
	
	
	/**
	 * Get given transactions states (Status, stability, outputs states and values)
	 * 
	 * @param txsUids the IDs of the transactions you want the state of
	 * @return {@link GetTxsStateResponse} Containing the states of each transactions
	 */
	//TODO: Refactor this shit, states shouldn't be got from different sources? not found states must be marked as not found, not fake data
	public GetTxsStateResponse getTxsState(String[] txsUids) {
		Iterator<Provider> providers = getProvidersWatcher().getProvidersByScore().iterator();
		
		ArrayList<String> wantedTransactions = new ArrayList<String>();
		
		//check if every transaction id is valid
		for(String txUid : txsUids) {
			if(!Utils.validateAddress(txUid, TX_IDENTIFIER))
				throw new IllegalArgumentException(txUid + " is not a valid transaction identifier");
			wantedTransactions.add(txUid);
		}
		
		//Loop through all peers, starting from the one with highest score (probably most up-to-date)
		while(providers.hasNext()) {
			Provider provider = providers.next();
		
			HashMap<String, TransactionState> states = new HashMap<String, TransactionState>();

			for(String transaction : wantedTransactions) {
				
				Response resp = provider.get("/tx/"+transaction+"/state");
				
				if(resp.getResponseCode() == ResponseCode.OK) {
					
					try {
						
						JSONObject state = new JSONObject(resp.getResponse());
						if(state.has("status") && state.has("confirmations") && state.has("outputsState")) {
						
							HashMap<String, TxOutput> outputsStateMap = new HashMap<String, TxOutput>();
							JSONArray outputsState = state.getJSONArray("outputsState");
							for(int i = 0; i < outputsState.length(); i++) {
								JSONObject outputState = outputsState.getJSONObject(i);
								TxOutput output = new TxOutput(outputState.getString("address"), outputState.getLong("amount"), outputState.getBoolean("state"));
								outputsStateMap.put(output.getAddress(), output);
							}
							
							int confirmations = state.getInt("confirmations");
							
							String beacon = null;
							
							if(confirmations > 0) {
								beacon = state.getString("beacon");
								if(!Utils.validateAddress(beacon, TX_IDENTIFIER))
									break;
							}
							
							states.put(transaction, new TransactionState(transaction, TxStatus.fromCode(state.getInt("status")), beacon, confirmations, outputsStateMap, true));
						}else break;
						
					}catch(JSONException e) {}
					
				}
				
			}
			
			if(states.size() == wantedTransactions.size())
				return new GetTxsStateResponse(ResponseCode.OK, states);
		}
		
		return new GetTxsStateResponse(ResponseCode.NOT_FOUND, null);
	}
	
	public GetPoWInformationsResponse getPowInformations() {
		Iterator<Provider> providers = getProvidersWatcher().getProvidersByScore().iterator();
		
		while(providers.hasNext()) {
			Provider provider = providers.next();
		
			Response resp = provider.get("/work");
			if(resp.getResponseCode() == ResponseCode.OK) {
				try {
					
					JSONObject respJSON = new JSONObject(resp.getResponse());
					
					JSONArray parentsJSON = respJSON.getJSONArray("parentTxs");
					
					ArrayList<String> parents = new ArrayList<String>();
					for(int i = 0; i < parentsJSON.length(); i++)
						parents.add(parentsJSON.getString(i));
					
					GetPoWInformationsResponse informations = new GetPoWInformationsResponse(ResponseCode.OK,
							respJSON.getString("parentBeacon"),
							respJSON.getString("key"),
							respJSON.getLong("difficulty"),
							parents
							);
					
					return informations;
					
				}catch(JSONException e) { }
			}
		}
		
		return new GetPoWInformationsResponse(ResponseCode.NOT_FOUND, "", "", 0, null);
		
	}
	
	public static VirgoAPI getInstance() {
		return instance;
	}
	
	
	
	/**
	 * New virgoAPI instance builder
	 * 
	 * <p>
	 * Example:<br><br>
	 * {@code VirgoAPI api = new VirgoAPI.Builder()}<br>
	 * {@code .eventListener(new CustomEventListener())}<br>
	 * {@code .port(1234)}<br>
	 * {@code .build();}
	 * <p>
	 * 
	 */
	public static class Builder {
		
		public VirgoAPI build() throws IOException {
			
			return new VirgoAPI(this);
		}

		
	}

	public ProvidersWatcher getProvidersWatcher() {
		return providersWatcher;
	}
	
}