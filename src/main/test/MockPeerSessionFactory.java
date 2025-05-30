import java.util.ArrayList;
import java.util.List;

/**
 * A mock implementation of the {@link PeerSessionFactory} interface for testing purposes.
 * This factory creates {@link MockPeerSession} instances with predefined pieces.
 *
 * @author KX
 */
public class MockPeerSessionFactory implements PeerSessionFactory {

  private List<byte[]> pieces;

  /**
   * Constructs a MockPeerSessionFactory with an empty list of pieces.
   */
  public MockPeerSessionFactory() {
    this.pieces = new ArrayList<>();
  }

  /**
   * Constructs a MockPeerSessionFactory with a specified list of pieces.
   *
   * @param pieces a list of byte arrays representing the pieces to be used in the mock sessions
   */
  public MockPeerSessionFactory(List<byte[]> pieces) {
    this.pieces = pieces;
  }

  @Override
  public PeerSession create(String ip, int port, String peerId, byte[] infoHash) {
    return new MockPeerSession(
        ip, port, peerId, infoHash, pieces);
  }

  public void setPieces(List<byte[]> pieces) {
    this.pieces = pieces;
  }
}
