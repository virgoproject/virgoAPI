package io.virgo.virgoAPI.network;

import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.geoWeb.events.PeerConnectionEvent;
import io.virgo.geoWeb.events.PeerDisconnectionEvent;
import io.virgo.geoWeb.events.PeerHandshakedEvent;

/**
 * Custom handler for GeoWeb events, you can use theses events
 * by extending {@link CustomEventListener} and override the events you want
 */
public class EventListener extends io.virgo.geoWeb.events.EventListener {
	
	@Override
	public void onPeerConnection(PeerConnectionEvent event) {
		System.out.println(event.getPeer().getAddress() + " connected");
		
		VirgoAPI.getInstance().getEventListener().onPeerConnection(event);
	}
	
	@Override
	public void onPeerDisconnection(PeerDisconnectionEvent event) {
		VirgoAPI.getInstance().getPeersWatcher().removePeer(event.getPeer());
		System.out.println(event.getPeer().getAddress() + " disconnected");
		
		VirgoAPI.getInstance().getEventListener().onPeerDisconnection(event);
	}
	
	@Override
	public void onPeerHandshaked(PeerHandshakedEvent event) {
		System.out.println("handshaked");
		
		//update peer score based on the informations it provided
		VirgoAPI.getInstance().getPeersWatcher().requestScoreUpdate(event.getPeer());
		
		VirgoAPI.getInstance().getEventListener().onPeerHandshaked(event);
	}
}
