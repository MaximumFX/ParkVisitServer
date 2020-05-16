package me.MaximumFX.ParkVisitServer;

import me.MaximumFX.ParkVisitServer.api.Helper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.*;

public class WebsocketServer extends WebSocketServer {

	private static WebsocketServer wss;
	private List<Player> players = new ArrayList<>();
	private List<Party> parties = new ArrayList<>();

	WebsocketServer() {}

	private WebsocketServer(int port) {
		super(new InetSocketAddress(port));
	}

//		conn.send(""); This method sends a message to the client
//		broadcast(""); This method sends a message to all clients connected

	void enable() {
		wss = new WebsocketServer(18190);
		wss.setReuseAddr(true);
		wss.start();
	}
//	static void disable() throws IOException, InterruptedException {
//		wss.stop();
//	}

	@Override
	public void onStart() {
		System.out.println("ParkVisitServer started on port: " + wss.getPort());
		setConnectionLostTimeout(0);
		setConnectionLostTimeout(100);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		read(conn, "ok", "message", "\"Welcome to the server!\"");
		System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered ParkVisit!");
	}
	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		if (conn != null) {
			System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " has left ParkVisit!");
			List<Player> toRemove = new ArrayList<>();
			List<Party> toRemoveParties = new ArrayList<>();
			for (Player p : players) {
				if (p.getConnection() == conn) {
					System.out.println(p.getName() + " has left ParkVisit!");
					toRemove.add(p);
					if (p.getParty() != null) {
						Party party = p.getParty();
						System.out.println(p.getName() + " was in party " + party.getId());
						party.leave(p);
						if (party.getPlayers().size() == 0) {
							System.out.println("Removing empty party " + party.getId() + ".");
							toRemoveParties.add(party);
						}
						else for (Player player : party.getPlayers()) {
							if (!player.getUuid().equals(p.getUuid())) {
								action(player.getConnection(), "ok", "left_party", "{\"uuid\":\"" + p.getUuid().toString() + "\"}");
							}
						}
					}
				}
			}
			players.removeAll(toRemove);
			parties.removeAll(toRemoveParties);
		}
		else System.out.println("Connection closed, connection was null. Code: " + code + " Reason: " + reason + " Remote: " + remote);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
