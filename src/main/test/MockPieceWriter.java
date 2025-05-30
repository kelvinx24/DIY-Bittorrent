import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import model.session.PieceWriter;

/**
 * Mock implementation of the model.session.PieceWriter interface for testing purposes.
 * This class simulates writing pieces of data to a file by storing them in memory.
 * It allows verification of written offsets and pieces without actual file I/O.
 *
 * @author KX
 */
public class MockPieceWriter implements PieceWriter {

  private final Map<String, Integer> writtenOffsets;
  private final Map<Integer, byte[]> writtenPieces;

  /**
   * Constructs a MockPieceWriter with empty maps for written offsets and pieces.
   */
  public MockPieceWriter() {
    this.writtenOffsets = new HashMap<>();
    this.writtenPieces = new HashMap<>();
  }

  /**
   * Puts a piece of data into the written pieces map with its offset.
   *
   * @param filePath the path to the file where the piece should be written. Not used.
   * @param data the byte array containing the piece of data to write
   * @param offset the offset in the file where the piece should be written
   * @throws IOException if an I/O error occurs while writing to the file. Not used.
   */
  @Override
  public void writePiece(String filePath, byte[] data, int offset) throws IOException {
    if (writtenOffsets.containsKey(filePath)) {
      int currentOffset = writtenOffsets.get(filePath);
      writtenOffsets.put(filePath, currentOffset + data.length);
    } else {
      writtenOffsets.put(filePath, data.length);
    }
    writtenPieces.put(offset, data);
  }

  public Map<String, Integer> getWrittenOffsets() {
    return writtenOffsets;
  }

  public Map<Integer, byte[]> getWrittenPieces() {
    return writtenPieces;
  }

  /**
   * Retrieves all written pieces as a single byte array.
   * If no pieces have been written, returns null.
   *
   * @return a byte array containing all written pieces concatenated, or null if no pieces were written
   */
  public byte[] getWritten() {
    if (writtenPieces.isEmpty()) {
      return null;
    }

    int totalLength = 0;
    for (byte[] piece : writtenPieces.values()) {
      totalLength += piece.length;
    }

    byte[] result = new byte[totalLength];
    int currentIndex = 0;
    for (byte[] piece : writtenPieces.values()) {
      System.arraycopy(piece, 0, result, currentIndex, piece.length);
      currentIndex += piece.length;
    }
    return result;
  }
}
