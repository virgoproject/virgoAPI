package io.virgo.virgoAPI.data;

import io.virgo.virgoAPI.crypto.Address;

/**
 * Object combining an Address together with a balance
 */
//TODO: Check what it was for, if has an utility better to make this implement Address rather than making a ref to already existing object ?
public class AddrAndBal {

	Address addr;
	long balance;
	
	public AddrAndBal(Address addr, long balance) {
		this.addr = addr;
		this.balance = balance;
	}
	
}
