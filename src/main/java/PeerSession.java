import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class PeerSession {
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

  private boolean inProgress;
  private boolean firstTimeHandshake = true;

  private Socket peerSocket;
  private OutputStream outputStream;
  private InputStream inputStream;

  private String sessionPeerId;

  private record PeerMessage(int id, byte[] payload) {}

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

  public boolean establishInterested() throws IOException {
    if (this.inputStream == null || this.outputStream == null) {
      peerHandshake();
    }

    long startTime = System.currentTimeMillis();

    // Wait for BITFIELD message
    while (true) {
      if (System.currentTimeMillis() - startTime > HANDSHAKE_TIMEOUT_MS) {
        throw new IOException("Timeout waiting for BITFIELD message.");
      }

      PeerMessage message = readMessage(inputStream);
      if (message == null) continue;

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
      if (message == null) continue;

      if (message.id == UNCHOKE_RESPONSE_ID) {
        break;
      }
    }

    firstTimeHandshake = false;
    return true;
  }

  private void sendInterested(OutputStream out) throws IOException {
    ByteArrayOutputStream msg = new ByteArrayOutputStream();
    msg.write(intToBytes(1)); // length
    msg.write(2);             // ID = interested
    out.write(msg.toByteArray());
    out.flush();
  }

  private void sendRequest(OutputStream out, int index, int begin, int length) throws IOException {
    ByteArrayOutputStream msg = new ByteArrayOutputStream();
    msg.write(intToBytes(13)); // 1 (ID) + 12 (payload)
    msg.write(6);              // ID = request
    msg.write(intToBytes(index));
    msg.write(intToBytes(begin));
    msg.write(intToBytes(length));
    out.write(msg.toByteArray());
    out.flush();
  }

  private static PeerMessage readMessage(InputStream in) throws IOException {
    byte[] lenBytes = in.readNBytes(4);
    if (lenBytes == null || lenBytes.length != 4) return null;

    int length = ByteBuffer.wrap(lenBytes).getInt();
    if (length == 0) return null; // keep-alive

    int id = in.read();
    byte[] payload = in.readNBytes(length - 1);
    return new PeerMessage(id, payload);
  }

  public byte[] downloadPiece(int pieceIndex, int pieceLength, byte[] expectedHash, int fileLength)
      throws IOException, PieceDownloadException {
    int remainingBytes = fileLength - pieceIndex * pieceLength;

    if (firstTimeHandshake) {
      establishInterested(); // performs BITFIELD/UNCHOKE negotiation
    }

    // Adjust the final piece size if it's shorter
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

    while (totalReceived < pieceLength) {
      this.inProgress = true;

      // Check for timeout
      if (System.currentTimeMillis() - startTime > DOWNLOAD_TIMEOUT_MS) {
        this.inProgress = false;
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
        this.inProgress = false;
        throw new PieceDownloadException("Invalid piece index or offset received");
      }

      byte[] block = new byte[msg.payload.length - 8];
      payload.get(block);

      if (begin + block.length > pieceLength) {
        this.inProgress = false;
        throw new PieceDownloadException("Block exceeds piece boundaries");
      }

      System.arraycopy(block, 0, pieceData, begin, block.length);
      totalReceived += block.length;

      // Reset timeout on successful block
      startTime = System.currentTimeMillis();
    }

    // Validate SHA-1 hash of the downloaded piece
    byte[] actualHash = TorrentFileHandler.sha1Hash(pieceData);
    if (!Arrays.equals(actualHash, expectedHash)) {
      this.inProgress = false;
      throw new PieceDownloadException("Piece hash mismatch");
    }

    this.inProgress = false;
    return pieceData;
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

  private static byte[] intToBytes(int val) {
    return ByteBuffer.allocate(4).putInt(val).array();
  }


}
