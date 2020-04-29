package io.virgo.virgoAPI.network;

import io.virgo.virgoAPI.BoltAPI;
import net.holm.geoWeb.events.PeerConnectionEvent;
import net.holm.geoWeb.events.PeerDisconnectionEvent;
import net.holm.geoWeb.events.PeerHandshakedEvent;

public class EventListener extends net.holm.geoWeb.events.EventListener {
	
	@Override
	public void onPeerConnection(PeerConnectionEvent event) {
		System.out.println(event.getPeer().getAddress() + " connected");
		
		BoltAPI.getInstance().getEventListener().onPeerConnection(event);
	}
	
	@Override
	public void onPeerDisconnection(PeerDisconnectionEvent event) {
		BoltAPI.getInstance().getPeersWatcher().removePeer(event.getPeer());
		System.out.println(event.getPeer().getAddress() + " disconnected");
		
		BoltAPI.getInstance().getEventListener().onPeerDisconnection(event);
	}
	
	@Override
	public void onPeerHandshaked(PeerHandshakedEvent event) {
		System.out.println("handshaked");
		
		BoltAPI.getInstance().getPeersWatcher().requestScoreUpdate(event.getPeer());
		
		BoltAPI.getInstance().getEventListener().onPeerHandshaked(event);
	}
}
