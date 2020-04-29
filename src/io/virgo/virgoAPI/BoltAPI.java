package io.virgo.virgoAPI;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.virgo.virgoAPI.crypto.Address;
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
import io.virgo.virgoAPI.requestsResponses.GetTipsResponse;
import io.virgo.virgoAPI.requestsResponses.GetTransactionResponse;
import io.virgo.virgoAPI.requestsResponses.GetTxsStateResponse;
import io.virgo.virgoAPI.utils.Miscellaneous;
import net.boltLabs.boltCryptoLib.Converter;
import net.boltLabs.boltCryptoLib.ECDSA;
import net.boltLabs.boltCryptoLib.ECDSASignature;
import net.boltLabs.boltCryptoLib.Sha256;
import net.boltLabs.boltCryptoLib.Sha256Hash;
import net.holm.geoWeb.GeoWeb;
import net.holm.geoWeb.ResponseCode;
import net.holm.geoWeb.Peer;
import net.holm.geoWeb.SyncMessageResponse;
import net.holm.geoWeb.exceptions.PortUnavailableException;

public class BoltAPI {

	private static BoltAPI instance;
	private GeoWeb geoWeb;
	private PeersWatcher peersWatcher;
	private CustomEventListener eventListener;
	
	public static final int DECIMALS = 8;
	public static final int TOTALCOINS = 32032000;
	public static final long TOTALUNITS = (long) (TOTALCOINS * Math.pow(10, DECIMALS));
	public static final byte[] ADDR_IDENTIFIER = new BigInteger("19879").toByteArray();
	public static final byte[] TX_IDENTIFIER = new BigInteger("3823").toByteArray();
	public static final float FEES_RATE = 0.005f;
	
