import java.util.ArrayList;
import java.util.List;

public class MockPeerSessionFactory implements PeerSessionFactory {

  private List<byte[]> pieces;

  public MockPeerSessionFactory() {
    this.pieces = new ArrayList<>();
  }

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
