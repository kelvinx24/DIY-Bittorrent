import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockPieceWriter implements PieceWriter {

  Map<String, Integer> writtenOffsets;
  Map<Integer, byte[]> writtenPieces;

  public MockPieceWriter() {
    this.writtenOffsets = new HashMap<>();
    this.writtenPieces = new HashMap<>();
  }

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
