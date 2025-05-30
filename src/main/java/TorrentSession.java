import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

/**
 * Represents a torrent session that manages the downloading of pieces from remote peers for a
 * tracker. This class handles the interaction with the tracker, manages peer sessions, and
 * coordinates the downloading of pieces. It can download pieces individually, or all at once
 * concurrently using multiple peer sessions.
 *
 * @author KX
 */
public class TorrentSession {

  /**
   * Represents the state of a piece in the torrent session. NOT_DOWNLOADED: The piece has not been
   * downloaded yet. DOWNLOADING: The piece is currently being downloaded. DOWNLOADED: The piece has
   * been successfully downloaded.
   */
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

  /**
   * Maps piece indices to their current state in the torrent session. This allows tracking which
   * pieces have been downloaded, are being downloaded, or not downloaded.
   */
  private final ConcurrentMap<Integer, PieceState> pieceStates;
  /**
   * Maps piece indices to the PeerSession that is currently downloading that piece. This allows
   * tracking which peer is responsible for downloading each piece.
   */
  private final ConcurrentMap<Integer, PeerSession> pieceDownloaders;
  /**
   * Queue of piece indices that are yet to be downloaded.
   */
  private final BlockingQueue<Integer> pieceQueue;

  private final TrackerClientFactory trackerClientFactory;
  private final PeerSessionFactory peerSessionFactory;
  private final PieceWriter pieceWriter;
  private ExecutorService executor;

  /**
   * List of futures representing the download tasks for each peer session. This allows tracking the
   * completion of each download task.
   */
  private List<Future<?>> downloadFutures = new ArrayList<>();

  /**
   * Constructs a TorrentSession with the specified parameters. Uses torrent files to initialize the
   * session and prepare for downloading pieces from remote peers.
   *
   * @param tfh                  {@link TorrentFileHandler} instance to handle downloads from
   *                             torrent files.
   * @param outputFilePath       Path where the downloaded file will be saved.
   * @param trackerClientFactory Factory to create {@link TrackerClient} instance.
   * @param peerSessionFactory   Factory to create {@link PeerSession} instances.
   * @param pieceWriter          PieceWriter instance to handle writing pieces to the file.
   * @param idGenerator          RandomIdGenerator to generate a unique peer ID.
   * @param executor             ExecutorService for managing concurrent downloads (optional).
   *                             Useful for testing purposes, can be set to null to use default
   *                             thread pool.
   */
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

    this.trackerClient = this.trackerClientFactory.create(tfh.getTrackerUrl(), DEFAULT_PORT,
        this.fileSize, tfh.getInfoHash(), this.peerId);

    if (pieceHashes.size() != numPieces) {
      throw new IllegalArgumentException("Mismatch in number of pieces");
    }

