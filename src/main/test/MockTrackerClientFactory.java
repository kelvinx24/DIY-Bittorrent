import model.session.TrackerClient;
import model.session.TrackerClientFactory;

/**
 * A mock implementation of {@link TrackerClientFactory} for testing purposes.
 * This factory creates instances of {@link MockTrackerClient}.
 */
public class MockTrackerClientFactory implements TrackerClientFactory {

  private final boolean throwError;

  /**
   * Constructs a MockTrackerClientFactory with default parameters. No errors will be thrown.
   */
  public MockTrackerClientFactory() {
    this.throwError = false;
  }

  /**
   * Constructs a MockTrackerClientFactory that can throw an error when creating a model.session.TrackerClient.
   *
   * @param throwError if true, the factory will create a model.session.TrackerClient that simulates an error
   */
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
