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
import net.holm.geoWeb.Peer;
import net.holm.geoWeb.ResponseCode;
import net.holm.geoWeb.SyncMessageResponse;

public class TransactionBuilder {

	private Address address = null;
	private HashMap<String, TxOutput> outputs = new HashMap<String, TxOutput>();
	
	public TransactionBuilder() {
		
	}
	
	public TransactionBuilder address(Address address) {
		this.address = address;
		
		return this;
	}
	
	public TransactionBuilder output(TxOutput output) {
		outputs.put(output.getAddress(), output);
		
		return this;
	}
	
	public JSONObject send(byte[] privateKey) throws IOException {
		if(address == null)
			throw new IllegalStateException("no address defined");
		
		if(outputs.size() <= 0)
			throw new IllegalStateException("no output defined");
		
		if(!address.checkAgainstPrivateKey(privateKey))
			throw new IllegalArgumentException("Given private key doesn't correspond to given address");
		
		String[] addr = {address.getAddress()};
		GetAddressesTxsResponse addrTxsResp = VirgoAPI.getInstance().getAddressesTxs(addr);
		
		if(addrTxsResp.getResponseCode() != ResponseCode.OK)
			throw new IOException("Unable to get address transactions from remote");
		
		AddressTxs addrTxs = addrTxsResp.getAddressTxs(address.getAddress());
		
		GetTxsStateResponse txsStateResp = VirgoAPI.getInstance().getTxsState(addrTxs.getInputs());
		
		if(txsStateResp.getResponseCode() != ResponseCode.OK)
			throw new IOException("Unable to get transactions states from remote");
		
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
		
		long outputsValue = 0;
		for(TxOutput output : outputs.values())
			outputsValue += output.getAmount();
				
		if(inputsValue-outputsValue*VirgoAPI.FEES_RATE < outputsValue)
			throw new IllegalArgumentException("Trying to spend more than allowed ("+outputsValue+" / " + inputsValue +")");
		else if(inputsValue > outputsValue) {
			outputs.put(address.getAddress(), new TxOutput(address.getAddress(),inputsValue-outputsValue-(long)(outputsValue/200)));
			
		}
			
		
		GetTipsResponse getTipsResp = VirgoAPI.getInstance().getTips();
		if(getTipsResp.getResponseCode() != ResponseCode.OK)
			throw new IOException("Unable to get last tips from remote");
		
		JSONObject transaction = new JSONObject();
		transaction.put("parents", new JSONArray(getTipsResp.getTips()));
		transaction.put("inputs", new JSONArray(unspentInputs));
		
		JSONArray outputsJSON = new JSONArray();
		for(TxOutput output : outputs.values()) {
			outputsJSON.put(output.toString());
		}
		transaction.put("outputs", outputsJSON);
		
		transaction.put("date", System.currentTimeMillis());
		
		transaction.put("pubKey", Converter.bytesToHex(address.getPublicKey(privateKey)));
		
		Sha256Hash txHash = Sha256.getHash((transaction.getJSONArray("parents").toString() +
				transaction.getJSONArray("inputs").toString() +
				outputsJSON.toString())
				.getBytes());
		
		ECDSASignature sig = address.sign(txHash, privateKey);
		transaction.put("sig", sig.toHexString());
		
		JSONObject txMessage = new JSONObject();
		txMessage.put("command", "tx");
		txMessage.put("tx", transaction);
		txMessage.put("callback", true);
		
		Peer bestPeer = VirgoAPI.getInstance().getPeersWatcher().getPeersByScore().get(0);
		
		SyncMessageResponse txSubmissionResp = bestPeer.sendSyncMessage(txMessage);
		if(txSubmissionResp.getResponseCode() != ResponseCode.REQUEST_TIMEOUT && txSubmissionResp.getResponse().getBoolean("result") == true) {
			VirgoAPI.getInstance().broadCast(txMessage, Arrays.asList(new Peer[] {bestPeer}) );
			return transaction;
		}
		
		return null;
	}
	
}
