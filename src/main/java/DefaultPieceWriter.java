import java.io.IOException;
import java.io.RandomAccessFile;

public class DefaultPieceWriter implements PieceWriter {
  @Override
  public void writePiece(String filePath, byte[] data, int offset) throws IOException {
    try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
      raf.seek(offset);
      raf.write(data);
    }
  }
}