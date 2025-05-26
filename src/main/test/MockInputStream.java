import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

public class MockInputStream extends InputStream {

  private byte[] concatenatedBytes;
  private int currentIndex = 0;

  public MockInputStream(List<byte[]> readResponses) {
    super();

    this.currentIndex = 0;
    this.concatenatedBytes = new byte[0];

    if (readResponses != null && !readResponses.isEmpty()) {
      this.concatenatedBytes = createConcatenatedBytes(readResponses);
    }
  }

  public MockInputStream() {
    super();
  }

  @Override
  public int read() throws IOException {
    if (concatenatedBytes == null) {
      return -1;
    }
    return concatenatedBytes[currentIndex++];
  }

  @Override
  public byte[] readNBytes(int len) throws IOException {
    if (concatenatedBytes == null) {
      return null;
    }
    if (currentIndex >= concatenatedBytes.length) {
      return null;
    }
    int bytesToRead = Math.min(len, concatenatedBytes.length - currentIndex);
    byte[] bytes = new byte[bytesToRead];
    System.arraycopy(concatenatedBytes, currentIndex, bytes, 0, bytesToRead);
    currentIndex += bytesToRead;
    return bytes;

  }

  @Override
  public int read(byte[] b) {
    if (concatenatedBytes == null) {
      return -1;
    }

    if (currentIndex >= concatenatedBytes.length) {
      return -1;
    }

    int bytesToRead = Math.min(b.length, concatenatedBytes.length - currentIndex);
    System.arraycopy(concatenatedBytes, currentIndex, b, 0, bytesToRead);

    currentIndex += bytesToRead;

    return bytesToRead;
  }

  private byte[] createConcatenatedBytes(List<byte[]> readResponses) {
    int totalLength = 0;
    for (byte[] bytes : readResponses) {
      totalLength += bytes.length;
    }

    byte[] concatenated = new byte[totalLength];
    int currentPosition = 0;

    for (byte[] bytes : readResponses) {
      System.arraycopy(bytes, 0, concatenated, currentPosition, bytes.length);
      currentPosition += bytes.length;
    }

    return concatenated;
  }

  public void setReadResponses(List<byte[]> readResponses) {
    this.concatenatedBytes = createConcatenatedBytes(readResponses);
    this.currentIndex = 0;
  }

  public void reset() {
    this.currentIndex = 0;
  }
}