    this.pieceStates = new ConcurrentHashMap<>();
    this.pieceDownloaders = new ConcurrentHashMap<>();
    this.pieceQueue = new LinkedBlockingDeque<>();
  }

  /**
   * Constructs a TorrentSession with the specified parameters. The executor is set to null, which
   * means the session will use as many threads as there are peer sessions available.
   *
   * @param tfh                  {@link TorrentFileHandler} instance to handle downloads from
   *                             torrent files.
   * @param outputFilePath       Path where the downloaded file will be saved.
   * @param trackerClientFactory Factory to create {@link TrackerClient} instance.
   * @param peerSessionFactory   Factory to create {@link PeerSession} instances.
   * @param pieceWriter          PieceWriter instance to handle writing pieces to the file.
   * @param idGenerator          RandomIdGenerator to generate a unique peer ID.
   */
  public TorrentSession(
      TorrentFileHandler tfh,
      Path outputFilePath,
      TrackerClientFactory trackerClientFactory,
      PeerSessionFactory peerSessionFactory,
      PieceWriter pieceWriter,
      RandomIdGenerator idGenerator) {
    this(tfh, outputFilePath, trackerClientFactory, peerSessionFactory, pieceWriter, idGenerator,
        null);
  }

  /**
   * Finds remote peers from the tracker and creates PeerSession instances for each peer. This
   * method requests the tracker for a list of peers and initializes {@link PeerSession} objects for
   * each peer found.
   *
   * @return List of PeerSession objects representing remote peers.
   */
  public List<PeerSession> findRemotePeers() {
    List<PeerSession> unconnectedPeers = new ArrayList<>();

    try {
      TrackerResponse tr = trackerClient.requestTracker();
      for (Entry<String, Integer> entry : tr.getPeersMap().entrySet()) {
        String ip = entry.getKey();
        int port = entry.getValue();
        PeerSession peerSession = peerSessionFactory.create(ip, port, peerId,
            trackerClient.getInfoHash());
        unconnectedPeers.add(peerSession);
      }

      return unconnectedPeers; // Return the list of unconnected peers
    } catch (Exception e) {
      return unconnectedPeers; // Return empty list if tracker request fails
    }
  }

  /**
   * Downloads a specific piece from the torrent session using the available peer sessions. This
   * method attempts to download the specified piece from any available peer session that is
   * currently idle. If the piece is successfully downloaded and its hash matches the expected hash,
   * it returns the piece data. If the piece cannot be downloaded or the hash does not match, it
   * throws an IOException or PieceDownloadException.
   * <p>
   * Does not close the peer sessions after downloading the piece, allowing for reuse of peer
   * sessions for subsequent downloads.
   *
   * @param pieceIndex the index of the piece to download
   * @return the byte array containing the downloaded piece data, or null if no peers are available
   * @throws IOException            if an I/O error occurs during the download process
   * @throws PieceDownloadException if the downloaded piece data is invalid or does not match the
   *                                expected hash
   */
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

    // Search for an idle peer session to download the piece
    while (true) {
      for (PeerSession peer : peerSessions) {
        if (peer.getSessionState() == PeerSession.SessionState.IDLE) {
          byte[] pieceData = peer.downloadPiece(pieceIndex, pieceLength,
              pieceHashes.get(pieceIndex), fileSize);

          byte[] expectedHash = pieceHashes.get(pieceIndex);

          if (pieceData != null && Arrays.equals(TorrentFileHandler.sha1Hash(pieceData),
              expectedHash)) {
            System.out.println("Downloaded piece " + pieceIndex + " from " + peer.getIpAddress());
            return pieceData;
          } else {
            throw new IOException("Invalid piece data received");
          }
        }
      }
    }

  }

  /**
   * Downloads all pieces from the torrent session using multiple peer sessions concurrently. This
   * method initializes peer sessions, prepares the piece queue, and starts downloading pieces from
   * all available peers. It uses an ExecutorService to manage concurrent downloads.
   * <p>
   * It will close all peer sessions after the download is complete, ensuring that resources are
   * released properly.
   *
   * @throws IOException if an I/O error occurs during the download process or if no peers are
   *                     available for download
   */
  public void downloadAll() throws IOException {
    // Initialize peer sessions and prepare for downloading
    initializePeerSessions();
    if (peerSessions.isEmpty()) {
      throw new IllegalStateException("No peers available for download");
    }

    initializePieceQueue();
    initializeOutputFile();

    this.executor = (this.executor != null)
        ? this.executor
        : Executors.newFixedThreadPool(peerSessions.size());

    // Submit download tasks to the executor, each task will download pieces from a peer
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

  /**
   * Awaits the completion of all download tasks submitted to the executor service. This method
   * blocks until all tasks are completed or a timeout occurs.
   *
   * @param executor the ExecutorService managing the download tasks
   * @throws IOException if the download times out or is interrupted
   */
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

  /**
   * Shuts down the executor service gracefully, waiting for tasks to complete or forcing shutdown
   * if they do not finish in a timely manner.
   *
   * @param executor the ExecutorService to shut down
   */
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

  /**
   * Initializes the output file by creating it and setting its length to the specified file size.
   * This method ensures that the output file is ready for writing downloaded pieces.
   *
   * @throws FileNotFoundException if the output file cannot be created
   * @throws IOException           if an I/O error occurs while initializing the output file
   */

  private void initializeOutputFile() throws FileNotFoundException, IOException {
    try (RandomAccessFile raf = new RandomAccessFile(outputFilePath.toFile(), "rw")) {
      raf.setLength(fileSize);
    }
  }

  /**
   * Submits download tasks for each peer session to the executor service. Each task will attempt to
   * download pieces from the corresponding peer session.
   *
   * @param executor the ExecutorService managing the download tasks
   */

  private void submitDownloadTasks(ExecutorService executor) {
    List<Future<?>> futures = new ArrayList<>();
    for (PeerSession peerSession : peerSessions) {
      // Download pieces concurrently for each peer session
      Future<?> future = executor.submit(() -> downloadPiecesForPeer(peerSession));
      futures.add(future);
    }
    // Store futures to check individual task completion
    this.downloadFutures = futures;
  }

  /**
   * While there are pieces to download, this method will attempt to download pieces using the
   * specified peer session. It will keep trying to download pieces until either the piece queue is
   * empty or the peer session is no longer in the downloading state.
   *
   * @param peerSession the PeerSession from which to download pieces
   */
  private void downloadPiecesForPeer(PeerSession peerSession) {
    while (!pieceQueue.isEmpty()) {
      // If peer session is already downloading another piece, skip to the next iteration
      if (peerSession.getSessionState().equals(PeerSession.SessionState.DOWNLOADING)) {
        continue;
      }

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
          int delay = (int) (Math.random() * 2000) + 1000; // Random delay between 1-6 seconds
          Thread.sleep(delay); // Sleep for 1 second before retrying
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt(); // Restore interrupted status
          System.err.println("Download interrupted while waiting to retry piece " + pieceIndex);
        }
      }
    }
  }


  /**
   * Downloads a single piece from the specified peer session. This method checks the state of the
   * piece, updates the state to DOWNLOADING, and attempts to download the piece data. If the piece
   * is successfully downloaded and its hash matches the expected hash, it writes the piece data to
   * the output file. If the piece data is invalid or the download fails, it throws an IOException
   * or PieceDownloadException.
   *
   * @param peerSession the PeerSession from which to download the piece
   * @param pieceIndex  the index of the piece to download
   * @throws IOException            if an I/O error occurs during the download process or if the
   *                                piece data is invalid
   * @throws PieceDownloadException if the downloaded piece data is invalid or does not match the
   *                                expected hash
   */
  private void downloadSinglePiece(PeerSession peerSession, Integer pieceIndex)
      throws IOException, PieceDownloadException {
    PieceState state = pieceStates.get(pieceIndex);

    // If the piece is already downloaded, skip it
    if (state == PieceState.NOT_DOWNLOADED) {
      pieceStates.put(pieceIndex, PieceState.DOWNLOADING);
      pieceDownloaders.put(pieceIndex, peerSession);

      byte[] pieceData = peerSession.downloadPiece(pieceIndex, pieceLength,
          pieceHashes.get(pieceIndex), fileSize);

      byte[] expectedHash = pieceHashes.get(pieceIndex);
      if (pieceData != null && Arrays.equals(TorrentFileHandler.sha1Hash(pieceData),
          expectedHash)) {
        writePieceToFile(outputFilePath.toString(), pieceData, pieceIndex * pieceLength);

        pieceStates.put(pieceIndex, PieceState.DOWNLOADED);
        pieceDownloaders.remove(pieceIndex);
        System.out.println(
            "Downloaded piece " + pieceIndex + " from " + peerSession.getIpAddress());
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

  /**
   * Closes all peer connections and clears the session state. This method is used to release
   * resources and ensure that all connections are properly closed after the download is complete.
   */
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
