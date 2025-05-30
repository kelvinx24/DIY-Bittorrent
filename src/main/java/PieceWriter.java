import java.io.IOException;

/**
 * Interface for writing pieces of downloaded data to a file
 *
 * @author KX
 */
public interface PieceWriter {

  /**
   * Writes a piece of data to the specified file at the given offset.
   * @param filePath the path to the file where the piece should be written
   * @param data the byte array containing the piece of data to write
   * @param offset the offset in the file where the piece should be written
   * @throws IOException  if an I/O error occurs while writing to the file
   */
  void writePiece(String filePath, byte[] data, int offset) throws IOException;

}
