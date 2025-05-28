import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TorrentSessionTests {
  private TorrentFileHandler torrentFileHandler;
  private MockInputStream mockInputStream;
  private OutputStream mockOutputStream;
  private MockSocket mockSocket;

  private MockTrackerClientFactory mockTrackerClientFactory;
  private MockPeerSessionFactory mockPeerSessionFactory;
  private MockPieceWriter mockPieceWriter;
  private MockIdGenerator mockIdGenerator;

  private TorrentSession torrentSession;

  private static final String OUTPUT_FILE_NAME = "output.torrent";


  @BeforeEach
  public void setUp() {
    // This method can be used to set up any common test data or configurations
    // before each test
    this.torrentFileHandler = new MockTorrentFileHandler();
    this.mockInputStream = new MockInputStream();
    this.mockOutputStream = mock(OutputStream.class);
    this.mockSocket = new MockSocket(mockInputStream, mockOutputStream);
    this.mockTrackerClientFactory = new MockTrackerClientFactory();
    this.mockPeerSessionFactory = new MockPeerSessionFactory();
    this.mockPieceWriter = new MockPieceWriter();
    this.mockIdGenerator = new MockIdGenerator();
  }

  @Test
  public void testTorrentSessionInitialization() {
    TorrentSession ts = new TorrentSession(
        torrentFileHandler,
        Paths.get(OUTPUT_FILE_NAME),
        mockTrackerClientFactory,
        mockPeerSessionFactory,
        mockPieceWriter,
        mockIdGenerator,
        Executors.newSingleThreadExecutor()
        );

    assertNotNull(ts);
    assertEquals(20, ts.getPeerId().length());
    assertEquals(torrentFileHandler.getFileLength(), ts.getFileSize());
    assertEquals(torrentFileHandler.getPieceLength(), ts.getPieceLength());
    assertEquals(OUTPUT_FILE_NAME, ts.getOutputFilePath().toString());
    assertEquals(torrentFileHandler.getHashedPieces().size(), ts.getPieceHashes().size());

    assertEquals(0, ts.getPeerSessions().size());
    assertEquals(0, ts.getPieceStates().size());
    assertEquals(0, ts.getPieceDownloaders().size());
    assertEquals(0, ts.getPieceQueue().size());

    assertNotNull(ts.getTrackerClient());
    assertEquals(torrentFileHandler.getTrackerUrl(), ts.getTrackerClient().getTrackerUrl());
  }

  @Test
  public void testTorrentSessionInvalidInitialization() {
    // Test with null torrentFileHandler
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      new TorrentSession(null,
          Paths.get(OUTPUT_FILE_NAME),
          mockTrackerClientFactory,
          mockPeerSessionFactory,
          mockPieceWriter,
          mockIdGenerator,
          Executors.newSingleThreadExecutor());
    });
    assertEquals("Constructor parameters cannot be null", exception.getMessage());

    // Test with null outputPath
    exception = assertThrows(IllegalArgumentException.class, () -> {
      new TorrentSession(torrentFileHandler,
          null,
          mockTrackerClientFactory,
          mockPeerSessionFactory,
          mockPieceWriter,
          mockIdGenerator,
          Executors.newSingleThreadExecutor());
    });
    assertEquals("Constructor parameters cannot be null", exception.getMessage());

    // Test with null trackerClientFactory
    exception = assertThrows(IllegalArgumentException.class, () -> {
      new TorrentSession(torrentFileHandler,
          Paths.get(OUTPUT_FILE_NAME),
          null,
          mockPeerSessionFactory,
          mockPieceWriter,
          mockIdGenerator,
          Executors.newSingleThreadExecutor());
    });
    assertEquals("Constructor parameters cannot be null", exception.getMessage());

    // Test with null peerSessionFactory
    exception = assertThrows(IllegalArgumentException.class, () -> {
      new TorrentSession(torrentFileHandler,
          Paths.get(OUTPUT_FILE_NAME),
          mockTrackerClientFactory,
          null,
          mockPieceWriter,
          mockIdGenerator,
          Executors.newSingleThreadExecutor());
    });
    assertEquals("Constructor parameters cannot be null", exception.getMessage());

    // Test with null pieceWriter
    exception = assertThrows(IllegalArgumentException.class, () -> {
      new TorrentSession(torrentFileHandler,
          Paths.get(OUTPUT_FILE_NAME),
          mockTrackerClientFactory,
          mockPeerSessionFactory,
          null,
          mockIdGenerator,
          Executors.newSingleThreadExecutor());
    });
    assertEquals("Constructor parameters cannot be null", exception.getMessage());

    // Test with null idGenerator
    exception = assertThrows(IllegalArgumentException.class, () -> {
      new TorrentSession(torrentFileHandler,
          Paths.get(OUTPUT_FILE_NAME),
          mockTrackerClientFactory,
          mockPeerSessionFactory,
          mockPieceWriter,
          null,
          Executors.newSingleThreadExecutor());
    });
    assertEquals("Constructor parameters cannot be null", exception.getMessage());

    // Test with null executorService
    exception = assertThrows(IllegalArgumentException.class, () -> {
      new TorrentSession(torrentFileHandler,
          Paths.get(OUTPUT_FILE_NAME),
          mockTrackerClientFactory,
          mockPeerSessionFactory,
          mockPieceWriter,
          mockIdGenerator,
          null);
    });
    assertEquals("Constructor parameters cannot be null", exception.getMessage());
  }

  @Test
  public void testValidFindRemotePeers() {
    TorrentSession ts = new TorrentSession(
        torrentFileHandler,
        Paths.get(OUTPUT_FILE_NAME),
        mockTrackerClientFactory,
        mockPeerSessionFactory,
        mockPieceWriter,
        mockIdGenerator,
        Executors.newSingleThreadExecutor()
    );

    // expected
    List<PeerSession> peers = ts.findRemotePeers();
    assertNotNull(peers);
    assertEquals(1, peers.size());
    PeerSession peer = peers.get(0);
    assertNotNull(peer);
    assertEquals(MockTrackerClient.expectedPort(), peer.getPort());
    assertEquals(MockTrackerClient.expectedAddress(), peer.getIpAddress());
  }

  @Test
  public void testInvalidFindRemotePeers() {
    TorrentSession ts = new TorrentSession(
        torrentFileHandler,
        Paths.get(OUTPUT_FILE_NAME),
        new MockTrackerClientFactory(true),
        mockPeerSessionFactory,
        mockPieceWriter,
        mockIdGenerator,
        Executors.newSingleThreadExecutor()
    );


    List<PeerSession> peers = ts.findRemotePeers();
    assertNotNull(peers);
    assertTrue(peers.isEmpty(), "Expected no peers to be found with default tracker client");
  }


}
