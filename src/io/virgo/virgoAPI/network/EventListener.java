package io.virgo.virgoAPI.network;

import io.virgo.virgoAPI.VirgoAPI;
import net.holm.geoWeb.events.PeerConnectionEvent;
import net.holm.geoWeb.events.PeerDisconnectionEvent;
import net.holm.geoWeb.events.PeerHandshakedEvent;

public class EventListener extends net.holm.geoWeb.events.EventListener {
	
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
		
		VirgoAPI.getInstance().getPeersWatcher().requestScoreUpdate(event.getPeer());
		
		VirgoAPI.getInstance().getEventListener().onPeerHandshaked(event);
	}
}
