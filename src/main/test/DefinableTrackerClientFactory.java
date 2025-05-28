import java.util.Map;

public class DefinableTrackerClientFactory implements TrackerClientFactory {

  private Map<String, Integer> peers;

  public DefinableTrackerClientFactory() {
    this.peers = Map.of();
  }

  public DefinableTrackerClientFactory(Map<String, Integer> peers) {
    this.peers = peers;
  }


  @Override
  public TrackerClient create(String trackerUrl, int port, int fileSize, byte[] infoHash,
      String peerId) {
    return new DefinableTrackerClient(peers);
  }

}
