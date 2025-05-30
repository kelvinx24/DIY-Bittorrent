package model.session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * model.session.PeerSession class represents a session with a peer in a BitTorrent network. It handles the
 * handshake, interested state, and downloading pieces of a torrent.
 *
 * @author KX
 */
public class PeerSession {

  /**
   * Enum representing the state of the peer session.
   */
  public enum SessionState {
    UNINITIALIZED,
    HANDSHAKE,
    INTERESTED,
    IDLE,
    DOWNLOADING,
    COMPLETED
  }

  private static final int HANDSHAKE_SIZE = 68;
  private static final int HANDSHAKE_PROTOCOL_SIZE = 19;
  private static final String HANDSHAKE_PROTOCOL = "BitTorrent protocol";
  private static final int BITFIELD_RESPONSE_ID = 5;
  private static final int UNCHOKE_RESPONSE_ID = 1;
  private static final int INTERESTED_ID = 2;
  private static final int REQUEST_ID = 6;

  private static final int BLOCK_SIZE = 16384; // 16 KiB block size

  private static final int HANDSHAKE_TIMEOUT_MS = 10_000; // 10 seconds timeout
  private static final int DOWNLOAD_TIMEOUT_MS = 30_000; // 30 seconds timeout


  private final String ipAddress;
  private final int port;
  private final String peerId;
  private final byte[] infoHash;

  private int currentPieceIndex;
  private int currentPieceLength;
  private int currentPieceOffset;

  private SessionState sessionState;

  private final Socket peerSocket;
  private OutputStream outputStream;
  private InputStream inputStream;

  private String sessionPeerId;

  private record PeerMessage(int id, byte[] payload) {

  }

  /**
   * Constructs a model.session.PeerSession with the specified parameters. Uses a default Socket.
   * Values originate from torrent file and tracker response.
   *
   * @param ipAddress The IP address of the peer.
   * @param port      The port number of the peer.
   * @param peerId    The unique identifier for the peer (20 bytes).
   * @param infoHash  The info hash of the torrent (20 bytes).
   */
  public PeerSession(String ipAddress, int port, String peerId, byte[] infoHash) {
    this(ipAddress, port, peerId, infoHash, new Socket());
  }

  /**
   * Constructs a model.session.PeerSession with the specified parameters and a custom Socket.
   *
   * @param ipAddress  The IP address of the peer.
   * @param port       The port number of the peer.
   * @param peerId     The unique identifier for the peer (20 bytes).
   * @param infoHash   The info hash of the torrent (20 bytes).
   * @param peerSocket The socket to communicate with the peer. Typically used for testing or custom
   *                   configurations.
   */
  public PeerSession(String ipAddress, int port, String peerId, byte[] infoHash,
      Socket peerSocket) {
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

    this.sessionState = SessionState.UNINITIALIZED;

    this.peerSocket = peerSocket;
  }

