package model.session;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * model.session.TrackerResponse is a data transfer object that encapsulates the response from a tracker server.
 * It contains the interval for the next request, a byte array of peers, and a map of peers with
 * their corresponding ports.
 */
public class TrackerResponse {

  private final int interval;
  private final byte[] peers;
  private final Map<String, Integer> peersMap;

  /**
   * Constructor for model.session.TrackerResponse with an interval and a byte array of peers. This constructor
   * initializes the interval and peers, and then parses the peers to create a map of IP addresses
   * and their corresponding ports.
   *
   * @param interval the interval in seconds for the next request to the tracker
   * @param peers    a byte array containing peer information, where each peer is represented by 6
   *                 bytes:
   */
  public TrackerResponse(int interval, byte[] peers) {
    this.interval = interval;
    this.peers = peers;
    this.peersMap = new LinkedHashMap<>();
    parsePeers();
  }

  /**
   * Parses the byte array of peers into a map of IP addresses and their corresponding ports. Each
   * peer is represented by 6 bytes: 4 for the IP address and 2 for the port. This method iterates
   * through the byte array in chunks of 6 bytes, extracts the IP address and port, and populates
   * the peersMap with the IP as the key and the port as the value.
   */
  private void parsePeers() {
    for (int i = 0; i < peers.length; i += 6) {
      String ip = String.format("%d.%d.%d.%d", peers[i] & 0xFF, peers[i + 1] & 0xFF,
          peers[i + 2] & 0xFF, peers[i + 3] & 0xFF);
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
