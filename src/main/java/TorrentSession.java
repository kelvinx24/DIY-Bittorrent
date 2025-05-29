import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class TorrentSession {

  public enum PieceState {
    NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED
  }

  private final static int DEFAULT_PORT = 6881; // Default port for BitTorrent
  private final TrackerClient trackerClient;
  private final Set<PeerSession> peerSessions;
  private final Path outputFilePath;

  private final String peerId;

  private final int fileSize;
  private final int pieceLength;
  private final int numPieces;
  private final List<byte[]> pieceHashes;

  private final ConcurrentMap<Integer, PieceState> pieceStates;
  private final ConcurrentMap<Integer, PeerSession> pieceDownloaders;
  private final BlockingQueue<Integer> pieceQueue;

  private final TrackerClientFactory trackerClientFactory;
  private final PeerSessionFactory peerSessionFactory;
  private final PieceWriter pieceWriter;
  // TOOD: Replace with factory pattern such that it can be mocked in tests
  // whilst still allowing for changing thread pool size
  private ExecutorService executor;

  private List<Future<?>> downloadFutures = new ArrayList<>();

  public TorrentSession(
      TorrentFileHandler tfh,
      Path outputFilePath,
      TrackerClientFactory trackerClientFactory,
      PeerSessionFactory peerSessionFactory,
      PieceWriter pieceWriter,
      RandomIdGenerator idGenerator,
      ExecutorService executor) {

    if (tfh == null || outputFilePath == null || trackerClientFactory == null
        || peerSessionFactory == null || pieceWriter == null || idGenerator == null) {
      throw new IllegalArgumentException("Constructor parameters cannot be null");
    }

    this.outputFilePath = outputFilePath;
    this.peerId = idGenerator.generate();
    this.peerSessions = new HashSet<>();
    this.fileSize = tfh.getFileLength();
    this.pieceLength = tfh.getPieceLength();
    this.numPieces = (int) Math.ceil((double) fileSize / pieceLength);
    this.pieceHashes = tfh.getHashedPieces();
    this.peerSessionFactory = peerSessionFactory;
    this.pieceWriter = pieceWriter;
    this.executor = executor;
    this.trackerClientFactory = trackerClientFactory;

    this.trackerClient = trackerClientFactory.create(tfh.getTrackerUrl(), DEFAULT_PORT,
        this.fileSize, tfh.getInfoHash(), this.peerId);

    if (pieceHashes.size() != numPieces) {
      throw new IllegalArgumentException("Mismatch in number of pieces");
    }

    this.pieceStates = new ConcurrentHashMap<>();
    this.pieceDownloaders = new ConcurrentHashMap<>();
    this.pieceQueue = new LinkedBlockingDeque<>();
  }

  public TorrentSession (
      TorrentFileHandler tfh,
      Path outputFilePath,
      TrackerClientFactory trackerClientFactory,
      PeerSessionFactory peerSessionFactory,
      PieceWriter pieceWriter,
      RandomIdGenerator idGenerator) {
    this(tfh, outputFilePath, trackerClientFactory, peerSessionFactory, pieceWriter, idGenerator, null);
  }

  public List<PeerSession> findRemotePeers() {
    List<PeerSession> unconnectedPeers = new ArrayList<>();

    try {
      TrackerResponse tr = trackerClient.requestTracker();
      for (Entry<String, Integer> entry : tr.getPeersMap().entrySet()) {
        String ip = entry.getKey();
        int port = entry.getValue();
        PeerSession peerSession = peerSessionFactory.create(ip, port, peerId, trackerClient.getInfoHash());
        unconnectedPeers.add(peerSession);
      }

      return unconnectedPeers; // Return the list of unconnected peers
    } catch (Exception e) {
      return unconnectedPeers; // Return empty list if tracker request fails
    }
  }

  public byte[] downloadPiece(int pieceIndex) throws IOException, PieceDownloadException {
    if (pieceIndex < 0 || pieceIndex >= numPieces) {
      throw new IllegalArgumentException("Invalid piece index: " + pieceIndex);
    }

    if (peerSessions.isEmpty()) {
      initializePeerSessions();
      if (peerSessions.isEmpty()) {
        return null;
      }
    }

    while (true) {
      for (PeerSession peer : peerSessions) {
        if (peer.getSessionState() == PeerSession.SessionState.IDLE) {
          byte[] pieceData = peer.downloadPiece(pieceIndex, pieceLength,
              pieceHashes.get(pieceIndex), fileSize);

          byte[] expectedHash = pieceHashes.get(pieceIndex);

          if (pieceData != null && Arrays.equals(TorrentFileHandler.sha1Hash(pieceData), expectedHash)) {
            System.out.println("Downloaded piece " + pieceIndex + " from " + peer.getIpAddress());
            return pieceData;
          } else {
            throw new IOException("Invalid piece data received");
          }
        }
      }
    }

  }

  public void downloadAll() throws IOException {
    initializePeerSessions();
    if (peerSessions.isEmpty()) {
      throw new IllegalStateException("No peers available for download");
    }

    initializePieceQueue();
    initializeOutputFile();

    this.executor = (this.executor != null)
        ? this.executor
        : Executors.newFixedThreadPool(peerSessions.size());

    try {
      submitDownloadTasks(executor);
      awaitCompletion(executor);
    } finally {
      shutdownExecutor(executor);
    }
  }

  private void initializePieceQueue() {
    for (int i = 0; i < numPieces; i++) {
      pieceStates.put(i, PieceState.NOT_DOWNLOADED);
      pieceQueue.add(i);
    }
  }

  private void awaitCompletion(ExecutorService executor) throws IOException {
    executor.shutdown();
    try {
      // Wait for all tasks to complete with a reasonable timeout
      if (!executor.awaitTermination(300, TimeUnit.SECONDS)) {
        throw new IOException("Download timeout - not all pieces downloaded within time limit");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Download interrupted", e);
    }
  }

  private void shutdownExecutor(ExecutorService executor) {
    if (!executor.isShutdown()) {
      executor.shutdown();
    }

    try {
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        executor.shutdownNow();
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
          System.err.println("ExecutorService did not terminate cleanly");
        }
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }


  private void initializeOutputFile() throws FileNotFoundException, IOException {
    try (RandomAccessFile raf = new RandomAccessFile(outputFilePath.toFile(), "rw")) {
      raf.setLength(fileSize);
    }
  }

  private void submitDownloadTasks(ExecutorService executor) {
    List<Future<?>> futures = new ArrayList<>();
    for (PeerSession peerSession : peerSessions) {
      Future<?> future = executor.submit(() -> downloadPiecesForPeer(peerSession));
      futures.add(future);
    }
    // Store futures if you need to check individual task completion
    this.downloadFutures = futures;
  }


  private void downloadPiecesForPeer(PeerSession peerSession) {
    while (!pieceQueue.isEmpty() && !peerSession.getSessionState()
        .equals(PeerSession.SessionState.DOWNLOADING)) {
      Integer pieceIndex = pieceQueue.poll();
      if (pieceIndex == null) {
        break;
      }

      try {
        System.out.println("Starting download for piece " + pieceIndex +
            " from peer " + peerSession.getIpAddress());
        downloadSinglePiece(peerSession, pieceIndex);
      } catch (Exception e) {
        handleDownloadError(peerSession, pieceIndex, e);
        // Wait a bit before retrying to avoid overwhelming the peer
        try {
          Thread.sleep(1000); // Sleep for 1 second before retrying
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt(); // Restore interrupted status
          System.err.println("Download interrupted while waiting to retry piece " + pieceIndex);
        }
      }
    }
  }

  private void downloadSinglePiece(PeerSession peerSession, Integer pieceIndex)
      throws IOException, PieceDownloadException {
    PieceState state = pieceStates.get(pieceIndex);
    if (state == PieceState.NOT_DOWNLOADED) {
      pieceStates.put(pieceIndex, PieceState.DOWNLOADING);
      pieceDownloaders.put(pieceIndex, peerSession);

      byte[] pieceData = peerSession.downloadPiece(pieceIndex, pieceLength,
          pieceHashes.get(pieceIndex), fileSize);

      byte[] expectedHash = pieceHashes.get(pieceIndex);
      if (pieceData != null && Arrays.equals(TorrentFileHandler.sha1Hash(pieceData), expectedHash)) {
        writePieceToFile(outputFilePath.toString(), pieceData, pieceIndex * pieceLength);
        pieceStates.put(pieceIndex, PieceState.DOWNLOADED);
        pieceDownloaders.remove(pieceIndex);
        System.out.println("Downloaded piece " + pieceIndex + " from " + peerSession.getIpAddress());
      } else {
        throw new IOException("Invalid piece data");
      }
    }
  }

  private void handleDownloadError(PeerSession peerSession, Integer pieceIndex, Exception e) {
    System.err.println("Exception downloading piece " + pieceIndex + " from " +
        peerSession.getIpAddress() + ": " + e.getMessage());
    pieceDownloaders.remove(pieceIndex);
    pieceStates.put(pieceIndex, PieceState.NOT_DOWNLOADED);
    pieceQueue.add(pieceIndex);
  }

  private void writePieceToFile(String filePath, byte[] data, int offset)
      throws IOException {
    this.pieceWriter.writePiece(filePath, data, offset);
  }

  private void initializePeerSessions() {
    List<PeerSession> peers = findRemotePeers();

    for (PeerSession peerSession : peers) {
      try {
        peerSession.peerHandshake();
        peerSessions.add(peerSession);
      } catch (IOException e) {
        System.err.println(
            "Failed to connect to peer: " + peerSession.getIpAddress() + ":"
                + peerSession.getPort());
        e.printStackTrace();
      }
    }
  }


  public void closeAllConnections() {
    for (PeerSession peerSession : peerSessions) {
      try {
        peerSession.closeConnection();
      } catch (IOException e) {
        System.err.println("Failed to close connection to peer: " + peerSession.getIpAddress() + ":"
            + peerSession.getPort());
        e.printStackTrace();
      }
    }

    pieceDownloaders.clear();
    peerSessions.clear();
  }

  // Getters
  public String getPeerId() {
    return peerId;
  }

  public int getFileSize() {
    return fileSize;
  }

  public int getPieceLength() {
    return pieceLength;
  }

  public int getNumPieces() {
    return numPieces;
  }

  public List<byte[]> getPieceHashes() {
    return pieceHashes;
  }

  public Path getOutputFilePath() {
    return outputFilePath;
  }

  public Set<PeerSession> getPeerSessions() {
    return peerSessions;
  }

  public TrackerClient getTrackerClient() {
    return trackerClient;
  }

  public ConcurrentMap<Integer, PieceState> getPieceStates() {
    return pieceStates;
  }

  public ConcurrentMap<Integer, PeerSession> getPieceDownloaders() {
    return pieceDownloaders;
  }

  public BlockingQueue<Integer> getPieceQueue() {
    return pieceQueue;
  }

  public ExecutorService getExecutor() {
    return executor;
  }

  public List<Future<?>> getDownloadFutures() {
    return downloadFutures;
  }
}
