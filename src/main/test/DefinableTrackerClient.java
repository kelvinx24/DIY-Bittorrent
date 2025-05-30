import java.util.Map;

/**
 * A mock implementation of {@link TrackerClient} that allows defining peers for testing purposes.
 *
 * @author KX
 */
public class DefinableTrackerClient extends TrackerClient {

  private Map<String, Integer> peers;

  /**
   * Constructs a DefinableTrackerClient with default parameters.
   *
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public DefinableTrackerClient() {
    super("http://bittorrent-test-tracker.codecrafters.io/announce", 8080, 1, new byte[20], "0".repeat(20));
  }

  /**
   * Constructs a DefinableTrackerClient with specified peers.
   *
   * @param peers a map of peer IP addresses and their corresponding ports
   */
  public DefinableTrackerClient(Map<String, Integer> peers) {
    this();
    this.peers = peers;
  }

  /**
   * Mock implementation of the requestTracker method.
   * @return a TrackerResponse containing the defined peers
   */
  @Override
  public TrackerResponse requestTracker() throws TrackerCommunicationException,
      IllegalArgumentException, MalformedTrackerResponseException {

    return new TrackerResponse(0, peersToBytes());
  }

  private byte[] peersToBytes() {
    // 4 bytes for each peer (IP) and 2 bytes for the port
    int totalBytes = peers.size() * 6; // 4 bytes for IP + 2 bytes for port
    byte[] peerBytes = new byte[totalBytes];
    int index = 0;

    for (Map.Entry<String, Integer> entry : peers.entrySet()) {
      String[] ipParts = entry.getKey().split("\\.");
      for (String part : ipParts) {
        peerBytes[index++] = (byte) Integer.parseInt(part);
      }
      int port = entry.getValue();
      peerBytes[index++] = (byte) (port >> 8); // High byte
      peerBytes[index++] = (byte) (port & 0xFF); // Low byte
    }

    return peerBytes;
  }
}
