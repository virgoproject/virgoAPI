package io.virgo.virgoAPI.requestsResponses;

import io.virgo.virgoAPI.data.Transaction;

import java.util.ArrayList;
import java.util.HashMap;

import io.virgo.geoWeb.ResponseCode;

/**
 * Object representing the response to a GetTransaction request
 */
public class GetTransactionsResponse extends RequestResponse {

	private HashMap<String, Transaction> transactions;
	
	public GetTransactionsResponse(ResponseCode responseCode, HashMap<String, Transaction> transactions) {
		super(RequestType.GET_TXS, responseCode);
		
		this.transactions = transactions;
	}
	
	public ArrayList<Transaction> getTransactions(){
		return new ArrayList<Transaction>(transactions.values());
	}
	
	/**
	 * @return The desired {@link Transaction} if found, otherwise null
	 * @param txId the id of the desired transaction
	 */
	public Transaction getTransaction(String txId) {
		return transactions.get(txId);
	}
	
}
