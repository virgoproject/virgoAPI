package io.virgo.virgoAPI.requestsResponses;

import java.util.ArrayList;
import java.util.HashMap;

import io.virgo.virgoAPI.data.TransactionState;
import io.virgo.virgoAPI.network.ResponseCode;
/**
 * Object representing the response to a GetTxsState request
 */
public class GetTxsStateResponse extends RequestResponse {

	private HashMap<String, TransactionState> states;
	
	public GetTxsStateResponse(ResponseCode code, HashMap<String, TransactionState> states) {
		super(RequestType.GET_TXS_STATE, code);
		this.states = states;
	}

	/**
	 * @return An ArrayList of the {@link TransactionState} for the target transactions
	 */
	public ArrayList<TransactionState> getStates() {
		return new ArrayList<TransactionState>(states.values());
	}
	
	/**
	 * Get the transaction state of the given transaction
	 * @param uid The ID of the transaction to get the state of
	 * @return The {@link TransactionState} of the given transaction or null if not found
	 */
	public TransactionState getState(String uid) {
		return states.get(uid);
	}
	
}
