package io.virgo.virgoAPI.network;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Util class to keep an up-to-date peer list with score
 */
public class ProvidersWatcher {

	private volatile HashMap<String, Provider> providersByHostname = new HashMap<String, Provider>();
	
	private volatile HashMap<Provider, Long> readyProviders = new HashMap<Provider, Long>();
	private volatile ArrayList<Provider> pendingProviders = new ArrayList<Provider>();
	
	private LinkedBlockingQueue<Provider> providersToCheck = new LinkedBlockingQueue<Provider>();
	
	private Thread pendingProvidersChecker;
	
	private Timer checkerTimer;
	
	public ProvidersWatcher(long checkRate) {
		
		/**
		 * Update providers scores every x seconds
		 * TODO: Make this parametrable
		 */
		checkerTimer = new Timer();
		checkerTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				
				for(Provider provider : pendingProviders)
					if(!providersToCheck.contains(provider))
						providersToCheck.add(provider);
				
				updateScores();
			}
			
		}, 0, 10000);
		
		//Start a thread that will check pending providers to add them to active providers list
		pendingProvidersChecker = new Thread(new Runnable() {

			@Override
			public void run() {
				while(!Thread.interrupted())
					try {
						Provider provider = providersToCheck.take();
						checkPendingProvider(provider);
					} catch (InterruptedException e) {
					}
			}
			
		});
		pendingProvidersChecker.start();
		
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
	
	/**
	 * Check a given provider validity by connecting to it and asking it's node informations
	 * @param provider the provider to check
	 */
	private void checkPendingProvider(Provider provider) {
		
		Response resp = provider.get("/nodeinfos");
		
		if(resp.getResponseCode() == ResponseCode.OK) {
			
			try{
				JSONObject state = new JSONObject(resp.getResponse());
				readyProviders.put(provider, state.getLong("BeaconChainWeight"));
				pendingProviders.remove(provider);
			}catch(JSONException e) {}
			
		}
	}
	
	/**
	 * Update ready providers scores by getting their node informations.
	 * Score corresponds to the weight of their DAG
	 */
	public void updateScores() {
		for(Provider provider : readyProviders.keySet()) {
			
			Response resp = provider.get("/nodeinfos");
			
			if(resp.getResponseCode() == ResponseCode.OK) {
				
				try {
					
					JSONObject state = new JSONObject(resp.getResponse());
					readyProviders.replace(provider, state.getLong("BeaconChainWeight"));
					
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
	
	/**
	 * Added a provider to watcher's list, it will then try to connect to it
	 * @param provider The provider object you want to add
	 * @return true if added, false if already in list
	 */
	public boolean addProvider(Provider provider) {
		if(providersByHostname.containsKey(provider.hostname))
			return false;
		
		pendingProviders.add(0, provider);
		providersByHostname.put(provider.hostname, provider);
		return true;
	}

	/**
	 * Remove the provider corresponding to the given hostname
	 * @param hostname The URL hostname of the provider to remove
	 */
	public void removeProvider(URL hostname) {
		String formatedHostname = hostname.getProtocol() + "://" + hostname.getHost();
		
		if(hostname.getPort() == -1)
			formatedHostname += ":"+hostname.getDefaultPort();
		else
			formatedHostname += ":"+hostname.getPort();
		
		removeProvider(formatedHostname);
	}
	
	/**
	 * Remove the given provider
	 * @param provider the provider to remove
	 */
	public void removeProvider(Provider provider) {
		removeProvider(provider.hostname);
	}
	
	/**
	 * Remove the provider corresponding to the given hostname
	 * @param hostname The hostname of the provider to remove
	 */
	public void removeProvider(String hostname) {
		Provider provider = providersByHostname.get(hostname);
		
		if(provider == null)
			return;
		providersByHostname.remove(hostname);
		
		readyProviders.remove(provider);
		pendingProviders.remove(provider);
		providersToCheck.remove(provider);
		
		System.out.println("removing " + hostname);
	}
	
	public void shutdown() {
		checkerTimer.cancel();
		pendingProvidersChecker.interrupt();
	}
	
	public ArrayList<String> getProvidersHostnames(){
		return new ArrayList<String>(providersByHostname.keySet());
	}
	
}
