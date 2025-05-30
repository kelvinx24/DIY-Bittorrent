import java.net.http.HttpClient;
import model.session.MalformedTrackerResponseException;
import model.session.TrackerClient;
import model.session.TrackerCommunicationException;
import model.session.TrackerResponse;

/**
 * A mock implementation of {@link TrackerClient} for testing purposes.
 * This class simulates the behavior of a tracker client without making real network requests.
 * It can be configured to throw exceptions or return predefined responses.
 *
 * @author KX
 */
public class MockTrackerClient extends TrackerClient {

  private boolean throwException = false;

  /**
   * Constructs a MockTrackerClient with specified parameters.
   *
   * @param trackerUrl the URL of the tracker
   * @param port the port number to connect to
   * @param downloadedFileSize the size of the downloaded file
   * @param infoHash the info hash of the torrent
   * @param peerId the peer ID
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public MockTrackerClient(String trackerUrl, int port, int downloadedFileSize, byte[] infoHash,
      String peerId) throws IllegalArgumentException {
    super(trackerUrl, port, downloadedFileSize, infoHash, peerId);
  }

  /**
   * Constructs a MockTrackerClient with specified parameters and an HTTP client.
   *
   * @param trackerUrl the URL of the tracker
   * @param port the port number to connect to
   * @param downloadedFileSize the size of the downloaded file
   * @param infoHash the info hash of the torrent
   * @param peerId the peer ID
   * @param client the HTTP client to use for requests
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public MockTrackerClient(String trackerUrl, int port, int downloadedFileSize, byte[] infoHash,
      String peerId, HttpClient client) throws IllegalArgumentException {
    super(trackerUrl, port, downloadedFileSize, infoHash, peerId, client);
  }

  /**
   * Constructs a MockTrackerClient with default parameters.
   * This constructor initializes the tracker client with a predefined URL, port, file size,
   * info hash, and peer ID.
   */
  public MockTrackerClient() {
    super("http://bittorrent-test-tracker.codecrafters.io/announce", 8080, 1, new byte[20], "0".repeat(20));
  }

  /**
   * Constructs a MockTrackerClient that can throw an exception when requesting the tracker.
   *
   * @param throwException if true, the requestTracker method will throw a model.session.TrackerCommunicationException
   */
  public MockTrackerClient(boolean throwException) {
    this();
    this.throwException = throwException;
  }

  /**
   * Requests the tracker and returns a simulated model.session.TrackerResponse.
   * If throwException is true, this method will throw a model.session.TrackerCommunicationException.
   *
   * @return a model.session.TrackerResponse containing a fake tracker response
   * @throws TrackerCommunicationException if throwException is true
   * @throws IllegalArgumentException if any parameter is invalid
   * @throws MalformedTrackerResponseException if the response is malformed
   */
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

  /**
   * Returns the expected port number for the tracker.
   *
   * @return the expected port number
   */
  public static int expectedPort() {
    return 51433;
  }

  /**
   * Returns the expected address of the tracker.
   *
   * @return the expected address as a string
   */
  public static String expectedAddress() {
    return "165.232.38.164";
  }
}
