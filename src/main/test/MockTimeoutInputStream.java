import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MockTimeoutInputStream extends MockInputStream {

  private final int timeout;

  private boolean timeoutActivated = false;

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
