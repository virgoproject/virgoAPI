package io.virgo.virgoAPI.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import io.virgo.geoWeb.Peer;

/**
 * Util class to keep an up-to-date peer list with score
 */
public class PeersWatcher {

	private volatile HashMap<Peer, Integer> readyPeers = new HashMap<Peer, Integer>();
	
	public PeersWatcher() {
		
	}
	
	/**
	 * Set a peer's score
	 * @param peer The target peer
	 * @param score The wanted score
	 */
	public void setScore(Peer peer, int score) {
		readyPeers.put(peer, score);
	}
	
	/**
	 * Remove a peer from the ready peers list
	 * @param peer The peer to remove
	 */
	public void removePeer(Peer peer) {
		readyPeers.remove(peer);
	}
	
	/**
	 * Send a request to a peer to update it's score
	 * @param peer The target peer
	 */
	public void requestScoreUpdate(Peer peer) {
		JSONObject infosRequest = new JSONObject();
		infosRequest.put("command", "getNodeInfos");
		
		peer.sendMessage(infosRequest);
		System.out.println("sent request " + peer.getAddress());
	}
	
	/**
	 * @return A list of the peers, sorted by descending score
	 */
	public ArrayList<Peer> getPeersByScore() {
		List<Integer> values = new ArrayList<Integer>(readyPeers.values());
		Collections.sort(values);
		
		List<Peer> keys = new ArrayList<Peer>(readyPeers.keySet());
		
		Iterator<Integer> valuesIterator = values.iterator();
		
		ArrayList<Peer> sortedPeers = new ArrayList<Peer>();
		
		while(valuesIterator.hasNext()) {
			Iterator<Peer> keysIterator = keys.iterator();
			int score = valuesIterator.next();
			
			while(keysIterator.hasNext()) {
				Peer peer = keysIterator.next();
				
				if(score == readyPeers.get(peer)) {
					sortedPeers.add(peer);
					keys.remove(peer);
					break;
				}
				
			}
			
		}
		
		return sortedPeers;
	}
	
}
