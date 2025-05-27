public class MockPeerSessionFactory implements PeerSessionFactory {

  @Override
  public PeerSession create(String ip, int port, String peerId, byte[] infoHash) {
    return null;
  }
}
