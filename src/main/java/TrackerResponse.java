import java.util.LinkedHashMap;
import java.util.Map;

public class TrackerResponse {
	private final int interval;
	private final byte[] peers;
	private Map<String, Integer> peersMap;

	public TrackerResponse(int interval, byte[] peers) {
		this.interval = interval;
		this.peers = peers;
		this.peersMap = new LinkedHashMap<>();
		parsePeers();
	}

	private void parsePeers() {
		for (int i = 0; i < peers.length; i += 6) {
			String ip = String.format("%d.%d.%d.%d", peers[i] & 0xFF, peers[i + 1] & 0xFF, peers[i + 2] & 0xFF, peers[i + 3] & 0xFF);
			int port = ((peers[i + 4] & 0xFF) << 8) | (peers[i + 5] & 0xFF);
			peersMap.put(ip, port);
		}
	}

	public int getInterval() {
		return interval;
	}

	public byte[] getPeers() {
		return peers;
	}

	public Map<String, Integer> getPeersMap() {
		return peersMap;
	}

}
