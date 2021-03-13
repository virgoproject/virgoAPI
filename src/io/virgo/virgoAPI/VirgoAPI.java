package io.virgo.virgoAPI;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.virgo.virgoAPI.crypto.TxOutput;
import io.virgo.virgoAPI.data.AddressBalance;
import io.virgo.virgoAPI.data.AddressTxs;
import io.virgo.virgoAPI.data.Transaction;
import io.virgo.virgoAPI.data.TransactionState;
import io.virgo.virgoAPI.data.TxStatus;
import io.virgo.virgoAPI.network.CustomEventListener;
import io.virgo.virgoAPI.network.EventListener;
import io.virgo.virgoAPI.network.MessageHandler;
import io.virgo.virgoAPI.network.PeersWatcher;
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
import io.virgo.geoWeb.GeoWeb;
import io.virgo.geoWeb.ResponseCode;
import io.virgo.geoWeb.Peer;
import io.virgo.geoWeb.SyncMessageResponse;
import io.virgo.geoWeb.exceptions.PortUnavailableException;

/**
 * Java library to interact with the Virgo network
 */
public class VirgoAPI {

	private static VirgoAPI instance;
	private GeoWeb geoWeb;
	private PeersWatcher peersWatcher;
	private CustomEventListener eventListener;
	
	public static final int DECIMALS = 8;
	public static final byte[] ADDR_IDENTIFIER = new BigInteger("4039").toByteArray();
	public static final byte[] TX_IDENTIFIER = new BigInteger("3823").toByteArray();
	
	/**
	 * Create a new virgoAPI instance from builder
	 */
	private VirgoAPI(Builder builder) throws IOException, PortUnavailableException {
		instance = this;
		
		GeoWeb.Builder geoWebBuilder = new GeoWeb.Builder();
		geoWeb = geoWebBuilder.netID(2946073207412533257l)
				.eventListener(new EventListener())
				.messageHandler(new MessageHandler())
				.port(builder.port)
				.build();
		
		peersWatcher = new PeersWatcher();
		eventListener = builder.eventListener;
	}
	
