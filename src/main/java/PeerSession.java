import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

public class PeerSession {
  private static final int HANDSHAKE_SIZE = 68;
  private static final int HANDSHAKE_PROTOCOL_SIZE = 19;
  private static final String HANDSHAKE_PROTOCOL = "BitTorrent protocol";

  private final String ipAddress;
  private final int port;
  private final String peerId;
  private final byte[] infoHash;

  private int currentPieceIndex;
  private int currentPieceLength;
  private int currentPieceOffset;

  private boolean inProgress;

  private Socket peerSocket;
  private OutputStream outputStream;
  private InputStream inputStream;

  private String sessionPeerId;

  public PeerSession(String ipAddress, int port, String peerId, byte[] infoHash) {
    this(ipAddress, port, peerId, infoHash, new Socket());
  }

  public PeerSession(String ipAddress, int port, String peerId, byte[] infoHash, Socket peerSocket) {
    if (ipAddress == null || ipAddress.isEmpty()) {
      throw new IllegalArgumentException("IP address cannot be null or empty");
    }
    if (port <= 0 || port > 65535) {
      throw new IllegalArgumentException("Port must be between 1 and 65535");
    }
    if (peerId == null || peerId.length() != 20 || !peerId.matches("[a-zA-Z0-9]{20}")) {
      throw new IllegalArgumentException("Peer ID must be 20 bytes long");
    }

    if (infoHash == null || infoHash.length != 20) {
      throw new IllegalArgumentException("Info hash must be 20 bytes long");
    }

    if (peerSocket == null) {
      throw new IllegalArgumentException("Socket cannot be null");
    }

    this.ipAddress = ipAddress;
    this.port = port;
    this.peerId = peerId;
    this.infoHash = infoHash;

    this.currentPieceIndex = 0;
    this.currentPieceLength = 0;
    this.currentPieceOffset = 0;
    this.inProgress = false;

    this.peerSocket = peerSocket;
  }

  public synchronized byte[] peerHandshake() throws IOException {
    peerSocket.connect(new InetSocketAddress(ipAddress, port), 5000);

    // Will automatically close the streams when done
    this.outputStream = peerSocket.getOutputStream();
    this.inputStream = peerSocket.getInputStream();
    byte[] handshake = buildHandshake();
    outputStream.write(handshake);
    outputStream.flush();

    byte[] response = new byte[HANDSHAKE_SIZE];
    int bytesRead = inputStream.read(response);

    if (bytesRead == -1) {
      throw new IOException("No response from peer");
    }


    // Check if the response is valid
    String protocol = new String(response, 1, HANDSHAKE_PROTOCOL_SIZE);
    if (!"BitTorrent protocol".equals(protocol)) {
      throw new IOException("Invalid response from peer. Response: " + Arrays.toString(response));
    }
    byte[] infoHash = Arrays.copyOfRange(response, 28, 48);
    byte[] peerId = Arrays.copyOfRange(response, 48, 68);
    if (!Arrays.equals(this.infoHash, infoHash)) {
      throw new IOException("Info hash mismatch");
    }

    this.sessionPeerId = new String(peerId);

    return response;
  }

  private byte[] buildHandshake() throws IOException {
    byte[] reserved = new byte[8];

    // Used to create a byte array (like StringBuilder is for string)
    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();

    byteArrayStream.write(HANDSHAKE_PROTOCOL_SIZE);
    byteArrayStream.write(HANDSHAKE_PROTOCOL.getBytes());
    byteArrayStream.write(reserved);
    byteArrayStream.write(infoHash);
    byteArrayStream.write(peerId.getBytes());

    return byteArrayStream.toByteArray();
  }

  public synchronized boolean establishInterested() {
    return false;
  }

  public synchronized  byte[] downloadPiece(int pieceIndex, int pieceLength, int pieceHash, int fileSize) {
    return new byte[pieceLength];
  }

  public synchronized void closeConnection() {
    try {
      if (peerSocket != null && !peerSocket.isClosed()) {
        peerSocket.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  // getters
  public String getIpAddress() {
    return ipAddress;
  }

  public int getPort() {
    return port;
  }

  public String getPeerId() {
    return peerId;
  }

  public int getCurrentPieceIndex() {
    return currentPieceIndex;
  }

  public int getCurrentPieceLength() {
    return currentPieceLength;
  }

  public int getCurrentPieceOffset() {
    return currentPieceOffset;
  }

  public boolean isInProgress() {
    return inProgress;
  }

  public String  getSessionPeerId() {
    return sessionPeerId;
  }

}
