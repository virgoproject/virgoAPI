package io.virgo.virgoAPI.network;

import org.json.JSONException;
import org.json.JSONObject;

import io.virgo.virgoAPI.VirgoAPI;
import io.virgo.geoWeb.Peer;

public class MessageHandler extends io.virgo.geoWeb.MessageHandler {
	
	@Override
	public void onMessage(JSONObject messageJson, Peer peer) {
		try {
			
			switch(messageJson.getString("command")) {
			
			case "nodeInfos":
				System.out.println("nodeInfos received");
				System.out.println(messageJson.toString());
				VirgoAPI.getInstance().getPeersWatcher().setScore(peer, messageJson.getInt("DAGHeight"));
				break;
			}
			
		}catch(JSONException e) {
			
		}
		
	}

}
