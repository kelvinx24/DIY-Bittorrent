public interface TrackerClientFactory {
  TrackerClient create(String trackerUrl, int port, int fileSize, byte[] infoHash, String peerId);
}