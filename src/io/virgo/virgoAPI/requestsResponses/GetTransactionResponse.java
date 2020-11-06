package io.virgo.virgoAPI.requestsResponses;

import io.virgo.virgoAPI.data.Transaction;
import io.virgo.geoWeb.ResponseCode;

/**
 * Object representing the response to a GetTransaction request
 */
public class GetTransactionResponse extends RequestResponse {

	private Transaction transaction;
	
	public GetTransactionResponse(ResponseCode responseCode, Transaction transaction) {
		super(RequestType.GET_TX, responseCode);
		
		this.transaction = transaction;
	}
	
	/**
	 * @return The desired {@link Transaction} if found, otherwise null
	 */
	public Transaction getTransaction() {
		return transaction;
	}
	
}
