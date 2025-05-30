/**
 * TrackerClientFactory produces instances of {@link TrackerClient}
 * which are used to communicate with a BitTorrent tracker.
 *
 * @author KX
 */
public interface TrackerClientFactory {

  /**
   * Creates a new instance of {@link TrackerClient}.
   * @param trackerUrl the URL of the tracker to connect to
   * @param port the port to use for the connection
   * @param fileSize the size of the file being downloaded
   * @param infoHash the SHA-1 hash of the file's info dictionary
   * @param peerId the unique identifier for the user
   * @return a new instance of {@link TrackerClient}
   */
  TrackerClient create(String trackerUrl, int port, int fileSize, byte[] infoHash, String peerId);
}