package model.session;

/**
 * Default implementation fo the {@link TrackerClientFactory} interface.
 * This factory creates instances of {@link TrackerClient} with the provided parameters.
 *
 * @author KX
 */
public class DefaultTrackerClientFactory implements TrackerClientFactory {
  @Override
  public TrackerClient create(String trackerUrl, int port, int fileSize, byte[] infoHash, String peerId) {
    return new TrackerClient(trackerUrl, port, fileSize, infoHash, peerId);
  }
}