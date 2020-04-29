package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;
import java.util.HashMap;

import io.virgo.virgoAPI.data.TransactionState;
import net.holm.geoWeb.ResponseCode;

public class GetTxsStateResponse extends RequestResponse {

	private HashMap<String, TransactionState> states;
	
	public GetTxsStateResponse(ResponseCode code, HashMap<String, TransactionState> states) {
		super(RequestType.GET_TXS_STATE, code);
		this.states = states;
	}

	public ArrayList<TransactionState> getStates() {
		return new ArrayList<TransactionState>(states.values());
	}
	
	public TransactionState getState(String uid) {
		return states.get(uid);
	}
	
}
