import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TorrentSessionTests {
  private MockTorrentFileHandler torrentFileHandler;
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

  @Test
  public void testDownloadPiece() throws PieceDownloadException, IOException {
    byte[] piece1 = new byte[16384 * 2];
    byte[] piece2 = new byte[16384 * 2];

    for (int i = 0; i < piece1.length; i++) {
      piece1[i] = (byte) (i % 256);
      piece2[i] = (byte) ((i + 1) % 256);
    }

    List<byte[]> pieces = Arrays.asList(piece1, piece2);

    byte[] pieceHash1 = TorrentFileHandler.sha1Hash(piece1);
    byte[] pieceHash2 = TorrentFileHandler.sha1Hash(piece2);

    List<byte[]> pieceHashes = Arrays.asList(pieceHash1, pieceHash2);

    torrentFileHandler.setPieceHashes(pieceHashes);

    TorrentSession ts = new TorrentSession(
        torrentFileHandler,
        Paths.get(OUTPUT_FILE_NAME),
        mockTrackerClientFactory,
        new MockPeerSessionFactory(pieces),
        mockPieceWriter,
        mockIdGenerator,
        Executors.newSingleThreadExecutor()
    );

    // Assuming piece index 0 is valid and can be downloaded
    byte[] pieceData = ts.downloadPiece(0);
    assertNotNull(pieceData, "Expected piece data to be downloaded");
    assertEquals(torrentFileHandler.getPieceLength(), pieceData.length, "Downloaded piece length mismatch");
    assertEquals(piece1.length, pieceData.length, "Downloaded piece length mismatch");
    assertArrayEquals(piece1, pieceData, "Downloaded piece data does not match expected hash");
  }

  @Test
  public void testInvalidDownloadPiece() throws PieceDownloadException, IOException {
    TorrentSession ts = new TorrentSession(
        torrentFileHandler,
        Paths.get(OUTPUT_FILE_NAME),
        mockTrackerClientFactory,
        mockPeerSessionFactory,
        mockPieceWriter,
        mockIdGenerator,
        Executors.newSingleThreadExecutor()
    );

    // Test with invalid piece index
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      ts.downloadPiece(-1);
    });
    assertEquals("Invalid piece index: -1", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      ts.downloadPiece(torrentFileHandler.getHashedPieces().size());
    });
    assertEquals("Invalid piece index: " + torrentFileHandler.getHashedPieces().size(), exception.getMessage());


    TorrentSession emptySession = new TorrentSession(
        torrentFileHandler,
        Paths.get(OUTPUT_FILE_NAME),
        new DefinableTrackerClientFactory(),
        mockPeerSessionFactory,
        mockPieceWriter,
        mockIdGenerator,
        Executors.newSingleThreadExecutor()
    );

    // Test with no peers available
    assertNull(emptySession.downloadPiece(0), "Expected null when no peers are available");

    byte[] piece1 = new byte[16384 * 2];
    byte[] piece2 = new byte[16384 * 2];

    for (int i = 0; i < piece1.length; i++) {
      piece1[i] = (byte) (i % 256);
      piece2[i] = (byte) ((i + 1) % 256);
    }

    List<byte[]> pieces = Arrays.asList(piece1, piece2);

    byte[] pieceHash1 = TorrentFileHandler.sha1Hash(piece1);
    byte[] pieceHash2 = TorrentFileHandler.sha1Hash(piece2);

    List<byte[]> pieceHashes = Arrays.asList(pieceHash1, pieceHash2);

    torrentFileHandler.setPieceHashes(pieceHashes);

    byte[] copy = Arrays.copyOf(piece1, pieceHash1.length);
    // Test with piece hash mismatch
    copy[0] ^= 1; // Modify the first byte to create a mismatch

    TorrentSession ts2 = new TorrentSession(
        torrentFileHandler,
        Paths.get(OUTPUT_FILE_NAME),
        mockTrackerClientFactory,
        new MockPeerSessionFactory(Arrays.asList(copy, piece2)),
        mockPieceWriter,
        mockIdGenerator,
        Executors.newSingleThreadExecutor()
    );

    Exception exception2 = assertThrows(IOException.class, () -> {
      ts2.downloadPiece(0);
    });
    assertEquals("Invalid piece data received", exception2.getMessage(), "Expected piece hash mismatch exception");
  }

  @Test
  public void testDownloadAllPieces() throws IOException {
    TorrentSession ts = new TorrentSession(
        torrentFileHandler,
        Paths.get(OUTPUT_FILE_NAME),
        mockTrackerClientFactory,
        mockPeerSessionFactory,
        mockPieceWriter,
        mockIdGenerator,
        Executors.newSingleThreadExecutor()
    );


  }

}
