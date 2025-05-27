public interface PeerSessionFactory {
  PeerSession create(String ip, int port, String peerId, byte[] infoHash);
}