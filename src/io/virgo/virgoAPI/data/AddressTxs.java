package io.virgo.virgoAPI.data;

import java.util.ArrayList;

/**
 * An object resuming all input and output transactions of an address
 */
public class AddressTxs {

	private String address;
	private ArrayList<String> transactions;
	
	
	public AddressTxs(String address, ArrayList<String> transactions) {
		this.address = address;
		this.transactions = transactions;
	}
	
	/**
	 * @return The address this object is about
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * @return The address's input transactions ids
	 */
	public String[] getTransactions() {
		return transactions.toArray(new String[transactions.size()]);
	}
	
}
