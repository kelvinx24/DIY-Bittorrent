public class MockTrackerClientFactory implements TrackerClientFactory{

  private boolean throwError;

  public MockTrackerClientFactory() {
    this.throwError = false;
  }

  public MockTrackerClientFactory(boolean throwError) {
    this.throwError = throwError;
  }

  @Override
  public TrackerClient create(String trackerUrl, int port, int fileSize, byte[] infoHash,
      String peerId) {
    if (throwError) {
      return new MockTrackerClient(true);
    }

    return new MockTrackerClient();
  }
}
