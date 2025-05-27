public class DefaultTrackerClientFactory implements TrackerClientFactory {
  @Override
  public TrackerClient create(String trackerUrl, int port, int fileSize, byte[] infoHash, String peerId) {
    return new TrackerClient(trackerUrl, port, fileSize, infoHash, peerId);
  }
}