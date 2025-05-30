import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import model.session.PieceDownloadException;
import model.session.PeerSession;
import model.session.PeerSessionFactory;
import model.session.TorrentFileHandler;
import model.session.TorrentSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * Unit tests for the model.session.TorrentSession class.
 * This class tests the initialization, piece downloading, and session management of model.session.TorrentSession.
 * It uses mock objects to simulate the behavior of external dependencies.
 *
 * @author KX
 */
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


  @Mock
  private MockPeerSession mockPeerSession1;
  @Mock
  private MockPeerSession mockPeerSession2;

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

    this.mockPeerSession1 = mock(MockPeerSession.class);
    this.mockPeerSession2 = mock(MockPeerSession.class);
  }

  /**
   * Tests the initialization of model.session.TorrentSession with valid parameters.
   * It checks if the session is created successfully and if all fields are initialized correctly.
   */
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

  /**
   * Tests the model.session.TorrentSession constructor with invalid parameters.
   * It checks if the constructor throws IllegalArgumentException when any parameter is null.
   */
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
  }

  /**
   * Checks if the model.session.TorrentSession can find remote peers successfully.
   */
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

  /**
   * Tests the findRemotePeers method with an invalid tracker client.
   * It checks if the method returns an empty list when no peers are found.
   */
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

  /**
   * Tests if the model.session.TorrentSession can download a piece successfully.
   */
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
    assertEquals(torrentFileHandler.getPieceLength(), pieceData.length,
        "Downloaded piece length mismatch");
    assertEquals(piece1.length, pieceData.length, "Downloaded piece length mismatch");
    assertArrayEquals(piece1, pieceData, "Downloaded piece data does not match expected hash");
  }

  /**
   * Tests the downloadPiece method with an invalid piece.
   * It checks if the method throws an IllegalArgumentException when the piece index is out of bounds.
   */
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
    assertEquals("Invalid piece index: " + torrentFileHandler.getHashedPieces().size(),
        exception.getMessage());

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
    assertEquals("Invalid piece data received", exception2.getMessage(),
        "Expected piece hash mismatch exception");
  }

  /**
   * Tests the downloadAll method of model.session.TorrentSession.
   * It checks if all pieces are downloaded successfully and written to the output file.
   * This test uses a synchronous execution model to ensure all pieces are downloaded in order.
   */
  @Test
  void testDownloadAll_SynchronousExecution() throws Exception {
    DefinableTrackerClientFactory trackerClientFactory = setupTrackerClientFactory(3);
    List<byte[]> pieces = setupPieces(5);
    List<byte[]> pieceHashes = new ArrayList<>();
    for (byte[] piece : pieces) {
      pieceHashes.add(TorrentFileHandler.sha1Hash(piece));
    }
    torrentFileHandler.setPieceHashes(pieceHashes);
    MockPeerSessionFactory peerSessionFactory = new MockPeerSessionFactory(pieces);

    MockPieceWriter pieceWriter = new MockPieceWriter();
    MockIdGenerator idGenerator = new MockIdGenerator();

    TorrentSession torrentSession = new TorrentSession(
        torrentFileHandler,
        Paths.get(OUTPUT_FILE_NAME),
        trackerClientFactory,
        peerSessionFactory,
        pieceWriter,
        idGenerator,
        Executors.newSingleThreadExecutor()
    );

    // Download all pieces synchronously
    torrentSession.downloadAll();
    assertEquals(5, torrentSession.getPieceStates().size(), "Expected 5 pieces to be downloaded");
    assertEquals(5, pieceWriter.getWrittenPieces().size(), "Expected 5 pieces to be written");

  }

  /**
   * Tests the downloadAll method of model.session.TorrentSession with concurrent execution.
   * It checks if multiple peer sessions can download pieces concurrently and if the output file is written correctly.
   * This test uses a CountDownLatch to synchronize the start of piece downloads across multiple threads.
   */
  @Test
  public void testDownloadAll_ConcurrentExecution() throws Exception {
    DefinableTrackerClientFactory trackerClientFactory = setupTrackerClientFactory(2);
    List<byte[]> pieces = setupPieces(2);
    List<byte[]> pieceHashes = new ArrayList<>();
    for (byte[] piece : pieces) {
      pieceHashes.add(TorrentFileHandler.sha1Hash(piece));
    }

    torrentFileHandler.setPieceHashes(pieceHashes);
    mockPeerSession1.setPieces(pieces);
    mockPeerSession2.setPieces(pieces);

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completionLatch = new CountDownLatch(2); // 2 peer sessions

    // Create a custom executor that we can control
    ExecutorService controlledExecutor = Executors.newFixedThreadPool(2);
    PeerSessionFactory mockitoPeerSessionFactory = setupMockPeerSessions();

    TorrentSession torrentSession = new TorrentSession(
        torrentFileHandler,
        Paths.get(OUTPUT_FILE_NAME),
        trackerClientFactory,
        mockitoPeerSessionFactory,
        this.mockPieceWriter,
        this.mockIdGenerator,
        controlledExecutor
    );

    // Configure peers to wait for our signal
    when(mockPeerSession1.downloadPiece(eq(0), anyInt(), any(), anyInt()))
        .thenAnswer(invocation -> {
          startLatch.await(5, TimeUnit.SECONDS);
          completionLatch.countDown();
          return pieces.get(0);
        });

    when(mockPeerSession1.downloadPiece(eq(1), anyInt(), any(), anyInt()))
        .thenAnswer(invocation -> {
          startLatch.await(5, TimeUnit.SECONDS);
          completionLatch.countDown();
          return pieces.get(1);
        });

    when(mockPeerSession2.downloadPiece(eq(0), anyInt(), any(), anyInt()))
        .thenAnswer(invocation -> {
          startLatch.await(5, TimeUnit.SECONDS);
          completionLatch.countDown();
          return pieces.get(0);
        });

    when(mockPeerSession2.downloadPiece(eq(1), anyInt(), any(), anyInt()))
        .thenAnswer(invocation -> {
          startLatch.await(5, TimeUnit.SECONDS);
          completionLatch.countDown();
          return pieces.get(1);
        });

    // Start download in separate thread
    Future<?> downloadFuture = Executors.newSingleThreadExecutor().submit(() -> {
      try {
        torrentSession.downloadAll();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    // Release the worker threads
    startLatch.countDown();

    // Wait for both peers to start processing
    assertTrue(completionLatch.await(5, TimeUnit.SECONDS));

    // Verify download completes
    assertDoesNotThrow(() -> downloadFuture.get(5, TimeUnit.SECONDS));

    controlledExecutor.shutdown();

    assertEquals(2, mockPieceWriter.getWrittenPieces().size(), "Expected 5 pieces to be written");
  }

  /**
   * Tests the downloadAll method of model.session.TorrentSession with peers in different states.
   * It checks if only the connected peer is used for downloading pieces.
   * This test simulates a scenario where one peer is idle and another is downloading.
   *
   * @throws Exception if any error occurs during the test
   */
  @Test
  void testDownloadAll_PeersWithDifferentStates() throws Exception {
    List<byte[]> pieces = setupPieces(2);
    List<byte[]> pieceHashes = hashedEquivalentPieces(pieces);

    torrentFileHandler.setPieceHashes(pieceHashes);
    PeerSessionFactory peerSessionFactory = setupMockPeerSessions();

    // One peer connected, one downloading (should be skipped)
    when(mockPeerSession1.getSessionState())
        .thenReturn(PeerSession.SessionState.IDLE);
    when(mockPeerSession2.getSessionState())
        .thenReturn(PeerSession.SessionState.DOWNLOADING);

    TorrentSession torrentSession = new TorrentSession(
        torrentFileHandler,
        Paths.get(OUTPUT_FILE_NAME),
        mockTrackerClientFactory,
        peerSessionFactory,
        mockPieceWriter,
        mockIdGenerator,
        Executors.newSingleThreadExecutor()
    );

    when(mockPeerSession1.downloadPiece(anyInt(), anyInt(), any(), anyInt()))
        .thenReturn(pieces.get(0))
        .thenReturn(pieces.get(1));

    torrentSession.downloadAll();

    // Only the connected peer should be used for downloading
    verify(mockPeerSession1, atLeastOnce()).downloadPiece(anyInt(), anyInt(), any(), anyInt());
    verify(mockPeerSession2, never()).downloadPiece(anyInt(), anyInt(), any(), anyInt());
  }

  /**
   * Tests the downloadAll method of model.session.TorrentSession when no peers are available.
   * It checks if the method throws an IllegalStateException when no peers can be found.
   */
  @Test
  void testDownloadAll_NoPeersAvailable() {
    DefinableTrackerClientFactory emptyTrackerClientFactory = new DefinableTrackerClientFactory(
        new HashMap<>());

    TorrentSession torrentSession = new TorrentSession(
        torrentFileHandler,
        Paths.get(OUTPUT_FILE_NAME),
        emptyTrackerClientFactory,
        mockPeerSessionFactory,
        mockPieceWriter,
        mockIdGenerator,
        Executors.newSingleThreadExecutor()
    );

    // Attempt to download all pieces with no peers available
    Exception exception = assertThrows(IllegalStateException.class, () -> {
      torrentSession.downloadAll();
    });

    assertEquals("No peers available for download", exception.getMessage(),
        "Expected exception when no peers are available");

  }

  /**
   * Tests the downloadAll method of model.session.TorrentSession with retry handling.
   * It checks if the method can handle retries for piece downloads and successfully downloads all pieces.
   * This test simulates a scenario where a piece download fails initially but succeeds on retry.
   *
   * @throws IOException if any I/O error occurs during the test
   */
  @Test
  public void testDownloadAll_RetryHandling() throws IOException {
    PeerSessionFactory peerSessionFactory = setupMockPeerSessions();
    List<byte[]> pieces = setupPieces(2);
    List<byte[]> pieceHashes = hashedEquivalentPieces(pieces);
    torrentFileHandler.setPieceHashes(pieceHashes);

    when(mockPeerSession1.downloadPiece(anyInt(), anyInt(), any(), anyInt()))
        .thenThrow(new PieceDownloadException("Simulated download failure"))
        .thenReturn(pieces.get(1)); // Simulate successful retry

    when(mockPeerSession2.downloadPiece(anyInt(), anyInt(), any(), anyInt()))
        .thenThrow(new PieceDownloadException("Simulated download failure"))
        .thenReturn(new byte[16384 * 2]) // wrong data to simulate retry failure
        .thenReturn(pieces.get(0));

    DefinableTrackerClientFactory mockTrackerClientFactory = setupTrackerClientFactory(2);

    TorrentSession torrentSession = new TorrentSession(
        torrentFileHandler,
        Paths.get(OUTPUT_FILE_NAME),
        mockTrackerClientFactory,
        peerSessionFactory,
        mockPieceWriter,
        mockIdGenerator);

    torrentSession.downloadAll();

    assertEquals(2, mockPieceWriter.getWrittenPieces().size(),
        "Expected 2 pieces to be downloaded");
    assertArrayEquals(pieces.get(0), mockPieceWriter.getWrittenPieces().get(0),
        "First piece should match expected data");

    assertArrayEquals(pieces.get(1), mockPieceWriter.getWrittenPieces().get(32768));


  }

  private DefinableTrackerClientFactory setupTrackerClientFactory(int numPeers) {

    Map<String, Integer> peers = new HashMap<>();

    for (int i = 0; i < numPeers; i++) {
      String randomIP = "127.0.0." + i;
      int randomPort = 1000 + i;
      peers.put(randomIP, randomPort);
    }

    DefinableTrackerClientFactory trackerClientFactory = new DefinableTrackerClientFactory(peers);
    return trackerClientFactory;
  }

  private List<byte[]> setupPieces(int numPieces) {
    List<byte[]> pieces = new ArrayList<>();
    for (int i = 0; i < numPieces; i++) {
      byte[] piece = new byte[16384 * 2]; // Example piece size
      for (int j = 0; j < piece.length; j++) {
        piece[j] = (byte) (j % 256) ; // Fill with some data
        piece[i] = (byte) ((j + i) % 256); // Modify to differentiate pieces
      }
      pieces.add(piece);
    }

    return pieces;
  }

  private PeerSessionFactory setupMockPeerSessions() {
    PeerSessionFactory mockPeerSessionFactory = mock(PeerSessionFactory.class);

    // Configure the factory to return our mock peer sessions
    when(mockPeerSessionFactory.create(any(), anyInt(), any(), any()))
        .thenReturn(mockPeerSession1)
        .thenReturn(mockPeerSession2);

    // Configure mock peer sessions
    when(mockPeerSession1.getSessionState())
        .thenReturn(PeerSession.SessionState.IDLE);
    when(mockPeerSession2.getSessionState())
        .thenReturn(PeerSession.SessionState.IDLE);

    when(mockPeerSession1.getIpAddress()).thenReturn("192.168.1.1");
    when(mockPeerSession2.getIpAddress()).thenReturn("192.168.1.2");

    return mockPeerSessionFactory;
  }

  private List<byte[]> hashedEquivalentPieces(List<byte[]> pieces) {
    List<byte[]> hashedPieces = new ArrayList<>();
    for (byte[] piece : pieces) {
      hashedPieces.add(TorrentFileHandler.sha1Hash(piece));
    }
    return hashedPieces;
  }
}
