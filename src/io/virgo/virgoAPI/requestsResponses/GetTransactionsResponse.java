package io.virgo.virgoAPI.requestsResponses;

import io.virgo.virgoAPI.data.Transaction;
import io.virgo.virgoAPI.network.ResponseCode;
import io.virgo.virgoCryptoLib.Sha256Hash;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Object representing the response to a GetTransaction request
 */
public class GetTransactionsResponse extends RequestResponse {

	private HashMap<Sha256Hash, Transaction> transactions;
	
	public GetTransactionsResponse(ResponseCode responseCode, HashMap<Sha256Hash, Transaction> transactions) {
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
	public Transaction getTransaction(Sha256Hash txHash) {
		return transactions.get(txHash);
	}
	
}
