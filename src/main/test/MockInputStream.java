import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

public class MockInputStream extends InputStream {

  private List<byte[]> readResponses;
  private Iterator<byte[]> iteratorResponse;

  public MockInputStream(List<byte[]> readResponses) {
    super();
    this.readResponses = readResponses;
    this.iteratorResponse = readResponses.iterator();
  }

  public MockInputStream() {
    super();
  }

  @Override
  public int read() throws IOException {
    if (readResponses == null) {
      return -1;
    }
    return 68;
  }

  @Override
  public int read(byte[] b) {
    if (readResponses == null) {
      return -1;
    }

    if (!iteratorResponse.hasNext()) {
      return -1;
    }
    byte[] readResponse = iteratorResponse.next();

    if (b.length == readResponse.length) {
      System.arraycopy(readResponse, 0, b, 0, readResponse.length);
    } else {
      System.arraycopy(readResponse, 0, b, 0,
          Math.min(b.length, readResponse.length));
    }

    return readResponse.length;
  }
}
