package me.MaximumFX.ParkVisitServer;

import org.java_websocket.WebSocket;

import java.util.UUID;

public class Player {

	private String name;
	private String host;
	private UUID uuid;
	private WebSocket connection;
	private Party party;
	private float lat = 0f;
	private float lng = 0f;

	public Player(String name) {
		this.name = name;
		this.uuid = UUID.randomUUID();
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public UUID getUuid() {
		return uuid;
	}

	public void setHost(String host) {
		this.host = host;
	}
	public String getHost() {
		return host;
	}

	public void setConnection(WebSocket connection) {
		this.connection = connection;
	}
	public WebSocket getConnection() {
		return connection;
	}

	public void setParty(Party party) {
		this.party = party;
	}
	public Party getParty() {
		return party;
	}

	public void setLat(float lat) {
		this.lat = lat;
	}
	public float getLat() {
		return lat;
	}

	public void setLng(float lng) {
		this.lng = lng;
	}
	public float getLng() {
		return lng;
	}
}
