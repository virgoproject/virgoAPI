package io.virgo.virgoAPI.network;

import org.json.JSONException;
import org.json.JSONObject;

import io.virgo.virgoAPI.BoltAPI;
import net.holm.geoWeb.Peer;

public class MessageHandler extends net.holm.geoWeb.MessageHandler {
	
	@Override
	public void onMessage(JSONObject messageJson, Peer peer) {
		System.out.println("ss"+messageJson.toString());
		try {
			
			switch(messageJson.getString("command")) {
			
			case "nodeInfos":
				System.out.println("nodeInfos received");
				System.out.println(messageJson.toString());
				BoltAPI.getInstance().getPeersWatcher().setScore(peer, messageJson.getInt("DAGHeight"));
				break;
			}
			
		}catch(JSONException e) {
			
		}
		
	}

}
