package io.virgo.virgoAPI.requestsResponses;

import io.virgo.virgoAPI.data.Transaction;
import io.virgo.geoWeb.ResponseCode;

public class GetTransactionResponse extends RequestResponse {

	private Transaction transaction;
	
	public GetTransactionResponse(ResponseCode responseCode, Transaction transaction) {
		super(RequestType.GET_TX, responseCode);
		
		this.transaction = transaction;
	}
	
	public Transaction getTransaction() {
		return transaction;
	}
	
}
