import java.net.http.HttpClient;

public class MockTrackerClient extends TrackerClient {

  private boolean throwException = false;

  public MockTrackerClient(String trackerUrl, int port, int downloadedFileSize, byte[] infoHash,
      String peerId) throws IllegalArgumentException {
    super(trackerUrl, port, downloadedFileSize, infoHash, peerId);
  }

  public MockTrackerClient(String trackerUrl, int port, int downloadedFileSize, byte[] infoHash,
      String peerId, HttpClient client) throws IllegalArgumentException {
    super(trackerUrl, port, downloadedFileSize, infoHash, peerId, client);
  }

  public MockTrackerClient() {
    super("http://bittorrent-test-tracker.codecrafters.io/announce", 8080, 1, new byte[20], "0".repeat(20));
  }

  public MockTrackerClient(boolean throwException) {
    this();
    this.throwException = throwException;
  }

  @Override
  public TrackerResponse requestTracker() throws TrackerCommunicationException,
      IllegalArgumentException, MalformedTrackerResponseException {
    if (throwException) {
      throw new TrackerCommunicationException("Mock exception thrown for testing purposes");
    }

    byte[] fakeTrackerResponse = "d8:intervali60e5:peers6:".getBytes();
    byte[] peerAddress = new byte[]{
        -91, -24, 38, -92, -56, -23
    };

    return new TrackerResponse(60, peerAddress);
  }

  public static int expectedPort() {
    return 51433;
  }

  public static String expectedAddress() {
    return "165.232.38.164";
  }
}
