import java.util.List;

public class MockTorrentFileHandler extends TorrentFileHandler {

  private List<byte[]> pieceHashes;

  public MockTorrentFileHandler() {
    super("sample.torrent"); // or dummy path if needed
    this.pieceHashes = List.of(
        new byte[] { /* Example hash for piece 0 */ },
        new byte[] { /* Example hash for piece 1 */ },
        new byte[] { /* Example hash for piece 2 */ }
    );
  }

  public MockTorrentFileHandler(String filePath) {
    super(filePath);
    this.pieceHashes = List.of(
        new byte[] { /* Example hash for piece 0 */ },
        new byte[] { /* Example hash for piece 1 */ },
        new byte[] { /* Example hash for piece 2 */ }
    );
  }

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