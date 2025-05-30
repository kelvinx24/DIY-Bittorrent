import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Default implementation of the {@link PieceWriter} interface that writes pieces of data to a file.
 * Writes data to a specified file at a given offset using a RandomAccessFile.
 *
 * @author KX
 */
public class DefaultPieceWriter implements PieceWriter {
  @Override
  public void writePiece(String filePath, byte[] data, int offset) throws IOException {
    try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
      raf.seek(offset);
      raf.write(data);
    }
  }
}