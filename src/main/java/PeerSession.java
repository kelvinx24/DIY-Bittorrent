import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class PeerSession {
  private final String ipAddress;
  private final int port;
  private final String peerId;

  private int currentPieceIndex;
  private int currentPieceLength;
  private int currentPieceOffset;

  private boolean inProgress;

  private Socket peerSocket;
  private OutputStream outputStream;
  private InputStream inputStream;

  public PeerSession(String ipAddress, int port, String peerId) {
    this.ipAddress = ipAddress;
    this.port = port;
    this.peerId = peerId;
    this.currentPieceIndex = 0;
    this.currentPieceLength = 0;
    this.currentPieceOffset = 0;
    this.inProgress = false;

    this.peerSocket = new Socket();
  }

  public byte[] peerHandshake() {
    return new byte[0];
  }

  public boolean establishInterested() {
    return false;
  }

  public byte[] downloadPiece(int pieceIndex, int pieceLength, int pieceHash, int fileSize) {
    return new byte[pieceLength];
  }

  public void closeConnection() {
    try {
      if (peerSocket != null && !peerSocket.isClosed()) {
        peerSocket.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
