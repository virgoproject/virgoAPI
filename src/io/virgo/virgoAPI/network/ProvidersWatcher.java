package io.virgo.virgoAPI.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Util class to keep an up-to-date peer list with score
 */
public class ProvidersWatcher {

	private volatile HashMap<Provider, Long> readyProviders = new HashMap<Provider, Long>();
	private volatile ArrayList<Provider> pendingProviders = new ArrayList<Provider>();
	
	public ProvidersWatcher() {
		
	}
	
	/**
	 * @return A list of ready providers, sorted by descending score
	 */
	public ArrayList<Provider> getProvidersByScore() {
		List<Long> values = new ArrayList<Long>(readyProviders.values());
		Collections.sort(values);
		
		List<Provider> keys = new ArrayList<Provider>(readyProviders.keySet());
		
		Iterator<Long> valuesIterator = values.iterator();
		
		ArrayList<Provider> sortedProviders = new ArrayList<Provider>();
		
		while(valuesIterator.hasNext()) {
			Iterator<Provider> keysIterator = keys.iterator();
			long score = valuesIterator.next();
			
			while(keysIterator.hasNext()) {
				Provider provider = keysIterator.next();
				
				if(score == readyProviders.get(provider)) {
					sortedProviders.add(provider);
					keys.remove(provider);
					break;
				}
				
			}
			
		}
		
		return sortedProviders;
	}
	
	public void makePendingProvidersReady() {
		
		for(Provider provider : new ArrayList<Provider>(pendingProviders)) {
			
			Response resp = provider.get("/nodeinfos");
			
			if(resp.getResponseCode() == ResponseCode.OK) {
				
				try{
					JSONObject state = new JSONObject(resp.getResponse());
					readyProviders.put(provider, state.getLong("DAGWeight"));
					pendingProviders.remove(provider);
				}catch(JSONException e) {}
				
			}
		}
	}
	
	public void updateScores() {
		for(Provider provider : readyProviders.keySet()) {
			
			Response resp = provider.get("/nodeinfos");
			
			if(resp.getResponseCode() == ResponseCode.OK) {
				
				try {
					
					JSONObject state = new JSONObject(resp.getResponse());
					readyProviders.replace(provider, state.getLong("DAGWeight"));
					
				}catch(JSONException e) {
					readyProviders.remove(provider);
					pendingProviders.add(provider);
				}
				
			}else {
				readyProviders.remove(provider);
				pendingProviders.add(provider);
			}
			
		}
	}
	
}