	/**
	 * Get tips transactions from peers<br>
	 * Will only return result from the most up-to-date peer
	 * 
	 * @return {@link GetTipsResponse} containing the request result
	 */
	public GetTipsResponse getTips() {
		Iterator<Peer> peers = getPeersWatcher().getPeersByScore().iterator();
		
		//prepare getTips message, same for all peers
		JSONObject getTipsRequest = new JSONObject();
		getTipsRequest.put("command", "getTips");
		
		//Loop through all peers, starting from the one with highest score (probably most up-to-date)
		while(peers.hasNext()) {
			Peer peer = peers.next();
			
			//send request
			SyncMessageResponse resp = peer.sendSyncMessage(getTipsRequest);
			if(resp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT) {
				
				//if got a response check for data validity
				try {
					ArrayList<String> responseTips = new ArrayList<String>();
					JSONArray tipsJSON = resp.getResponse().getJSONArray("tips");
					
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
	    txsIds = new ArrayList<String>(
	    	      new HashSet<String>(txsIds));
		
		//First check if given ids are valid, if not return 400 BAD_REQUEST
		for(String txId : txsIds)
			if(!Utils.validateAddress(txId, VirgoAPI.TX_IDENTIFIER))
			return new GetTransactionsResponse(ResponseCode.BAD_REQUEST, new HashMap<String, Transaction>());
		
		Iterator<Peer> peers = getPeersWatcher().getPeersByScore().iterator();
		
		//prepare askTxs message, same for everyone
		JSONObject askTxsRequest = new JSONObject();
		askTxsRequest.put("command", "askTxs");
		askTxsRequest.put("ids", new JSONArray(txsIds));
		
		HashMap<String, Transaction> foundTransactions = new HashMap<String, Transaction>();
		
		
		//if one wanted transaction is genesis get it without consulting peers as it's hardcoded
		if(txsIds.contains("TXfxpq19sBUFgd8LRcUgjg1NdGK2ZGzBBdN")) {
			HashMap<String, TxOutput> genesisOutputs = new HashMap<String, TxOutput>();
			genesisOutputs.put("V2N5tYdd1Cm1xqxQDsY15x9ED8kyAUvjbWv", new TxOutput("V2N5tYdd1Cm1xqxQDsY15x9ED8kyAUvjbWv",(long) (100000 * Math.pow(10, DECIMALS))));
			
			foundTransactions.put("TXfxpq19sBUFgd8LRcUgjg1NdGK2ZGzBBdN", new Transaction("TXfxpq19sBUFgd8LRcUgjg1NdGK2ZGzBBdN",null,null,new String[0],new String[0], genesisOutputs, "", 0, 0));
			txsIds.remove("TXfxpq19sBUFgd8LRcUgjg1NdGK2ZGzBBdN");
		}
		
		
		//Loop through all peers, starting from the one with highest score (probably most up-to-date)
		while(peers.hasNext() && !foundTransactions.keySet().containsAll(txsIds)) {
			Peer peer = peers.next();
			
			//ask peer if it has the wanted transaction
			SyncMessageResponse resp = peer.sendSyncMessage(askTxsRequest);
			
			if(resp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT) {
				
				//If got a response and peer indicate that it has the wanted transaction
				if(resp.getResponse().getString("command").equals("inv")) {
					
					ArrayList<String> desiredTxs = new ArrayList<String>();
					
					JSONArray invitedTxs = resp.getResponse().getJSONArray("ids");
					
					//Remove unwanted and already obtained txs from invivation
					for(int i = 0; i < invitedTxs.length(); i++) {
						String invitedTx = invitedTxs.getString(i);
						if(txsIds.contains(invitedTx) && !foundTransactions.containsKey(invitedTx) && !desiredTxs.contains(invitedTx))
							desiredTxs.add(invitedTx);
					}
					
					//ask peer to send transaction data
					JSONObject getTxsRequest = new JSONObject();
					getTxsRequest.put("command", "getTxs");
					getTxsRequest.put("ids", new JSONArray(desiredTxs));
					SyncMessageResponse getTxsResp = peer.sendSyncMessage(getTxsRequest);
					
					//If got a response check if given transaction is valid and the one we want
					if(getTxsResp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT) {
						
						JSONArray txs = getTxsResp.getResponse().getJSONArray("txs");
						
						for(int i = 0; i < txs.length(); i++) {
							JSONObject txJson = txs.getJSONObject(i);
							
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
							if(desiredTxs.contains(receivedTxUid)) {
	
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
									
									//remove tx from desired transactions as we got it
									desiredTxs.remove(receivedTxUid);
								}
								
							}
						}
					}
					
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
		Iterator<Peer> peers = getPeersWatcher().getPeersByScore().iterator();
		
		//Check if every given address is valid, if not throw an illegalArgumentException
		for(String address : addresses) {
			if(!Utils.validateAddress(address, ADDR_IDENTIFIER))
				throw new IllegalArgumentException(address + " is not a valid address");
		}
		
		//prepare getAddrTxs message as its the same for every peers
		JSONObject getTxsRequest = new JSONObject();
		getTxsRequest.put("command", "getAddrTxs");
		getTxsRequest.put("addresses", new JSONArray(addresses));
		
		ArrayList<String> addrs = new ArrayList<String>(Arrays.asList(addresses));
		
		//Loop through all peers, starting from the one with highest score (probably most up-to-date)
		while(peers.hasNext()) {
			Peer peer = peers.next();
			
			//send request
			SyncMessageResponse resp = peer.sendSyncMessage(getTxsRequest);
			if(resp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT) {
				
				try {
					JSONArray addressesTxs = resp.getResponse().getJSONArray("addrTxs");
					
					//valid transactions container
					HashMap<String, AddressTxs> addressesTxsMap = new HashMap<String, AddressTxs>();
					
					//for each address
					for(int i = 0; i < addressesTxs.length(); i++) {
						JSONObject addrTxs = addressesTxs.getJSONObject(i);
						
						//check if we requested this address
						if(addrs.contains(addrTxs.getString("address"))) {
							
							JSONArray inputs = addrTxs.getJSONArray("inputs");
							JSONArray outputs = addrTxs.getJSONArray("outputs");
							
							//verify input txs
							ArrayList<String> inputsArray = new ArrayList<String>();
							for(int i2 = 0; i2 < inputs.length(); i2++) {
								String inputTx = inputs.getString(i2);
								if(!Utils.validateAddress(inputTx, VirgoAPI.TX_IDENTIFIER))
									break;
								
								inputsArray.add(inputTx);
							}
							
							//verify output txs
							ArrayList<String> outputsArray = new ArrayList<String>();
							for(int i2 = 0; i2 < outputs.length(); i2++) {
								String outputTx = outputs.getString(i2);
								if(!Utils.validateAddress(outputTx, VirgoAPI.TX_IDENTIFIER))
									break;
								
								outputsArray.add(outputTx);
							}
							
							//if everything good add data to the valid transactions container
							if(inputs.length() == inputsArray.size() && outputs.length() == outputsArray.size()) {
								addressesTxsMap.put(addrTxs.getString("address"), new AddressTxs(
										addrTxs.getString("address"),
										inputsArray,
										outputsArray));	
							} else { break; }

						}
					}
					
					//If peer gave all addresses we wanted return everything 
					if(addressesTxsMap.size() == addrs.size())
						return new GetAddressesTxsResponse(ResponseCode.OK, addressesTxsMap);
					
				}catch(JSONException e) { }
				
			}
		
		}
		
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
		Iterator<Peer> peers = getPeersWatcher().getPeersByScore().iterator();
		
		//Check if every given address is valid, if not throw an illegalArgumentException
		for(String address : addresses) {
			if(!Utils.validateAddress(address, ADDR_IDENTIFIER))
				throw new IllegalArgumentException(address + " is not a valid address");
		}
		
		//prepare getBalances message as its the same for every peer
		JSONObject getBalancesRequest = new JSONObject();
		getBalancesRequest.put("command", "getBalances");
		getBalancesRequest.put("addresses", new JSONArray(addresses));
		
		ArrayList<String> addrs = new ArrayList<String>(Arrays.asList(addresses));
		
		//Loop through all peers, starting from the one with highest score (probably most up-to-date)
		while(peers.hasNext()) {
			Peer peer = peers.next();
			
			//send request
			SyncMessageResponse resp = peer.sendSyncMessage(getBalancesRequest);
			if(resp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT) {
				
				try {
					JSONArray balances = resp.getResponse().getJSONArray("balances");
					
					//valid balances container
					HashMap<String, AddressBalance> balancesMap = new HashMap<String, AddressBalance>();
					
					//check for validity of each received balance
					for(int i = 0; i < balances.length(); i++) {
						JSONObject balance = balances.getJSONObject(i);
						
						if(addrs.contains(balance.getString("address"))) {
							balancesMap.put(balance.getString("address"), new AddressBalance(
									balance.getString("address"),
									balance.getLong("received"),
									balance.getLong("sent")) );
						}
					}
					
					//if got all wanted balances return data, else goto next iteration
					if(balancesMap.size() == addrs.size())
						return new GetBalancesResponse(ResponseCode.OK, balancesMap);
					
				}catch(JSONException e) { }
				
			}
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
		Iterator<Peer> peers = getPeersWatcher().getPeersByScore().iterator();
		
		//check if every transaction id is valid
		for(String txUid : txsUids) {
			if(!Utils.validateAddress(txUid, TX_IDENTIFIER))
				throw new IllegalArgumentException(txUid + " is not a valid transaction identifier");
		}
		
		HashMap<String, TransactionState> states = new HashMap<String, TransactionState>();
		
		ArrayList<String> tbdStates = new ArrayList<String>(Arrays.asList(txsUids));
		
		while(peers.hasNext()) {
			Peer peer = peers.next();
			
			JSONObject getStateRequest = new JSONObject();
			getStateRequest.put("command", "getTxsState");
			getStateRequest.put("txs", new JSONArray(tbdStates));
			
			SyncMessageResponse resp = peer.sendSyncMessage(getStateRequest);
			
			if(resp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT) {
				
				try {
					JSONArray txsState = resp.getResponse().getJSONArray("txsState");
					
					for(int i = 0; i < txsState.length(); i++) {
						JSONObject state = txsState.getJSONObject(i);
						
						if(tbdStates.contains(state.getString("tx"))
								&& !state.has("notLoaded") && state.has("status") && state.has("confirmations") && state.has("outputsState")) {
							
							HashMap<String, TxOutput> outputsStateMap = new HashMap<String, TxOutput>();
							JSONArray outputsState = state.getJSONArray("outputsState");
							for(int i2 = 0; i2 < outputsState.length(); i2++) {
								JSONObject outputState = outputsState.getJSONObject(i2);
								TxOutput output = new TxOutput(outputState.getString("address"), outputState.getLong("amount"), outputState.getBoolean("state"));
								outputsStateMap.put(output.getAddress(), output);
							}
							
							states.put(state.getString("tx"), new TransactionState(
									state.getString("tx"),
									TxStatus.fromCode(state.getInt("status")),
									state.getInt("confirmations"), outputsStateMap, true));
							
							tbdStates.remove(state.getString("tx"));
							
						}
						
					}
					
					if(tbdStates.size() == 0) {
						break;
					}
					
				}catch(JSONException e) { }
				
			}
			
		}
		
		if(states.size() != 0) {
			
			for(String txUid : tbdStates) {
				states.put(txUid, new TransactionState(txUid, TxStatus.PENDING, 0, new HashMap<String, TxOutput>(), false));
			}
			
			return new GetTxsStateResponse(ResponseCode.OK, states);
		}
		
		return new GetTxsStateResponse(ResponseCode.NOT_FOUND, null);
	}
	
	public GetPoWInformationsResponse getPowInformations() {
		Iterator<Peer> peers = getPeersWatcher().getPeersByScore().iterator();
		
		JSONObject request = new JSONObject();
		request.put("command", "getPoWInformations");
		
		while(peers.hasNext()) {
			Peer peer = peers.next();
		
			SyncMessageResponse resp = peer.sendSyncMessage(request);
			if(resp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT) {
				try {
					
					JSONArray parentsJSON = resp.getResponse().getJSONArray("parentTxs");
					
					ArrayList<String> parents = new ArrayList<String>();
					for(int i = 0; i < parentsJSON.length(); i++)
						parents.add(parentsJSON.getString(i));
					
					GetPoWInformationsResponse informations = new GetPoWInformationsResponse(ResponseCode.OK,
							resp.getResponse().getString("parentBeacon"),
							resp.getResponse().getLong("difficulty"),
							parents
							);
					
					return informations;
					
				}catch(JSONException e) { }
			}
		}
		
		return new GetPoWInformationsResponse(ResponseCode.NOT_FOUND, "", 0, null);
		
	}
	
	public CustomEventListener getEventListener() {
		return eventListener;
	}
	
	public static VirgoAPI getInstance() {
		return instance;
	}
	
	
	
	/*--- geoWeb functions wrappers ---*/
	
	/**
	 * Try to connect to a new peer
	 * 
	 * @param hostname the IP or domain name of the machine to connect to
	 * @param port the port of the machine to connect to
	 * @return true if connected, false otherwise
	 */
	public boolean connectTo(String hostname, int port) {
		return geoWeb.connectTo(hostname, port);
				
	}
	
	/**
	 * Send a message to all connected peers
	 * 
	 * @param message The message to send
	 * <br>
	 * The Json object must contain a string parameter called 'command' (witch is the subject of your message)
	 * otherwise it will be ignored by peers
	 */
	public void broadCast(JSONObject txMessage) {
		geoWeb.broadCast(txMessage);
	}
	
	/**
	 * Send a message to all connected peers except given one
	 * 
	 * @param message The message to send
	 * @param peerToIgnore the peers to ignore
	 * 
	 * The Json object must contain a string parameter called 'command' (witch is the subject of your message)
	 * otherwise it will be ignored by peers
	 */
	public void broadCast(JSONObject message, List<Peer> peersToIgnore) {
		geoWeb.broadCast(message, peersToIgnore);
	}
	
	/**
	 * Send a message to target connected peers
	 * 
	 * @param message The message to send
	 * @param targetPeers the peers to send a message to
	 * 
	 * The Json object must contain a string parameter called 'command' (witch is the subject of your message)
	 * otherwise it will be ignored by peers
	 */
	public void broadCast(JSONObject message, Collection<Peer> targetPeers) {
		geoWeb.broadCast(message, targetPeers);
	}
	
	/**
	 * Disconnect peers and close all running threads
	 * virgoAPI will stop working after calling this function
	 */
	public void shutdown() {
		geoWeb.shutdown();
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
		
		private int port = 25565;
		private CustomEventListener eventListener;
		
		public VirgoAPI build() throws IOException, PortUnavailableException {
			
			if(eventListener == null)
				eventListener = new CustomEventListener();
			
			return new VirgoAPI(this);
		}
		
		public Builder port(int port) {
			this.port = port;
			
			return this;
		}
		
		public Builder eventListener(CustomEventListener eventListener) {
			this.eventListener = eventListener;
			
			return this;
		}
		
	}

	public PeersWatcher getPeersWatcher() {
		return peersWatcher;
	}
	
}