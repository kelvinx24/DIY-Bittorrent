import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
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
import java.util.concurrent.LinkedBlockingDeque;

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
  private final ExecutorService executor;

  public TorrentSession(
      TorrentFileHandler tfh,
      Path outputFilePath,
      TrackerClientFactory trackerClientFactory,
      PeerSessionFactory peerSessionFactory,
      PieceWriter pieceWriter,
      RandomIdGenerator idGenerator,
      ExecutorService executor) {

    if (tfh == null || outputFilePath == null || trackerClientFactory == null
        || peerSessionFactory == null || pieceWriter == null || idGenerator == null
        || executor == null) {
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

  private String randomPeerId() {
    int peerIdLength = 20;
    StringBuilder peerIdBuilder = new StringBuilder(peerIdLength);
    for (int i = 0; i < peerIdLength; i++) {
      char randomChar = (char) ('a' + Math.random() * 26);
      peerIdBuilder.append(randomChar);
    }
    return peerIdBuilder.toString();
  }

  public List<PeerSession> findRemotePeers() {
    List<PeerSession> unconnectedPeers = new ArrayList<>();

    try {
      TrackerResponse tr = trackerClient.requestTracker();
      for (Entry<String, Integer> entry : tr.getPeersMap().entrySet()) {
        String ip = entry.getKey();
        int port = entry.getValue();
        PeerSession peerSession = new PeerSession(ip, port, peerId, trackerClient.getInfoHash());
        unconnectedPeers.add(peerSession);
      }

      return unconnectedPeers; // Return the list of unconnected peers
    } catch (Exception e) {
      return unconnectedPeers; // Return empty list if tracker request fails
    }
  }

  public byte[] downloadPiece(int pieceIndex) {
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
          try {
            byte[] pieceData = peer.downloadPiece(pieceIndex, pieceLength,
                pieceHashes.get(pieceIndex), fileSize);
            if (pieceData != null && pieceData.length == pieceLength) {
              System.out.println("Downloaded piece " + pieceIndex + " from " + peer.getIpAddress());
              return pieceData;
            } else {
              throw new IOException("Invalid piece data received");
            }
          } catch (Exception e) {
            System.err.println(
                "Failed to download piece " + pieceIndex + " from " + peer.getIpAddress() + ": "
                    + e.getMessage());
          }
        }
      }
    }

  }

  public void downloadAll() throws FileNotFoundException, IOException {
    initializePeerSessions();
    if (peerSessions.isEmpty()) {
      throw new IllegalStateException("No peers available for download");
    }

    for (int i = 0; i < numPieces; i++) {
      pieceStates.put(i, PieceState.NOT_DOWNLOADED);
      pieceQueue.add(i);
    }

    try (RandomAccessFile raf = new RandomAccessFile(outputFilePath.toFile(), "rw")) {
      raf.setLength(fileSize);
    }

    ExecutorService executor = Executors.newFixedThreadPool(peerSessions.size());
    for (PeerSession peerSession : peerSessions) {
      executor.submit(() -> {
        while (!pieceQueue.isEmpty() && !peerSession.getSessionState()
            .equals(PeerSession.SessionState.DOWNLOADING)) {
          Integer pieceIndex = pieceQueue.poll();
          if (pieceIndex == null) {
            break;
          }

          try {
            PieceState state = pieceStates.get(pieceIndex);
            if (state == PieceState.NOT_DOWNLOADED) {
              pieceStates.put(pieceIndex, PieceState.DOWNLOADING);
              pieceDownloaders.put(pieceIndex, peerSession);
              byte[] pieceData = peerSession.downloadPiece(pieceIndex, pieceLength,
                  pieceHashes.get(pieceIndex), fileSize);

              if (pieceData != null && pieceData.length == pieceLength) {
                writePieceToFile(outputFilePath.toString(), pieceData, pieceIndex * pieceLength);
                pieceStates.put(pieceIndex, PieceState.DOWNLOADED);
                pieceDownloaders.remove(pieceIndex);
                System.out.println(
                    "Downloaded piece " + pieceIndex + " from " + peerSession.getIpAddress());
              } else {
                throw new IOException("Invalid piece data");
              }
            }
          } catch (Exception e) {
            System.err.println(
                "Exception downloading piece " + pieceIndex + " from " + peerSession.getIpAddress()
                    + ": " + e.getMessage());
            pieceDownloaders.remove(pieceIndex);
            pieceStates.put(pieceIndex, PieceState.NOT_DOWNLOADED);
            pieceQueue.add(pieceIndex); // Re-add the piece to the queue for retry
          }
        }
      });
    }


  }

  private static void writePieceToFile(String filePath, byte[] data, int offset)
      throws IOException {
    try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw")) {
      raf.seek(offset);
      raf.write(data);
    }
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

}
