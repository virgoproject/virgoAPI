package io.virgo.virgoAPI.data;

import java.util.ArrayList;

/**
 * An object resuming all input and output transactions of an address
 */
public class AddressTxs {

	private String address;
	private ArrayList<String> transactions;
	private int size;
	
	public AddressTxs(String address, ArrayList<String> transactions, int size) {
		this.address = address;
		this.transactions = transactions;
		this.size = size;
	}
	
	/**
	 * @return The address this object is about
	 */
	public String getAddress() {
		return address;
	}
	
	/**
	 * @return The address's transactions ids
	 */
	public String[] getTransactions() {
		return transactions.toArray(new String[transactions.size()]);
	}
	
	public int getTotalSize() {
		return size;
	}
	
}
