package io.virgo.virgoAPI.network;

import io.virgo.geoWeb.events.PeerConnectionEvent;
import io.virgo.geoWeb.events.PeerDisconnectionEvent;
import io.virgo.geoWeb.events.PeerHandshakedEvent;

/**
 * Overrideable handler for VirgoAPI's events
 */
public class CustomEventListener {

	public void onPeerConnection(PeerConnectionEvent event) {
	}
	
	public void onPeerDisconnection(PeerDisconnectionEvent event) {
	}
	
	public void onPeerHandshaked(PeerHandshakedEvent event) {
	}
	
}
