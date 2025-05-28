import java.net.http.HttpClient;
import java.util.Map;

public class DefinableTrackerClient extends TrackerClient {

  private Map<String, Integer> peers;

  public DefinableTrackerClient(String trackerUrl, int port, int downloadedFileSize, byte[] infoHash,
      String peerId) throws IllegalArgumentException {
    super(trackerUrl, port, downloadedFileSize, infoHash, peerId);
  }

  public DefinableTrackerClient(String trackerUrl, int port, int downloadedFileSize, byte[] infoHash,
      String peerId, HttpClient client) throws IllegalArgumentException {
    super(trackerUrl, port, downloadedFileSize, infoHash, peerId, client);
  }

  public DefinableTrackerClient() {
    super("http://bittorrent-test-tracker.codecrafters.io/announce", 8080, 1, new byte[20], "0".repeat(20));
  }

  public DefinableTrackerClient(Map<String, Integer> peers) {
    this();
    this.peers = peers;
  }

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
