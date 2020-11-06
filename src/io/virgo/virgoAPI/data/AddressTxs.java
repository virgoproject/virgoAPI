package io.virgo.virgoAPI.data;

import java.util.ArrayList;

/**
 * An object resuming all input and output transactions of an address
 */
public class AddressTxs {

	private String address;
	private ArrayList<String> inputs;
	private ArrayList<String> outputs;
	
	
	public AddressTxs(String address, ArrayList<String> inputs, ArrayList<String> outputs) {
		this.address = address;
		this.inputs = inputs;
		this.outputs = outputs;
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
	public String[] getInputs() {
		return inputs.toArray(new String[inputs.size()]);
	}
	
	/**
	 * @return The address's output transactions ids
	 */
	public String[] getOutputs() {
		return outputs.toArray(new String[outputs.size()]);
	}
	
}