	private BoltAPI(Builder builder) throws IOException, PortUnavailableException {
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
	
	public boolean connectTo(String hostname, int port) {
		return geoWeb.connectTo(hostname, port);
				
	}
	
	public void broadCast(JSONObject txMessage) {
		geoWeb.broadCast(txMessage);
	}
	
	public void broadCast(JSONObject message, List<Peer> peersToIgnore) {
		geoWeb.broadCast(message, peersToIgnore);
	}
	
	public void broadCast(JSONObject message, Collection<Peer> targetPeers) {
		geoWeb.broadCast(message, targetPeers);
	}
	
	public GetTipsResponse getTips() {
		Iterator<Peer> peers = getPeersWatcher().getPeersByScore().iterator();
		
		JSONObject getTipsRequest = new JSONObject();
		getTipsRequest.put("command", "getTips");
		
		while(peers.hasNext()) {
			Peer peer = peers.next();
			
			SyncMessageResponse resp = peer.sendSyncMessage(getTipsRequest);
			if(resp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT) {
				
				try {
					ArrayList<String> responseTips = new ArrayList<String>();
					JSONArray tipsJSON = resp.getResponse().getJSONArray("tips");
					
					for(int i = 0; i < tipsJSON.length(); i++) {
						String tip = tipsJSON.getString(i);
						
						if(Miscellaneous.validateAddress(tip, TX_IDENTIFIER))
							responseTips.add(tip);
						else break;
						
					}
					
					if(!responseTips.isEmpty())
						return new GetTipsResponse(ResponseCode.OK, responseTips);
					
				}catch(JSONException e) { }
				
			}
			
		}
				
		return new GetTipsResponse(ResponseCode.NOT_FOUND, null);
	}
	
	public GetTransactionResponse getTransaction(String txId) {
		
		if(!Miscellaneous.validateAddress(txId, BoltAPI.TX_IDENTIFIER))
			return new GetTransactionResponse(ResponseCode.BAD_REQUEST, null);
		
		if(txId.equals("TXfxpq19sBUFgd8LRcUgjg1NdGK2ZGzBBdN")) {//if genesis
			HashMap<String, TxOutput> genesisOutputs = new HashMap<String, TxOutput>();
			genesisOutputs.put("BoLT9o1RJKSSaTq1AWE4sQ3SbWFVkUvRuVEQq", new TxOutput("BoLT9o1RJKSSaTq1AWE4sQ3SbWFVkUvRuVEQq",BoltAPI.TOTALUNITS));
			
			return new GetTransactionResponse(ResponseCode.OK,
					new Transaction("TXfxpq19sBUFgd8LRcUgjg1NdGK2ZGzBBdN",null,null,null,null, genesisOutputs, 0));
		}
		
		Iterator<Peer> peers = getPeersWatcher().getPeersByScore().iterator();
		
		JSONObject askTxRequest = new JSONObject();
		askTxRequest.put("command", "askTx");
		askTxRequest.put("id", txId);
		
		while(peers.hasNext()) {
			Peer peer = peers.next();
			
			SyncMessageResponse resp = peer.sendSyncMessage(askTxRequest);
			if(resp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT) {
				
				if(resp.getResponse().getString("command").equals("inv")
						&& resp.getResponse().getString("id").equals(txId)) {
					
					JSONObject getTxRequest = new JSONObject();
					getTxRequest.put("command", "getTx");
					getTxRequest.put("id", txId);
					
					SyncMessageResponse getTxResp = peer.sendSyncMessage(getTxRequest);
					
					if(getTxResp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT) {
						
						JSONObject txJson = getTxResp.getResponse().getJSONObject("tx");
						String receivedTxUid = Converter.Addressify(Converter.hexToBytes(txJson.getString("sig")), BoltAPI.TX_IDENTIFIER);

						if(receivedTxUid.equals(txId)) {

							ECDSASignature sig = ECDSASignature.fromByteArray(Converter.hexToBytes(txJson.getString("sig")));
							byte[] pubKey = Converter.hexToBytes(txJson.getString("pubKey"));
							
							JSONArray parents = txJson.getJSONArray("parents");
							JSONArray inputs = txJson.getJSONArray("inputs");
							JSONArray outputs = txJson.getJSONArray("outputs");
							
							ECDSA signer = new ECDSA();
							
							//check if signature is good
							Sha256Hash TxHash = Sha256.getHash((parents.toString() + inputs.toString() + outputs.toString()).getBytes());
							if(!signer.Verify(TxHash, sig, pubKey))
								break;

							ArrayList<String> inputsArray = new ArrayList<String>();
							for(int i = 0; i < inputs.length(); i++) {
								String inputTx = inputs.getString(i);
								if(!Miscellaneous.validateAddress(inputTx, BoltAPI.TX_IDENTIFIER))
									break;
								
								inputsArray.add(inputTx);
							}

							ArrayList<String> parentsArray = new ArrayList<String>();
							for(int i = 0; i < parents.length(); i++) {
								String parentTx = parents.getString(i);
								if(!Miscellaneous.validateAddress(parentTx, BoltAPI.TX_IDENTIFIER))
									break;
								
								parentsArray.add(parentTx);
							}							

							HashMap<String, TxOutput> outputsArray = new HashMap<String, TxOutput>();
							for(int i = 0; i < outputs.length(); i++) {
								String outputString = outputs.getString(i);
								try {
									TxOutput output = TxOutput.fromString(outputString);
									outputsArray.put(output.getAddress(), output);
								}catch(IllegalArgumentException e) {
									break;
								}
							}							

							if(inputsArray.size() == inputs.length() && parentsArray.size() == parents.length()
									&& outputsArray.size() == outputs.length()) {
								
								Transaction tx = new Transaction(receivedTxUid, sig, pubKey,
										parentsArray.toArray(new String[0]), inputsArray.toArray(new String[0]), outputsArray, System.currentTimeMillis());
																
								return new GetTransactionResponse(ResponseCode.OK, tx);
								
							}
							
						}
						
					}
					
				}
				
			}
		}
		
		return new GetTransactionResponse(ResponseCode.NOT_FOUND, null);
		
	}
	
	public GetAddressesTxsResponse getAddressesTxs(String[] addresses) {
		Iterator<Peer> peers = getPeersWatcher().getPeersByScore().iterator();
		
		for(String address : addresses) {
			if(!Miscellaneous.validateAddress(address, ADDR_IDENTIFIER))
				throw new IllegalArgumentException(address + " is not a valid address");
		}
		
		JSONObject getTxsRequest = new JSONObject();
		getTxsRequest.put("command", "getAddrTxs");
		getTxsRequest.put("addresses", new JSONArray(addresses));
		
		ArrayList<String> addrs = new ArrayList<String>(Arrays.asList(addresses));
		
		while(peers.hasNext()) {
			Peer peer = peers.next();
			
			SyncMessageResponse resp = peer.sendSyncMessage(getTxsRequest);
			
			if(resp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT) {
				
				try {
					JSONArray addressesTxs = resp.getResponse().getJSONArray("addrTxs");
					
					HashMap<String, AddressTxs> addressesTxsMap = new HashMap<String, AddressTxs>();
					
					for(int i = 0; i < addressesTxs.length(); i++) {
						JSONObject addrTxs = addressesTxs.getJSONObject(i);
						
						if(addrs.contains(addrTxs.getString("address"))) {
							
							JSONArray inputs = addrTxs.getJSONArray("inputs");
							JSONArray outputs = addrTxs.getJSONArray("outputs");
							
							ArrayList<String> inputsArray = new ArrayList<String>();
							for(int i2 = 0; i2 < inputs.length(); i2++) {
								String inputTx = inputs.getString(i2);
								if(!Miscellaneous.validateAddress(inputTx, BoltAPI.TX_IDENTIFIER))
									break;
								
								inputsArray.add(inputTx);
							}
							
							ArrayList<String> outputsArray = new ArrayList<String>();
							for(int i2 = 0; i2 < outputs.length(); i2++) {
								String outputTx = outputs.getString(i2);
								if(!Miscellaneous.validateAddress(outputTx, BoltAPI.TX_IDENTIFIER))
									break;
								
								outputsArray.add(outputTx);
							}
							
							if(inputs.length() == inputsArray.size() && outputs.length() == outputsArray.size()) {
								addressesTxsMap.put(addrTxs.getString("address"), new AddressTxs(
										addrTxs.getString("address"),
										inputsArray,
										outputsArray));	
							} else { break; }

						}
					}
					
					if(addressesTxsMap.size() == addrs.size())
						return new GetAddressesTxsResponse(ResponseCode.OK, addressesTxsMap);
					
				}catch(JSONException e) { }
				
			}
		
		}
		
		return new GetAddressesTxsResponse(ResponseCode.NOT_FOUND, null);
	}
	
	public GetBalancesResponse getBalances(String[] addresses) {
		Iterator<Peer> peers = getPeersWatcher().getPeersByScore().iterator();
		
		for(String address : addresses) {
			if(!Miscellaneous.validateAddress(address, ADDR_IDENTIFIER))
				throw new IllegalArgumentException(address + " is not a valid address");
		}
		
		JSONObject getBalancesRequest = new JSONObject();
		getBalancesRequest.put("command", "getBalances");
		getBalancesRequest.put("addresses", new JSONArray(addresses));
		
		ArrayList<String> addrs = new ArrayList<String>(Arrays.asList(addresses));
		
		while(peers.hasNext()) {
			Peer peer = peers.next();
			
			SyncMessageResponse resp = peer.sendSyncMessage(getBalancesRequest);
			
			if(resp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT) {
				
				try {
					JSONArray balances = resp.getResponse().getJSONArray("balances");
					
					HashMap<String, AddressBalance> balancesMap = new HashMap<String, AddressBalance>();
					
					for(int i = 0; i < balances.length(); i++) {
						JSONObject balance = balances.getJSONObject(i);
						
						if(addrs.contains(balance.getString("address"))) {
							balancesMap.put(balance.getString("address"), new AddressBalance(
									balance.getString("address"),
									balance.getLong("received"),
									balance.getLong("sent")) );
						}
					}
					
					if(balancesMap.size() == addrs.size())
						return new GetBalancesResponse(ResponseCode.OK, balancesMap);
					
				}catch(JSONException e) { }
				
			}
		}
		
		return new GetBalancesResponse(ResponseCode.NOT_FOUND, null);
	}
	
	public GetTxsStateResponse getTxsState(String[] txsUids) {
		Iterator<Peer> peers = getPeersWatcher().getPeersByScore().iterator();
		
		for(String txUid : txsUids) {
			if(!Miscellaneous.validateAddress(txUid, TX_IDENTIFIER))
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
								&& !state.has("notLoaded") && state.has("status") && state.has("stability") && state.has("outputsState")) {
							
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
									state.getInt("stability"), outputsStateMap, true));
							
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
	
	public static Address generateNewAddress() {
		byte[] pKey = ECDSA.generatePrivateKey();
		
		return new Address(pKey);
	}
	
	public CustomEventListener getEventListener() {
		return eventListener;
	}
	
	public static BoltAPI getInstance() {
		return instance;
	}
	
	public static class Builder {
		
		private int port = 25565;
		private CustomEventListener eventListener;
		
		public BoltAPI build() throws IOException, PortUnavailableException {
			
			if(eventListener == null)
				eventListener = new CustomEventListener();
			
			return new BoltAPI(this);
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