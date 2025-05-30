/**
 * Default implementation of the {@link PeerSessionFactory} interface.
 * This factory creates instances of {@link PeerSession} with the provided parameters.
 *
 * @author KX
 */
public class DefaultPeerSessionFactory implements PeerSessionFactory {

  @Override
  public PeerSession create(String ip, int port, String peerId, byte[] infoHash) {
    if (ip == null || ip.isEmpty()) {
      throw new IllegalArgumentException("IP address cannot be null or empty");
    }
    if (port <= 0 || port > 65535) {
      throw new IllegalArgumentException("Port must be between 1 and 65535");
    }
    if (peerId == null || peerId.isEmpty()) {
      throw new IllegalArgumentException("Peer ID cannot be null or empty");
    }
    if (infoHash == null || infoHash.length != 20) {
      throw new IllegalArgumentException("Info hash must be a 20-byte array");
    }

    return new PeerSession(ip, port, peerId, infoHash);
  }
}