  /**
   * Performs the initial handshake with the peer. This method connects to the peer, sends a
   * handshake message, and waits for a response. If the response is valid, it updates the session
   * state to HANDSHAKE and returns the response bytes. If the response is invalid or an error
   * occurs, it closes the connection and throws an IOException.
   *
   * @return the response bytes from the peer after the handshake.
   * @throws IOException if an error occurs during the handshake process, such as connection
   *                     failure,
   */
  public synchronized byte[] peerHandshake() throws IOException {
    peerSocket.connect(new InetSocketAddress(ipAddress, port), 5000);
    this.outputStream = peerSocket.getOutputStream();
    this.inputStream = peerSocket.getInputStream();

    // Build and send the handshake message
    byte[] handshake = buildHandshake();
    outputStream.write(handshake);
    outputStream.flush();

    // Read the response from the peer
    byte[] response = new byte[HANDSHAKE_SIZE];
    int bytesRead = inputStream.read(response);

    if (bytesRead == -1) {
      this.closeConnection();
      throw new IOException("No response from peer");
    }

    // Check if the response is valid
    String protocol = new String(response, 1, HANDSHAKE_PROTOCOL_SIZE);
    if (!"BitTorrent protocol".equals(protocol)) {
      this.closeConnection();
      throw new IOException("Invalid response from peer. Response: " + Arrays.toString(response));
    }
    byte[] infoHash = Arrays.copyOfRange(response, 28, 48);
    byte[] peerId = Arrays.copyOfRange(response, 48, 68);
    if (!Arrays.equals(this.infoHash, infoHash)) {
      this.closeConnection();
      throw new IOException("Info hash mismatch");
    }

    // Set the session state and peer ID after successful handshake
    this.sessionPeerId = new String(peerId);
    this.sessionState = SessionState.HANDSHAKE;

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

  /**
   * Establishes the interested state with the peer by performing the BITFIELD and UNCHOKE
   *
   * @return true if the interested state was successfully established, false otherwise.
   * @throws IOException if an error occurs during the process, such as a timeout waiting for
   *                     messages.
   */
  public boolean establishInterested() throws IOException {
    // Perform handshake if not already done
    if (this.inputStream == null || this.outputStream == null) {
      peerHandshake();
    }

    long startTime = System.currentTimeMillis(); // Start the timer for timeout

    // Wait for BITFIELD message
    while (true) {
      if (System.currentTimeMillis() - startTime > HANDSHAKE_TIMEOUT_MS) {
        throw new IOException("Timeout waiting for BITFIELD message.");
      }

      PeerMessage message = readMessage(inputStream);
      if (message == null) {
        continue;
      }

      if (message.id == BITFIELD_RESPONSE_ID) {
        break;
      }
    }

    // Send INTERESTED message
    sendInterested(outputStream);

    startTime = System.currentTimeMillis(); // reset for unchoke wait

    // Wait for UNCHOKE message
    while (true) {
      if (System.currentTimeMillis() - startTime > HANDSHAKE_TIMEOUT_MS) {
        throw new IOException("Timeout waiting for UNCHOKE message.");
      }

      PeerMessage message = readMessage(inputStream);
      if (message == null) {
        continue;
      }

      if (message.id == UNCHOKE_RESPONSE_ID) {
        break;
      }
    }

    this.sessionState = SessionState.INTERESTED;
    return true;
  }

  private void sendInterested(OutputStream out) throws IOException {
    ByteArrayOutputStream msg = new ByteArrayOutputStream();
    msg.write(intToBytes(1)); // length
    msg.write(INTERESTED_ID);             // ID = interested
    out.write(msg.toByteArray());
    out.flush();
  }

  private void sendRequest(OutputStream out, int index, int begin, int length) throws IOException {
    ByteArrayOutputStream msg = new ByteArrayOutputStream();
    msg.write(intToBytes(13)); // 1 (ID) + 12 (payload)
    msg.write(REQUEST_ID);              // ID = request
    msg.write(intToBytes(index));
    msg.write(intToBytes(begin));
    msg.write(intToBytes(length));
    out.write(msg.toByteArray());
    out.flush();
  }

  private static PeerMessage readMessage(InputStream in) throws IOException {
    byte[] lenBytes = in.readNBytes(4); // Read the first 4 bytes for length of payload
    if (lenBytes == null || lenBytes.length != 4) {
      return null;
    }

    int length = ByteBuffer.wrap(lenBytes).getInt();
    if (length == 0) {
      return null; // keep-alive
    }

    int id = in.read();
    byte[] payload = in.readNBytes(length - 1); // Read the rest of the payload
    return new PeerMessage(id, payload);
  }

  /**
   * Downloads a piece of the torrent from the peer. This method requests the specified piece and
   * waits for the blocks to be received. After combining the blocks, it validates the piece against
   * the expected SHA-1 hash and returns the downloaded piece data.
   *
   * @param pieceIndex   the index of the piece to download.
   * @param pieceLength  the length of the piece in bytes.
   * @param expectedHash the expected SHA-1 hash of the piece, used for validation.
   * @param fileLength   the total length of the file being downloaded, used to adjust the final
   *                     piece size
   * @return the downloaded piece data as a byte array.
   * @throws IOException if an error occurs during the download process, such as a timeout or
   *                     connection issue.
   * @throws PieceDownloadException if the downloaded piece does not match the expected hash or if
   *                                the piece index or offset is invalid.
   */
  public byte[] downloadPiece(int pieceIndex, int pieceLength, byte[] expectedHash, int fileLength)
      throws IOException, PieceDownloadException {

    // Ensure the session is in a valid state before downloading
    if (this.sessionState.ordinal() < SessionState.INTERESTED.ordinal()) {
      establishInterested(); // performs BITFIELD/UNCHOKE negotiation
    }

    // Adjust the final piece size if it's shorter
    int remainingBytes = fileLength - pieceIndex * pieceLength;
    if (remainingBytes < pieceLength) {
      pieceLength = Math.max(0, remainingBytes);
    }

    // Request all blocks in this piece
    for (int offset = 0; offset < pieceLength; offset += BLOCK_SIZE) {
      int blockLength = Math.min(BLOCK_SIZE, pieceLength - offset);
      sendRequest(outputStream, pieceIndex, offset, blockLength);
    }

    byte[] pieceData = new byte[pieceLength];
    int totalReceived = 0;
    long startTime = System.currentTimeMillis();

    this.sessionState = SessionState.DOWNLOADING;
    // Receive the blocks for the piece, assemble them, and validate the piece
    try {
      setPieceState(pieceIndex, pieceLength, 0);
      while (totalReceived < pieceLength) {

        // Check for timeout
        if (System.currentTimeMillis() - startTime > DOWNLOAD_TIMEOUT_MS) {
          throw new IOException("Timed out while downloading piece " + pieceIndex);
        }

        PeerMessage msg = readMessage(inputStream);
        if (msg == null) {
          continue; // Ignore keep-alive or malformed messages
        }

        if (msg.id != 7) {
          continue; // Not a piece message
        }

        ByteBuffer payload = ByteBuffer.wrap(msg.payload);
        int receivedIndex = payload.getInt();
        int begin = payload.getInt();

        if (receivedIndex != pieceIndex || begin < 0 || begin >= pieceLength) {
          throw new PieceDownloadException("Invalid piece index or offset received");
        }

        byte[] block = new byte[msg.payload.length - 8];
        payload.get(block);

        if (begin + block.length > pieceLength) {
          throw new PieceDownloadException("Block exceeds piece boundaries");
        }

        System.arraycopy(block, 0, pieceData, begin, block.length);
        totalReceived += block.length;
        currentPieceOffset = begin + block.length; // Update current piece offset for next block

        // Reset timeout on successful block
        startTime = System.currentTimeMillis();
      }

      // Validate SHA-1 hash of the downloaded piece
      byte[] actualHash = TorrentFileHandler.sha1Hash(pieceData);
      if (!Arrays.equals(actualHash, expectedHash)) {
        throw new PieceDownloadException("Piece hash mismatch");
      }

      return pieceData;
    } finally {
      // Ensure we reset the piece state and session state if an exception occurs
      resetPieceState();
      this.sessionState = SessionState.IDLE;
    }

  }

  private void resetPieceState() {
    this.currentPieceIndex = 0;
    this.currentPieceLength = 0;
    this.currentPieceOffset = 0;
  }

  private void setPieceState(int pieceIndex, int pieceLength, int pieceOffset) {
    this.currentPieceIndex = pieceIndex;
    this.currentPieceLength = pieceLength;
    this.currentPieceOffset = pieceOffset;
  }

  /**
   * Closes the connection to the peer and resets the session state to UNINITIALIZED.
   *
   * @throws IOException if an error occurs while closing the connection.
   */
  public synchronized void closeConnection() throws IOException {
    if (peerSocket != null && !peerSocket.isClosed()) {

      if (outputStream != null) {
        outputStream.flush();
        outputStream.close();

      }

      if (inputStream != null) {
        inputStream.close();
      }

      peerSocket.close();
      this.sessionState = SessionState.UNINITIALIZED;

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

  public SessionState getSessionState() {
    return sessionState;
  }

  public String getSessionPeerId() {
    return sessionPeerId;
  }

  private static byte[] intToBytes(int val) {
    return ByteBuffer.allocate(4).putInt(val).array();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PeerSession)) {
      return false;
    }
    PeerSession that = (PeerSession) o;
    return port == that.port &&
        ipAddress.equals(that.ipAddress) &&
        peerId.equals(that.peerId) &&
        Arrays.equals(infoHash, that.infoHash);
  }

  @Override
  public int hashCode() {
    int result = ipAddress.hashCode();
    result = 31 * result + port;
    result = 31 * result + peerId.hashCode();
    result = 31 * result + Arrays.hashCode(infoHash);
    return result;

  }


}
