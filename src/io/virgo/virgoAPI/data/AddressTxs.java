package io.virgo.virgoAPI.data;

import java.util.ArrayList;

public class AddressTxs {

	private String address;
	private ArrayList<String> inputs;
	private ArrayList<String> outputs;
	
	public AddressTxs(String address, ArrayList<String> inputs, ArrayList<String> outputs) {
		this.address = address;
		this.inputs = inputs;
		this.outputs = outputs;
	}
	
	public String getAddress() {
		return address;
	}
	
	public String[] getInputs() {
		return inputs.toArray(new String[inputs.size()]);
	}
	
	public String[] getOutputs() {
		return outputs.toArray(new String[outputs.size()]);
	}
	
}
