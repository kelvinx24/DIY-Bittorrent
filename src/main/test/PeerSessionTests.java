import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class PeerSessionTests {

  @Test
  public void testInvalidInitialization() {
    // Initialize the PeerRequester with dummy values
    MockTorrentFileHandler tfh = new MockTorrentFileHandler();
    Exception ex = assertThrows(IllegalArgumentException.class,
        () -> new PeerSession(null, 6881, "01234567890123456789", tfh.getInfoHash()));
    assertTrue(ex.getMessage().contains("IP address cannot be null or empty"));

    ex = assertThrows(IllegalArgumentException.class,
        () -> new PeerSession("", 6881, "01234567890123456789", tfh.getInfoHash()));
    assertTrue(ex.getMessage().contains("IP address cannot be null or empty"));

    ex = assertThrows(IllegalArgumentException.class,
        () -> new PeerSession("localhost", -1, "01234567890123456789", tfh.getInfoHash()));
    assertTrue(ex.getMessage().contains("Port must be between 1 and 65535"));

    ex = assertThrows(IllegalArgumentException.class,
        () -> new PeerSession("localhost", 6881, null, tfh.getInfoHash()));
    assertTrue(ex.getMessage().contains("Peer ID must be 20 bytes long"));

    ex = assertThrows(IllegalArgumentException.class,
        () -> new PeerSession("localhost", 6881, "", tfh.getInfoHash()));
    assertTrue(ex.getMessage().contains("Peer ID must be 20 bytes long"));

    ex = assertThrows(IllegalArgumentException.class,
        () -> new PeerSession("localhost", 6881, "0123456789ABCDEF01234QWQWW", tfh.getInfoHash()));
    assertTrue(ex.getMessage().contains("Peer ID must be 20 bytes long"));

    ex = assertThrows(IllegalArgumentException.class,
        () -> new PeerSession("localhost", 6881, "01234567890123456789", null));
    assertTrue(ex.getMessage().contains("Info hash must be 20 bytes long"));

    ex = assertThrows(IllegalArgumentException.class,
        () -> new PeerSession("localhost", 6881, "01234567890123456789", new byte[19]));
    assertTrue(ex.getMessage().contains("Info hash must be 20 bytes long"));

    ex = assertThrows(IllegalArgumentException.class,
        () -> new PeerSession("localhost", 6881, "01234567890123456789", new byte[21]));
    assertTrue(ex.getMessage().contains("Info hash must be 20 bytes long"));

    ex = assertThrows(IllegalArgumentException.class,
        () -> new PeerSession("localhost", 6881, "01234567890123456789", new byte[20], null));
    assertTrue(ex.getMessage().contains("Socket cannot be null"));

  }

  @Test
  public void testValidInitialization() {
    // Initialize the PeerRequester with dummy values
    MockTorrentFileHandler tfh = new MockTorrentFileHandler();
    PeerSession peerSession = new PeerSession("localhost", 6881, "01234567890123456789",
        tfh.getInfoHash());
    assertEquals("localhost", peerSession.getIpAddress());
    assertEquals(6881, peerSession.getPort());
    assertEquals("01234567890123456789", peerSession.getPeerId());
    assertEquals(false, peerSession.isInProgress());


  }

  @Test
  public void testPeerHandshake() throws IOException {
    // Initialize the PeerRequester with dummy values
    MockTorrentFileHandler tfh = new MockTorrentFileHandler();

    // Mock the response from the peer
    byte[] mockResponse = createHandshakeResponse(tfh.getInfoHash());

    // Mock the socket
    MockInputStream mockInputStream = new MockInputStream(Arrays.asList(mockResponse));
    OutputStream mockOutputStream = mock(OutputStream.class);
    MockSocket mockSocket = new MockSocket(mockInputStream, mockOutputStream);

    PeerSession peerSession = new PeerSession("localhost", 6881, "01234567890123456789",
        tfh.getInfoHash(), mockSocket);
    byte[] response = peerSession.peerHandshake();
    assertEquals("01234567890123456789", peerSession.getSessionPeerId());
    assertEquals("BitTorrent protocol", new String(response, 1, 19));
    assertEquals("01234567890123456789", new String(response, 48, 20));
    assertArrayEquals(tfh.getInfoHash(), Arrays.copyOfRange(response, 28, 48));
    assertArrayEquals(mockResponse, response);

  }

  @Test
  public void testBadHandshake() throws IOException {
    // Initialize the PeerRequester with dummy values
    MockTorrentFileHandler tfh = new MockTorrentFileHandler();

    // Mock the socket
    MockInputStream badMockInputStream = new MockInputStream();
    OutputStream mockOutputStream = mock(OutputStream.class);
    MockSocket badMockSocket = new MockSocket(badMockInputStream, mockOutputStream);

    PeerSession peerSession = new PeerSession("localhost", 6881, "01234567890123456789",
        tfh.getInfoHash(), badMockSocket);

    Exception ex = assertThrows(IOException.class,
        peerSession::peerHandshake);
    assertTrue(ex.getMessage().contains("No response from peer"));

    // Invalid protocol
    byte[] badMockResponse = new byte[68];
    badMockResponse[0] = 19; // Protocol length
    System.arraycopy("BadTorrent protocol".getBytes(), 0, badMockResponse, 1, 19);
    System.arraycopy(tfh.getInfoHash(), 0, badMockResponse, 28, 20);
    System.arraycopy("01234567890123456789".getBytes(), 0, badMockResponse, 48, 20);
    MockInputStream badMockInputStream2 = new MockInputStream(Arrays.asList(badMockResponse));
    MockSocket badMockSocket2 = new MockSocket(badMockInputStream2, mockOutputStream);

    peerSession = new PeerSession("localhost", 6881, "01234567890123456789",
        tfh.getInfoHash(), badMockSocket2);
    ex = assertThrows(IOException.class,
        peerSession::peerHandshake);
    assertTrue(ex.getMessage().contains("Invalid response from peer"));

    // Info hash mismatch
    byte[] badMockResponse2 = new byte[68];
    badMockResponse2[0] = 19; // Protocol length
    System.arraycopy("BitTorrent protocol".getBytes(), 0, badMockResponse2, 1, 19);
    System.arraycopy("BadInfoHash 00000000".getBytes(), 0, badMockResponse2, 28, 20);
    System.arraycopy("01234567890123456789".getBytes(), 0, badMockResponse2, 48, 20);
    MockInputStream badMockInputStream3 = new MockInputStream(Arrays.asList(badMockResponse2));
    MockSocket badMockSocket3 = new MockSocket(badMockInputStream3, mockOutputStream);

    peerSession = new PeerSession("localhost", 6881, "01234567890123456789",
        tfh.getInfoHash(), badMockSocket3);
    ex = assertThrows(IOException.class,
        peerSession::peerHandshake);
    assertTrue(ex.getMessage().contains("Info hash mismatch"));
  }

  @Test
  public void testEstablishConnection() {
    // Initialize the PeerRequester with dummy values
    MockTorrentFileHandler tfh = new MockTorrentFileHandler();
    PeerSession peerSession = new PeerSession("localhost", 6881, "01234567890123456789",
        tfh.getInfoHash());
  }

  @Test
  public void testEstablishInterested() throws IOException {
    // Initialize the PeerRequester with dummy values
    MockTorrentFileHandler tfh = new MockTorrentFileHandler();

    List<byte[]> responses = new ArrayList<>();
    responses.add(createHandshakeResponse(tfh.getInfoHash()));
    responses.add(createBitfieldResponse());
    responses.add(createUnchokeResponse());
    MockInputStream mockInputStream = new MockInputStream(responses);


    OutputStream mockOutputStream = mock(OutputStream.class);
    MockSocket mockSocket = new MockSocket(mockInputStream, mockOutputStream);


    PeerSession peerSession = new PeerSession("localhost", 6881, "01234567890123456789",
        tfh.getInfoHash(), mockSocket);
    boolean success = peerSession.establishInterested();

    assertEquals(true, success);
  }

  @Test
  public void testEstablishInterestedBadBitfield() {
    MockTorrentFileHandler tfh = new MockTorrentFileHandler();
    List<byte[]> responses = new ArrayList<>();
    responses.add(createHandshakeResponse(tfh.getInfoHash()));
    responses.add(new byte[0]); // Empty bitfield

    MockInputStream mockInputStream = new MockInputStream(responses);
    OutputStream mockOutputStream = mock(OutputStream.class);
    MockSocket mockSocket = new MockSocket(mockInputStream, mockOutputStream);
    PeerSession peerSession = new PeerSession("localhost",
        6881, "01234567890123456789", tfh.getInfoHash(), mockSocket);
    Exception ex = assertThrows(IOException.class,
        peerSession::establishInterested);
    assertTrue(ex.getMessage().contains("Timeout waiting for BITFIELD message."));
  }

  @Test
  public void testEstablishInterestedBadUnchoke() {
    MockTorrentFileHandler tfh = new MockTorrentFileHandler();
    List<byte[]> responses = new ArrayList<>();
    responses.add(createHandshakeResponse(tfh.getInfoHash()));
    responses.add(createBitfieldResponse());
    responses.add(new byte[0]); // Empty unchoke

    MockInputStream mockInputStream = new MockInputStream(responses);
    OutputStream mockOutputStream = mock(OutputStream.class);
    MockSocket mockSocket = new MockSocket(mockInputStream, mockOutputStream);
    PeerSession peerSession = new PeerSession("localhost",
        6881, "01234567890123456789", tfh.getInfoHash(), mockSocket);
    Exception ex = assertThrows(IOException.class,
        peerSession::establishInterested);
    assertTrue(ex.getMessage().contains("Timeout waiting for UNCHOKE message."));
  }

  @Test
  public void testEstablishInterestedTimeout() throws IOException {
    MockTorrentFileHandler tfh = new MockTorrentFileHandler();
    List<byte[]> responses = new ArrayList<>();
    responses.add(createHandshakeResponse(tfh.getInfoHash()));
    // No bitfield response, simulating a timeout

    MockTimeoutInputStream mockInputStream = new MockTimeoutInputStream(responses,1100);
    OutputStream mockOutputStream = mock(OutputStream.class);
    MockSocket mockSocket = new MockSocket(mockInputStream, mockOutputStream);
    PeerSession peerSession = new PeerSession("localhost",
        6881, "01234567890123456789", tfh.getInfoHash(), mockSocket);

    peerSession.peerHandshake();
    mockInputStream.setTimeoutActivated(true); // Simulate timeout
    Exception ex = assertThrows(IOException.class,
        peerSession::establishInterested);
    assertTrue(ex.getMessage().contains("Timeout waiting for BITFIELD message."));
  }

  @Test
  public void testDownloadPiece() throws Exception {
    int pieceIndex = 0;
    int pieceLength = 32768; // 2 blocks of 16384
    int fileLength = 100000;

    // Create a mock piece data and its expected SHA-1 hash
    byte[] expectedData = new byte[pieceLength];
    new Random().nextBytes(expectedData);
    byte[] expectedHash = TorrentFileHandler.sha1Hash(expectedData); // or mock this

    // Create responses for the peer session
    List<byte[]> responses = new ArrayList<>();
    responses.add(createHandshakeResponse(expectedHash));
    responses.add(createBitfieldResponse());
    responses.add(createUnchokeResponse());

    // Simulate piece messages - each piece is split into blocks of 16384 bytes
    for (int offset = 0; offset < pieceLength; offset += 16384) {
      int blockLen = Math.min(16384, pieceLength - offset);
      byte[] block = Arrays.copyOfRange(expectedData, offset, offset + blockLen);

      ByteBuffer payload = ByteBuffer.allocate(4 + 4 + block.length);
      payload.putInt(pieceIndex);
      payload.putInt(offset);
      payload.put(block);

      ByteBuffer fullMessage = ByteBuffer.allocate(4 + 1 + payload.capacity());
      fullMessage.putInt(1 + payload.capacity()); // message length
      fullMessage.put((byte) 7); // piece message ID
      fullMessage.put(payload.array());

      responses.add(fullMessage.array());
    }

    MockInputStream mockIn = new MockInputStream(responses);
    ByteArrayOutputStream mockOut = new ByteArrayOutputStream();

    PeerSession downloader = new PeerSession("localhost", 6881, "01234567890123456789",
        expectedHash, new MockSocket(mockIn, mockOut));

    byte[] result = downloader.downloadPiece(pieceIndex, pieceLength, expectedHash, fileLength);

    assertArrayEquals(expectedData, result);
  }

  @Test
  public void testDownloadPieceTimeout() throws IOException {
    int pieceIndex = 0;
    int pieceLength = 32768; // 2 blocks of 16384
    int fileLength = 100000;

    // Create a mock piece data and its expected SHA-1 hash
    byte[] expectedData = new byte[pieceLength];
    new Random().nextBytes(expectedData);
    byte[] expectedHash = TorrentFileHandler.sha1Hash(expectedData); // or mock this

    // Create responses for the peer session
    List<byte[]> responses = new ArrayList<>();
    responses.add(createHandshakeResponse(expectedHash));
    responses.add(createBitfieldResponse());
    responses.add(createUnchokeResponse());

    MockTimeoutInputStream mockIn = new MockTimeoutInputStream(responses, 5100);
    ByteArrayOutputStream mockOut = new ByteArrayOutputStream();

    PeerSession downloader = new PeerSession("localhost", 6881, "01234567890123456789",
        expectedHash, new MockSocket(mockIn, mockOut));

    downloader.peerHandshake();
    downloader.establishInterested();
    mockIn.setTimeoutActivated(true); // Simulate timeout

    Exception ex = assertThrows(IOException.class,
        () -> downloader.downloadPiece(pieceIndex, pieceLength, expectedHash, fileLength));
    assertTrue(ex.getMessage().contains("Timed out while downloading piece"));
  }

  @Test
  public void testDownloadPieceHashMismatch() throws IOException {
    int pieceIndex = 0;
    int pieceLength = 32768; // 2 blocks of 16384
    int fileLength = 100000;

    // Create a mock piece data and its expected SHA-1 hash
    byte[] expectedData = new byte[pieceLength];
    new Random().nextBytes(expectedData);
    byte[] expectedHash = TorrentFileHandler.sha1Hash(expectedData); // or mock this

    // Create responses for the peer session
    List<byte[]> responses = new ArrayList<>();
    responses.add(createHandshakeResponse(expectedHash));
    responses.add(createBitfieldResponse());
    responses.add(createUnchokeResponse());

    // Simulate piece messages - each piece is split into blocks of 16384 bytes
    for (int offset = 0; offset < pieceLength; offset += 16384) {
      int blockLen = Math.min(16384, pieceLength - offset);
      byte[] block = Arrays.copyOfRange(expectedData, offset, offset + blockLen);

      ByteBuffer payload = ByteBuffer.allocate(4 + 4 + block.length);
      payload.putInt(pieceIndex);
      payload.putInt(offset);
      payload.put(block);

      ByteBuffer fullMessage = ByteBuffer.allocate(4 + 1 + payload.capacity());
      fullMessage.putInt(1 + payload.capacity()); // message length
      fullMessage.put((byte) 7); // piece message ID
      fullMessage.put(payload.array());

      responses.add(fullMessage.array());
    }

    MockInputStream mockIn = new MockInputStream(responses);
    ByteArrayOutputStream mockOut = new ByteArrayOutputStream();

    PeerSession downloader = new PeerSession("localhost", 6881, "01234567890123456789",
        expectedHash, new MockSocket(mockIn, mockOut));

    Exception ex = assertThrows(PieceDownloadException.class,
        () -> downloader.downloadPiece(pieceIndex, pieceLength, new byte[20], fileLength));
    assertTrue(ex.getMessage().contains("Piece hash mismatch"));
  }

  @Test
  public void testDownloadPieceInvalidIndex() throws IOException {
    int pieceIndex = 0;
    int pieceLength = 32768; // 2 blocks of 16384
    int fileLength = 100000;

    // Create a mock piece data and its expected SHA-1 hash
    byte[] expectedData = new byte[pieceLength];
    new Random().nextBytes(expectedData);
    byte[] expectedHash = TorrentFileHandler.sha1Hash(expectedData); // or mock this

    // Create responses for the peer session
    List<byte[]> responses = new ArrayList<>();
    responses.add(createHandshakeResponse(expectedHash));
    responses.add(createBitfieldResponse());
    responses.add(createUnchokeResponse());

    // Simulate piece messages - each piece is split into blocks of 16384 bytes
    for (int offset = 0; offset < pieceLength; offset += 16384) {
      int blockLen = Math.min(16384, pieceLength - offset);
      byte[] block = Arrays.copyOfRange(expectedData, offset, offset + blockLen);

      ByteBuffer payload = ByteBuffer.allocate(4 + 4 + block.length);
      payload.putInt(pieceIndex);
      payload.putInt(offset);
      payload.put(block);

      ByteBuffer fullMessage = ByteBuffer.allocate(4 + 1 + payload.capacity());
      fullMessage.putInt(1 + payload.capacity()); // message length
      fullMessage.put((byte) 7); // piece message ID
      fullMessage.put(payload.array());

      responses.add(fullMessage.array());
    }

    MockInputStream mockIn = new MockInputStream(responses);
    ByteArrayOutputStream mockOut = new ByteArrayOutputStream();

    PeerSession downloader = new PeerSession("localhost", 6881, "01234567890123456789",
        expectedHash, new MockSocket(mockIn, mockOut));

    Exception ex = assertThrows(PieceDownloadException.class,
        () -> downloader.downloadPiece(-1, pieceLength, expectedHash, fileLength));
    assertTrue(ex.getMessage().contains("Invalid piece index or offset received"));
  }


  @Test
  public void testDownloadPieceInvalidOffset() throws IOException {
    int pieceIndex = 0;
    int pieceLength = 32768; // 2 blocks of 16384
    int fileLength = 100000;

    // Create a mock piece data and its expected SHA-1 hash
    byte[] expectedData = new byte[pieceLength];
    new Random().nextBytes(expectedData);
    byte[] expectedHash = TorrentFileHandler.sha1Hash(expectedData); // or mock this

    // Create responses for the peer session
    List<byte[]> responses = new ArrayList<>();
    responses.add(createHandshakeResponse(expectedHash));
    responses.add(createBitfieldResponse());
    responses.add(createUnchokeResponse());

    // Simulate piece messages - each piece is split into blocks of 16384 bytes
    for (int offset = 0; offset < pieceLength; offset += 16384) {
      int blockLen = Math.min(16384, pieceLength - offset);
      byte[] block = Arrays.copyOfRange(expectedData, offset, offset + blockLen);

      ByteBuffer payload = ByteBuffer.allocate(4 + 4 + block.length);
      payload.putInt(5); // Invalid piece index
      payload.putInt(offset);
      payload.put(block);

      ByteBuffer fullMessage = ByteBuffer.allocate(4 + 1 + payload.capacity());
      fullMessage.putInt(1 + payload.capacity()); // message length
      fullMessage.put((byte) 7); // piece message ID
      fullMessage.put(payload.array());

      responses.add(fullMessage.array());
    }

    MockInputStream mockIn = new MockInputStream(responses);
    ByteArrayOutputStream mockOut = new ByteArrayOutputStream();

    PeerSession downloader = new PeerSession("localhost", 6881, "01234567890123456789",
        expectedHash, new MockSocket(mockIn, mockOut));

    Exception ex = assertThrows(PieceDownloadException.class,
        () -> downloader.downloadPiece(pieceIndex, pieceLength, expectedHash, fileLength));
    assertTrue(ex.getMessage().contains("Invalid piece index or offset received"));
  }


  private static byte[] createHandshakeResponse(byte[] infoHash) {
    byte[] mockResponse = new byte[68];
    mockResponse[0] = 19; // Protocol length
    System.arraycopy("BitTorrent protocol".getBytes(), 0, mockResponse, 1, 19);
    System.arraycopy(infoHash, 0, mockResponse, 28, 20);
    System.arraycopy("01234567890123456789".getBytes(), 0, mockResponse, 48, 20);

    return mockResponse;

  }

  private static byte[] createBitfieldResponse() {
    int lengthDefault = 4;
    int messageLength = 1;
    byte[] response = new byte[lengthDefault + messageLength];
    response[0] = 0;
    response[1] = 0;
    response[2] = 0;
    response[3] = 1;
    response[4] = 5; // Bitfield interested

    return response;

  }

  private static byte[] createUnchokeResponse() {
    int lengthDefault = 4;
    int messageLength = 1;
    byte[] unchokeResponse = new byte[lengthDefault + messageLength];
    unchokeResponse[0] = 0;
    unchokeResponse[1] = 0;
    unchokeResponse[2] = 0;
    unchokeResponse[3] = 1;
    unchokeResponse[4] = 1; // Unchoke

    return unchokeResponse;

  }



}
