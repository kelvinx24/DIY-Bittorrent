package model.session;

/**
 * model.session.PeerSessionFactory is an interface for creating {@link PeerSession} instances.
 * It provides a method to create a model.session.PeerSession with the necessary parameters.
 *
 * @author KX
 */
public interface PeerSessionFactory {

  /**
   * Creates a new {@link PeerSession} instance with the specified parameters.
   * @param ip the IP address of the peer
   * @param port the port number of the peer
   * @param peerId the unique identifier of the peer
   * @param infoHash the info hash of the torrent file
   * @return a new model.session.PeerSession instance
   */
  PeerSession create(String ip, int port, String peerId, byte[] infoHash);
}