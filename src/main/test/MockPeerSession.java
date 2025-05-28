import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class MockPeerSession extends PeerSession {

  private List<byte[]> pieces;

  public MockPeerSession(String ipAddress, int port, String peerId, byte[] infoHash,
      List<byte[]> pieces) {
    super(ipAddress, port, peerId, infoHash);
    this.pieces = pieces;
  }

  public MockPeerSession(String ipAddress, int port, String peerId, byte[] infoHash) {
    super(ipAddress, port, peerId, infoHash);
  }

  public MockPeerSession(String ipAddress, int port, String peerId, byte[] infoHash,
      Socket peerSocket) {
    super(ipAddress, port, peerId, infoHash, peerSocket);
  }

  @Override
  public byte[] peerHandshake() throws IOException {
    // Mock implementation for testing purposes
    return new byte[0];
  }

  @Override
  public boolean establishInterested() {
    // Mock implementation for testing purposes
    return true;
  }

  @Override
  public byte[] downloadPiece(int pieceIndex, int pieceLength, byte[] expectedHash, int fileLength) {
    // Mock implementation for testing purposes
    if (pieceIndex < 0 || pieceIndex >= pieces.size()) {
      throw new IndexOutOfBoundsException("Piece index out of bounds");
    }
    return pieces.get(pieceIndex);
  }

  @Override
  public PeerSession.SessionState getSessionState() {
    // Mock implementation for testing purposes
    return PeerSession.SessionState.IDLE;
  }
}
