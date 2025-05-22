import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class MockSocket extends Socket {
  private InputStream mockInputStream;
  private OutputStream mockOutputStream;

  public MockSocket() {
    super();
  }

  public MockSocket(InputStream inputStream, OutputStream outputStream) {
    super();
    this.mockInputStream = inputStream;
    this.mockOutputStream = outputStream;
  }

  @Override
  public void connect(java.net.SocketAddress endpoint, int timeout) {
    // Mock behavior: do nothing or simulate a successful connection
  }

  @Override
  public void close() {
    // Mock behavior: do nothing or simulate a successful close
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
