import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Mock implementation of an InputStream that allows for reading predefined byte arrays. This is
 * useful for testing purposes where you want to simulate reading from a stream without relying on
 * actual I/O operations. Used in unit tests for {@link TrackerClient} and {@link PeerSession}
 *
 * @author KX
 */
public class MockInputStream extends InputStream {

  private byte[] concatenatedBytes;
  private int currentIndex = 0;

  /**
   * Constructs a MockInputStream with a list of byte arrays to read from.
   *
   * @param readResponses a list of byte arrays that will be concatenated and read sequentially
   */
  public MockInputStream(List<byte[]> readResponses) {
    super();

    this.currentIndex = 0;
    this.concatenatedBytes = new byte[0];

    if (readResponses != null && !readResponses.isEmpty()) {
      this.concatenatedBytes = createConcatenatedBytes(readResponses);
    }
  }

  /**
   * Default constructor for MockInputStream. Initializes an empty stream.
   */
  public MockInputStream() {
    super();
  }

  /**
   * Returns the byte at the current index and increments the index.
   *
   * @return the byte at the current index, or -1 if the end of the stream has been reached
   * throws IOException if an I/O error occurs
   */
  @Override
  public int read() throws IOException {
    if (concatenatedBytes == null) {
      return -1;
    }
    return concatenatedBytes[currentIndex++];
  }

  /**
   * Reads up to len bytes starting from the current index and returns them as a byte array.
   * @param len the maximum number of bytes to read
   * @return a byte array containing the read bytes, or null if there are no more bytes to read
   * @throws IOException
   */
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

  /**
   * Reads bytes into the provided byte array starting from the current index.
   * @param b the byte array to read bytes into
   * @return the number of bytes read, or -1 if there are no more bytes to read
   */
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

  /**
   * Concatenates a list of byte arrays into a single byte array.
   * @param readResponses the list of byte arrays to concatenate
   * @return a single byte array containing all the bytes from the list
   */
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

  /**
   * Resets the current index to 0, allowing the stream to be read from the beginning again.
   */
  public void reset() {
    this.currentIndex = 0;
  }
}
