package me.MaximumFX.ParkVisitServer;

import java.util.ArrayList;
import java.util.List;

public class Party {

	private String id;
	private String park;
	private List<Player> players = new ArrayList<>();

	Party(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setPark(String park) {
		this.park = park;
	}
	public String getPark() {
		return park;
	}

	public void join(Player p) {
		players.add(p);
	}
	public void leave(Player p) {
		players.remove(p);
	}
	public List<Player> getPlayers() {
		return players;
	}
}
