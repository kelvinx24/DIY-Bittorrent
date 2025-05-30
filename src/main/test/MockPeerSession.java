import java.io.IOException;
import java.net.Socket;
import java.util.List;
import model.session.PeerSession;

/**
 * Mock implementation of the {@link PeerSession} class for testing purposes.
 * This class simulates a peer session without requiring actual network connections.
 *
 * @author KX
 */
public class MockPeerSession extends PeerSession {

  private List<byte[]> pieces;
  private PeerSession.SessionState sessionState = PeerSession.SessionState.IDLE;

  /**
   * Constructs a MockPeerSession with specified parameters. Has the ability to set pieces of data
   * to simulate downloading pieces of a torrent.
   *
   * @param ipAddress the IP address of the peer
   * @param port the port number of the peer
   * @param peerId the unique identifier for the peer
   * @param infoHash the info hash of the torrent
   * @param pieces a list of byte arrays representing pieces of data for testing
   */
  public MockPeerSession(String ipAddress, int port, String peerId, byte[] infoHash,
      List<byte[]> pieces) {
    super(ipAddress, port, peerId, infoHash);
    this.pieces = pieces;
  }

  /**
   * Copy of the constructor from {@link PeerSession}, allowing for the creation of a MockPeerSession
   * @param ipAddress the IP address of the peer
   * @param port the port number of the peer
   * @param peerId the unique identifier for the peer
   * @param infoHash the info hash of the torrent
   */
  public MockPeerSession(String ipAddress, int port, String peerId, byte[] infoHash) {
    super(ipAddress, port, peerId, infoHash);
  }

  /**
   * Copy of the constructor from {@link PeerSession}, allowing for the creation of a MockPeerSession
   * @param ipAddress the IP address of the peer
   * @param port the port number of the peer
   * @param peerId the unique identifier for the peer
   * @param infoHash the info hash of the torrent
   * @param peerSocket the socket connection to the peer
   */
  public MockPeerSession(String ipAddress, int port, String peerId, byte[] infoHash,
      Socket peerSocket) {
    super(ipAddress, port, peerId, infoHash, peerSocket);
  }

  /**
   * Default constructor for MockPeerSession. Initializes with default values.
   * This constructor is useful for testing purposes where no specific parameters are needed.
   */
  public MockPeerSession() {
    super("localhost", 6881, "0".repeat(20), new byte[20]);
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

  /**
   * Simulates downloading a piece of data from the peer. This method returns the piece
   * at the specified index from the predefined list of pieces.
   * @param pieceIndex   the index of the piece to download.
   * @param pieceLength  the length of the piece in bytes. Not used in this mock implementation.
   * @param expectedHash the expected SHA-1 hash of the piece, used for validation. Not used.
   * @param fileLength   the total length of the file being downloaded, used to adjust the final
   *                     piece size. Not used in this mock implementation.
   * @return the byte array representing the downloaded piece.
   */
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
    return sessionState;
  }

  /**
   * Sets the session state of the mock peer session.
   * @param sessionState the new session state to set
   */
  public void setSessionState(PeerSession.SessionState sessionState) {
    // Mock implementation for testing purposes
    this.sessionState = sessionState;
  }

  /**
   * Sets the pieces of data for the mock peer session.
   * This method allows defining the pieces that can be downloaded during testing.
   * @param pieces a list of byte arrays representing pieces of data
   */
  public void setPieces(List<byte[]> pieces) {
    // Mock implementation for testing purposes
    this.pieces = pieces;
  }
}
