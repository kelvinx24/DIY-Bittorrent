import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    byte[] mockResponse = new byte[68];
    mockResponse[0] = 19; // Protocol length
    System.arraycopy("BitTorrent protocol".getBytes(), 0, mockResponse, 1, 19);
    System.arraycopy(tfh.getInfoHash(), 0, mockResponse, 28, 20);
    System.arraycopy("01234567890123456789".getBytes(), 0, mockResponse, 48, 20);

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

}
