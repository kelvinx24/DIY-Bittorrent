import java.util.Map;

/**
 * A factory for creating instances of {@link DefinableTrackerClient}.
 * This factory allows for the definition of peers that the tracker client will use.
 *
 * @author KX
 */
public class DefinableTrackerClientFactory implements TrackerClientFactory {

  private Map<String, Integer> peers;

  /**
   * Constructs a DefinableTrackerClientFactory with no predefined peers.
   */
  public DefinableTrackerClientFactory() {
    this.peers = Map.of();
  }

  /**
   * Constructs a DefinableTrackerClientFactory with specified peers.
   *
   * @param peers a map of peer IP addresses and their corresponding ports
   */
  public DefinableTrackerClientFactory(Map<String, Integer> peers) {
    this.peers = peers;
  }


  @Override
  public TrackerClient create(String trackerUrl, int port, int fileSize, byte[] infoHash,
      String peerId) {
    return new DefinableTrackerClient(peers);
  }

}
