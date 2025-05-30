import java.util.List;
import model.session.TorrentFileHandler;

/**
 * Mock implementation of the {@link TorrentFileHandler} for testing purposes.
 * Has the ability to define piece hashes and other properties.
 */
public class MockTorrentFileHandler extends TorrentFileHandler {

  private List<byte[]> pieceHashes;

  /**
   * Constructs a MockTorrentFileHandler with default parameters.
   * Initializes with a sample torrent file path and predefined piece hashes.
   */
  public MockTorrentFileHandler() {
    super("sample.torrent"); // or dummy path if needed
    this.pieceHashes = List.of(
        new byte[] { /* Example hash for piece 0 */ },
        new byte[] { /* Example hash for piece 1 */ },
        new byte[] { /* Example hash for piece 2 */ }
    );
  }

  /**
   * Constructs a MockTorrentFileHandler with a specified file path.
   * Initializes with predefined piece hashes.
   *
   * @param filePath the path to the torrent file
   */
  public MockTorrentFileHandler(String filePath) {
    super(filePath);
    this.pieceHashes = List.of(
        new byte[] { /* Example hash for piece 0 */ },
        new byte[] { /* Example hash for piece 1 */ },
        new byte[] { /* Example hash for piece 2 */ }
    );
  }

  /**
   * Constructs a MockTorrentFileHandler with a specified file path and piece hashes.
   *
   * @param filePath the path to the torrent file
   * @param pieceHashes a list of byte arrays representing the piece hashes
   */
  public MockTorrentFileHandler(String filePath, List<byte[]> pieceHashes) {
    super(filePath);
    this.pieceHashes = pieceHashes;
  }

  @Override
  public byte[] getInfoHash() {
    return new byte[] {
        -42, -97, -111, -26, -78, -82, 76, 84, 36, 104, -47, 7, 58, 113, -44, -22, 19, -121, -102, 127
    };
  }

  @Override
  public String getTrackerUrl() {
    return "http://bittorrent-test-tracker.codecrafters.io/announce";
  }

  @Override
  public int getPieceLength() {
    return 16384 * 2; // Example piece length
  }

  @Override
  public int getFileLength() {
    return getPieceLength() * pieceHashes.size(); // Example total file length
  }

  @Override
  public List<byte[]> getHashedPieces() {
    return pieceHashes;
  }

  public void setPieceHashes(List<byte[]> pieceHashes) {
    this.pieceHashes = pieceHashes;
  }


}