package io.virgo.virgoAPI;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
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
import io.virgo.virgoCryptoLib.Sha256;
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
		providersWatcher = new ProvidersWatcher(builder.checkRate);
		
		for(URL providerHostname : builder.providers)
			addProvider(providerHostname);
	}
	
	/**
	 * Add a REST API provider and try to connect to it
	 * @param hostname
	 * @return true if added, false otherwise
	 */
	public boolean addProvider(URL hostname) {
		String formatedHostname = hostname.getProtocol() + "://" + hostname.getHost();
		
		if(hostname.getPort() == -1)
			formatedHostname += ":"+hostname.getDefaultPort();
		else
			formatedHostname += ":"+hostname.getPort();
		
		Provider provider = new Provider(formatedHostname);
		return providersWatcher.addProvider(provider);
	}
	
	/**
	 * Remove a provider from list
	 * @param hostname the hostname of the REST API provider
	 */
	public void removeProvider(String hostname) {
		providersWatcher.removeProvider(hostname);
	}
	
	/**
	 * Get all list of all providers 
	 * @return
	 */
	public ArrayList<String> getProvidersHostnames(){
		return providersWatcher.getProvidersHostnames();
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

							Transaction tx = Transaction.fromJSONObject(txJson);
							if(tx != null)
								foundTransactions.put(receivedTxUid, tx);
							
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
	
	
	/**
	 * Get all transactions relative to given addresses
	 * 
	 * @param addresses An array of the addresses to fetch
	 * @return {@link GetAddressesTxsResponse} Containing the transactions IDs corresponding to each addresses
	 */
	public GetAddressesTxsResponse getAddressesTransactions(String[] addresses, int perPage, int page, String type) {
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
				
				Response resp = provider.get("/address/"+address+"/"+type+"/"+perPage+"/"+page);
				
				if(resp.getResponseCode() == ResponseCode.OK) {
					
					try {
						JSONObject respJSON = new JSONObject(resp.getResponse());
						
						JSONArray transactionsJSON = respJSON.getJSONArray(type);
						
						ArrayList<String> transactions = new ArrayList<String>();
						for(int i = 0; i < transactionsJSON.length(); i++) {
							String tx = transactionsJSON.getString(i);
							if(!Utils.validateAddress(tx, VirgoAPI.TX_IDENTIFIER))
								break;
							
							transactions.add(tx);
						}
						
						
						if(transactions.size() == transactionsJSON.length())
							addressesTxsMap.put(address, new AddressTxs(address, transactions, respJSON.getInt("size")));
						else break;
							
					}catch(JSONException e) {
						e.printStackTrace();
					}
					
				}
				
			}
			
		}
		
		if(addressesTxsMap.size() != 0)
			return new GetAddressesTxsResponse(ResponseCode.OK, addressesTxsMap);
		
		//If nothing has been returned yet return 404 NOT FOUND error
		return new GetAddressesTxsResponse(ResponseCode.NOT_FOUND, null);
	}
	
	public GetAddressesTxsResponse getAddressesOutputs(String[] addresses, int perPage, int page) {
		return getAddressesTransactions(addresses, perPage, page, "outputs");
	}
	
	public GetAddressesTxsResponse getAddressesOutputs(String[] addresses) {
		return getAddressesTransactions(addresses, 100, 1, "outputs");
	}
	
	public GetAddressesTxsResponse getAddressesInputs(String[] addresses, int perPage, int page) {
		return getAddressesTransactions(addresses, perPage, page, "inputs");
	}
	
	public GetAddressesTxsResponse getAddressesInputs(String[] addresses) {
		return getAddressesTransactions(addresses, 100, 1, "inputs");
	}
	
	public GetAddressesTxsResponse getAddressesTxs(String[] addresses, int perPage, int page) {
		return getAddressesTransactions(addresses, perPage, page, "txs");
	}
	
	public GetAddressesTxsResponse getAddressesTxs(String[] addresses) {
		return getAddressesTransactions(addresses, 100, 1, "txs");
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
	//TODO: Refactor, states shouldn't be got from different sources? not found states must be marked as not found, not fake data
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
								JSONArray claimersJSON = outputState.getJSONArray("claimers");
								
								HashMap<String, TxStatus> claimers = new HashMap<String, TxStatus>();
								for(int i2 = 0; i2 < claimersJSON.length(); i2++) {
									JSONObject claimer = claimersJSON.getJSONObject(i2);
									claimers.put(claimer.getString("id"), TxStatus.fromCode(claimer.getInt("status")));
								}
								
								TxOutput output = new TxOutput(outputState.getString("address"), outputState.getLong("amount"), outputState.getBoolean("spent"), claimers);
								outputsStateMap.put(output.getAddress(), output);
							}
							
							int confirmations = state.getInt("confirmations");
							
							String beacon = null;
							
							if(confirmations > 0) {
								beacon = state.getString("beacon");
								if(!Utils.validateAddress(beacon, TX_IDENTIFIER))
									break;
							}
							
							states.put(transaction, new TransactionState(transaction, TxStatus.fromCode(state.getInt("status")), beacon, confirmations, outputsStateMap));
						}else break;
						
					}catch(JSONException e) {}
					
				}
				
			}
			
			if(states.size() == wantedTransactions.size())
				return new GetTxsStateResponse(ResponseCode.OK, states);
		}
		
		return new GetTxsStateResponse(ResponseCode.NOT_FOUND, null);
	}
	
	/**
	 * Get all neccessary informations for proof of work mining
	 * @return {@link GetPoWinformations} Containing the informations (recommanded parent beacon, randomX key, difficulty and parent transactions
	 */
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
							new BigInteger(respJSON.getString("difficulty")),
							parents
							);
					
					return informations;
					
				}catch(JSONException e) { }
			}
		}
		
		return new GetPoWInformationsResponse(ResponseCode.NOT_FOUND, "", "", BigInteger.ONE, null);
		
	}
	
	/***
	 * Broadcast a transaction to the network
	 * @param transaction the transaction to broadcast in form of JSON Object
	 */
	public void broadcastTransaction(JSONObject transaction) {
		Iterator<Provider> providers = getProvidersWatcher().getProvidersByScore().iterator();
		
		while(providers.hasNext()) {
			Provider provider = providers.next();
			provider.post("/tx", transaction.toString());
		}
	}
	
	public static VirgoAPI getInstance() {
		return instance;
	}
	
	public void shutdown() {
		providersWatcher.shutdown();
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
		
		private long checkRate = 10000;
		
		private ArrayList<URL> providers = new ArrayList<URL>();
		
		public VirgoAPI build() throws IOException {
			return new VirgoAPI(this);
		}

		public Builder provider(URL hostname) {
			providers.add(hostname);
			return this;
		}
		
		public Builder providersUpdateRate(long rate) {
			checkRate = rate;
			return this;
		}
		
	}

	public ProvidersWatcher getProvidersWatcher() {
		return providersWatcher;
	}
	
}