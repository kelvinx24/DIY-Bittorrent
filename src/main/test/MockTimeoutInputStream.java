import java.io.IOException;
import java.util.List;

/**
 * Mock implementation of an InputStream that simulates a timeout during read operations. This is
 * useful for testing purposes where you want to simulate a delay in reading from a stream. Used in
 * unit tests for {@link TrackerClient} and {@link PeerSession}.
 *
 * @author KX
 */
public class MockTimeoutInputStream extends MockInputStream {

  private final int timeout;

  private boolean timeoutActivated = false;

  /**
   * Constructs a MockTimeoutInputStream with a list of byte arrays to read from and a specified
   * timeout.
   *
   * @param readResponses a list of byte arrays that will be concatenated and read sequentially
   * @param timeout       the timeout in milliseconds to simulate during read operations
   */
  public MockTimeoutInputStream(List<byte[]> readResponses, int timeout) {
    super(readResponses);
    this.timeout = timeout;
  }

  @Override
  public int read() throws IOException {
    if (timeoutActivated) {
      try {
        Thread.sleep(timeout);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt(); // Restore interrupted status
      }
    }
    return super.read();
  }


  public boolean isTimeoutActivated() {
    return timeoutActivated;
  }

  public void setTimeoutActivated(boolean timeoutActivated) {
    this.timeoutActivated = timeoutActivated;
  }

}