//		System.out.println("Message: " + message);
		Map<String, List<String>> query = new HashMap<>();
		try {
			query = Helper.splitQuery(conn.getResourceDescriptor().substring(1));
		} catch (UnsupportedEncodingException ignored) {}
		UUID uuid = UUID.fromString(query.get("uuid").get(0));
		String host = conn.getRemoteSocketAddress().getAddress().getHostAddress();
		JSONParser parser = new JSONParser();
		JSONObject json;
		try {
			json = (JSONObject) parser.parse(message);
			String event = json.get("event").toString();
			if (event.equalsIgnoreCase("connecting")) {
				if (json.get("data") != null) {
					if (!hasConnection(uuid, host)) {
						Player p = new Player(json.get("data").toString());
						p.setUuid(uuid);
						p.setHost(host);
						p.setConnection(conn);
						players.add(p);
						read(conn, "ok", "connecting", "\"connected\"");
						System.out.println("Linked \"" + host + "\" to player \"" + p.getName() + "\"");//ConsoleColor.GREEN ++ ConsoleColor.RESET
					}
					else {
						error(conn, "\"" + host + "\" is already connected!", message);
						read(conn, "error", "connecting", "\"is_connected\"");
						conn.close();
					}
				}
				else {
					error(conn, host + " didn't provide a name!", message);
					read(conn, "error", "connecting", "\"no_name\"");
					conn.close();
				}
			}
			else if (event.equalsIgnoreCase("create_party")) {
				if (json.get("eventType").toString().equalsIgnoreCase("action")) {
					Party party = new Party(getRandom());
					if (json.get("data") != null) party.setPark(json.get("data").toString());
					parties.add(party);
					read(conn, "ok", "create_party", "\"" + party.getId() + "\"");
					System.out.println("Created party " + party.getId() + ".");
				}
				else error(conn, "Invalid action!", message);
			}
			else if (event.equalsIgnoreCase("remove_party")) {
				if (json.get("eventType").toString().equalsIgnoreCase("action")) {
					if (json.get("data") != null) {
						Party party = getParty(json.get("data").toString());
						if (party != null) {
							parties.remove(party);
							for (Player p: party.getPlayers()) p.setParty(null);
							read(conn, "ok", "remove_party", "\"removed_party\"");
							System.out.println("Removed party " + party.getId() + ".");
						}
						else error(conn, "Can't find party!", message);
					}
					else error(conn, "No data provided!", message);
				}
				else error(conn, "Invalid action!", message);
			}
			else if (event.equalsIgnoreCase("set_park")) {
				if (json.get("eventType").toString().equalsIgnoreCase("action")) {
					if (json.get("data") != null) {
						JSONObject data = (JSONObject) json.get("data");
						String partyId = data.get("party").toString();
						String park = data.get("park").toString();
						Party party = getParty(partyId);
						if (party != null) {
							party.setPark(park);
							read(conn, "ok", "set_park", "\"" + park + "\"");
							System.out.println("Set party " + party.getId() + "'s park to " + park + ".");
						}
						else error(conn, "Can't find party!", message);
					}
					else error(conn, "No data provided!", message);
				}
				else error(conn, "Invalid action!", message);
			}
			else if (event.equalsIgnoreCase("get_park")) {
				if (json.get("eventType").toString().equalsIgnoreCase("read")) {
					if (json.get("data") != null) {
						Party party = getParty(json.get("data").toString());
						if (party != null) {
							read(conn, "ok", "get_park", "\"" + party.getPark() + "\"");
							System.out.println("Send party " + party.getId() + "'s park.");
						}
						else error(conn, "Can't find party!", message);
					}
					else error(conn, "No data provided!", message);
				}
				else error(conn, "Invalid action!", message);
			}
			else if (event.equalsIgnoreCase("join_party")) {
				if (json.get("eventType").toString().equalsIgnoreCase("action")) {
					if (json.get("data") != null) {
						Player p = getPlayer(uuid, host);
						if (p != null && p.getConnection() != null) {
							JSONObject data = (JSONObject) json.get("data");
							String partyId = data.get("party").toString();
							String lat = data.get("lat").toString();
							String lng = data.get("lng").toString();
							String heading = data.get("heading").toString();
							Party party = getParty(partyId);
							if (party != null) {
								if (p.getParty() == null) {
									party.join(p);
									p.setParty(party);
									p.setLat(Helper.toFloat(lat));
									p.setLng(Helper.toFloat(lng));
									for (Player player: party.getPlayers()) {
										if (!player.getUuid().equals(p.getUuid())) {
											action(player.getConnection(), "ok", "joined_party", "{\"uuid\":\"" + p.getUuid().toString() + "\",\"name\":\"" + p.getName() + "\",\"lat\":" + lat + ",\"lng\":" + lng + ",\"heading\":" + heading + "}");
											action(p.getConnection(), "ok", "joined_party", "{\"uuid\":\"" + player.getUuid().toString() + "\",\"name\":\"" + player.getName() + "\",\"lat\":" + player.getLat() + ",\"lng\":" + player.getLng() + ",\"heading\":" + heading + "}");
										}
									}
									read(p.getConnection(), "ok", "join_party", "\"" + party.getId() + "\"");
									System.out.println(p.getName() + " joined party " + party.getId() + ".");
								}
								else error(conn, "Player already in a party!", message);
							}
							else error(conn, "Can't find party!", message);
						}
						else error(conn, "Can't find player!", message);
					}
					else error(conn, "No data provided!", message);
				}
				else error(conn, "Invalid action!", message);
			}
			else if (event.equalsIgnoreCase("leave_party")) {
				if (json.get("eventType").toString().equalsIgnoreCase("action")) {
					if (json.get("data") != null) {
						Player p = getPlayer(uuid, host);
						if (p != null && p.getConnection() != null) {
							Party party = getParty(json.get("data").toString());
							if (party != null) {
								party.leave(p);
								p.setParty(null);
								for (Player player: party.getPlayers()) {
									if (!player.getUuid().equals(p.getUuid())) {
										action(player.getConnection(), "ok", "left_party", "{\"uuid\":\"" + p.getUuid().toString() + "\"}");
									}
								}
								read(p.getConnection(), "ok", "leave_party", "\"" + party.getId() + "\"");
								System.out.println(p.getName() + " left party " + party.getId() + ".");
							}
							else error(conn, "Can't find party!", message);
						}
						else error(conn, "Can't find player!", message);
					}
					else error(conn, "No data provided!", message);
				}
				else error(conn, "Invalid action!", message);
			}
			else if (event.equalsIgnoreCase("chat")) {
				if (json.get("eventType").toString().equalsIgnoreCase("action")) {
					if (json.get("data") != null) {
						Player p = getPlayer(uuid, host);
						if (p != null && p.getConnection() != null) {
							Party party = getParty(p);
							if (party != null) {
								String msg = json.get("data").toString();
								JSONObject data =  new JSONObject();
								data.put("name", p.getName());
								data.put("message", msg);
								for (Player player: party.getPlayers()) {
									if (player.getConnection() != null)
										read(player.getConnection(), "message", "chat", data.toJSONString());
								}
								read(p.getConnection(), "ok", "chat", "\"sent_message\"");
								System.out.println(getTime() + ": [" + party.getId() + "/Chat] " + p.getName() + ": " + msg);
							}
							else error(conn, "Player hasn't joined a party!", message);
						}
						else error(conn, "Can't find player!", message);
					}
					else error(conn, "No data provided!", message);
				}
			}
			else if (event.equalsIgnoreCase("change_location")) {
				if (json.get("eventType").toString().equalsIgnoreCase("action")) {
					if (json.get("data") != null) {
						Player p = getPlayer(uuid, host);
						if (p != null && p.getConnection() != null) {
							JSONObject data = (JSONObject) json.get("data");
							String partyId = data.get("party").toString();
							String lat = data.get("lat").toString();
							String lng = data.get("lng").toString();
							String heading = data.get("heading").toString();
							Party party = getParty(partyId);
							if (party != null) {
								if (p.getParty() != null) {
									p.setLat(Helper.toFloat(lat));
									p.setLng(Helper.toFloat(lng));
									for (Player player: party.getPlayers()) {
										if (!player.getUuid().equals(p.getUuid()))
											action(player.getConnection(), "ok", "changed_location", "{\"uuid\":\"" + p.getUuid().toString() + "\",\"lat\":" + lat + ",\"lng\":" + lng + ",\"heading\":" + heading + "}");
									}
									read(p.getConnection(), "ok", "change_location", "\"" + party.getId() + "\"");
									System.out.println(getTime() + ": [" + party.getId() + "/Loc] " + p.getName() + " changed location.");
								}
								else error(conn, "Player isn't a party!", message);
							}
							else error(conn, "Can't find party!", message);
						}
						else error(conn, "Can't find player!", message);
					}
					else error(conn, "No data provided!", message);
				}
				else error(conn, "Invalid action!", message);
			}
			else if (event.equalsIgnoreCase("ride")) {
				if (json.get("eventType").toString().equalsIgnoreCase("action")) {
					if (json.get("data") != null) {
						Player p = getPlayer(uuid, host);
						if (p != null && p.getConnection() != null && p.getParty() != null) {
							String rideId = json.get("data").toString();
							Party party = p.getParty();
							if (party != null) {
								if (p.getParty() != null) {
									for (Player player: party.getPlayers()) {
										if (!player.getUuid().equals(p.getUuid()))
											action(player.getConnection(), "ok", "ride", "\"" + rideId + "\"");
									}
									action(p.getConnection(), "ok", "ride", "\"" + rideId + "\"");
									System.out.println(getTime() + ": [" + party.getId() + "/Ride] " + p.getName() + " started riding " + rideId + ".");
								}
								else error(conn, "Player isn't a party!", message);
							}
							else error(conn, "Can't find party!", message);
						}
						else error(conn, "Can't find player!", message);
					}
					else error(conn, "No data provided!", message);
				}
				else error(conn, "Invalid action!", message);
			}
			else System.out.println(host + ": " + message);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void onError(WebSocket conn, Exception ex) { ex.printStackTrace(); }

	private boolean hasConnection(UUID uuid, String host) {
		return players.stream().anyMatch(o -> o.getUuid().equals(uuid) && o.getHost().equals(host));
	}
	private Player getPlayer(UUID uuid, String host) {
		return players.stream().filter(o -> o.getUuid().equals(uuid) && o.getHost().equals(host)).findFirst().orElse(null);
	}
	private Party getParty(Player p) {
		return parties.stream().filter(o -> o.getPlayers().stream().anyMatch(i -> i.getUuid() == p.getUuid())).findFirst().orElse(null);
	}
	private Party getParty(String id) {
		return parties.stream().filter(o -> o.getId().equals(id)).findFirst().orElse(null);
	}

	private String getRandom() {
		String id = String.format("%04d", new Random().nextInt(9999));
		if (parties.stream().noneMatch(o -> id.equalsIgnoreCase(o.getId()))) return id;
		else return getRandom();
	}

	private String getTime() {
		Date date = new Date();
		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTime(date);
		return calendar.get(Calendar.HOUR_OF_DAY) + ":" + ((calendar.get(Calendar.MINUTE) < 10) ? "0" + calendar.get(Calendar.MINUTE) : calendar.get(Calendar.MINUTE));
	}

//	void action(Player p, String status, String event, String data) {
//		WebSocket conn = getConnection(p);
//		if (conn != null)
//			action(conn, status, event, data);
//	}
//	void read(Player p, String status, String event, String data) {
//		WebSocket conn = getConnection(p);
//		if (conn != null)
//			read(conn, status, event, data);
//	}
	private void action(WebSocket conn, String status, String event, String data) {
		conn.send(String.format(json("action"), status, event, data));
	}
	private void read(WebSocket conn, String status, String event, String data) {
		conn.send(String.format(json("read"), status, event, data));
	}
	private String json(String eventType) {
		return "{\"status\":\"%s\",\"eventType\":\"" + eventType + "\",\"event\":\"%s\",\"data\":%s}";
	}

	private void error(WebSocket conn, String error, String message) {
		if (conn.isOpen()) conn.send(String.format("{\"status\":\"error\",\"data\":\"%s\"}", error.replace("\"", "\\\"")));
		System.out.println(error);//ConsoleColor.RED + error + ConsoleColor.RESET
		System.out.println(message);//ConsoleColor.RED + message + ConsoleColor.RESET
	}
}
