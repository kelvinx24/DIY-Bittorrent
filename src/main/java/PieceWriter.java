import java.io.IOException;

public interface PieceWriter {
  void writePiece(String filePath, byte[] data, int offset) throws IOException;

}
