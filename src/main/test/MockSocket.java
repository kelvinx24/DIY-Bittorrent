import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Mock implementation of a Socket that allows for testing without actual network connections.
 * This is useful for unit tests where you want to simulate socket behavior without relying on
 * real I/O operations.
 *
 * @author KX
 */
public class MockSocket extends Socket {
  private InputStream mockInputStream;
  private OutputStream mockOutputStream;

  private boolean isClosed;

  /**
   * Constructs a MockSocket with default parameters.
   * Initializes an empty socket without any input or output streams.
   */
  public MockSocket() {
    super();
  }

  /**
   * Constructs a MockSocket with specified input and output streams.
   *
   * @param inputStream the InputStream to be used for reading data
   * @param outputStream the OutputStream to be used for writing data
   */
  public MockSocket(InputStream inputStream, OutputStream outputStream) {
    super();
    this.mockInputStream = inputStream;
    this.mockOutputStream = outputStream;
    this.isClosed = false;
  }

  @Override
  public void connect(java.net.SocketAddress endpoint, int timeout) {
    // Mock behavior: do nothing or simulate a successful connection
  }

  @Override
  public void close() {
    // Mock behavior: do nothing or simulate a successful close
    this.isClosed = true;
  }

  @Override
  public boolean isClosed() {
    return isClosed;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return mockInputStream != null ? mockInputStream : super.getInputStream();
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return mockOutputStream != null ? mockOutputStream : super.getOutputStream();
  }

}
