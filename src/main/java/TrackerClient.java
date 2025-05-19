import java.net.http.HttpClient;
import java.util.HashSet;
import java.util.Set;

public class TrackerClient {

  public static final Set<Byte> UNRESERVED = new HashSet<>();

  static {
    // Populate unreserved byte set
    for (byte b = 'A'; b <= 'Z'; b++) {
      UNRESERVED.add(b);
    }
    for (byte b = 'a'; b <= 'z'; b++) {
      UNRESERVED.add(b);
    }
    for (byte b = '0'; b <= '9'; b++) {
      UNRESERVED.add(b);
    }
    UNRESERVED.add((byte) '-');
    UNRESERVED.add((byte) '_');
    UNRESERVED.add((byte) '.');
    UNRESERVED.add((byte) '~');
  }

  private final String trackerUrl;
  private final int port;
  private final int downloadedFileSize;
  private final byte[] infoHash;
  private final String peerId;

  private int uploaded;
  private int downloaded;
  private int left;
  private int compactMode;

  private HttpClient client;

  public TrackerClient(String trackerUrl, int port, int downloadedFileSize, byte[] infoHash,
      String peerId) throws IllegalArgumentException {
    if (trackerUrl == null || trackerUrl.isEmpty()) {
      throw new IllegalArgumentException("Tracker URL cannot be null or empty");
    }

    if (peerId == null || peerId.isEmpty()) {
      throw new IllegalArgumentException("Peer ID cannot be null or empty");
    }

    if (infoHash == null || infoHash.length != 20) {
      throw new IllegalArgumentException("Info hash must be 20 bytes long");
    }

    if (peerId.length() != 20) {
      throw new IllegalArgumentException("Peer ID must be 20 bytes long");
    }

    if (port <= 0) {
      throw new IllegalArgumentException("Port must be a positive integer");
    }

    if (downloadedFileSize <= 0) {
      throw new IllegalArgumentException("File size must be a positive integer");
    }

    this.trackerUrl = trackerUrl;
    this.port = port;
    this.downloadedFileSize = downloadedFileSize;
    this.infoHash = infoHash;
    this.peerId = peerId;
    this.uploaded = 0;
    this.downloaded = 0;
    this.left = downloadedFileSize;
    this.compactMode = 1; // Assuming compact mode is enabled
  }

  public TrackerResponse requestTracker() {
    return null;
  }

  public static String urlEncodeHash(byte[] hash) {
    if (hash == null || hash.length == 0) {
      throw new IllegalArgumentException("Hash cannot be null or empty");
    }

    StringBuilder encoded = new StringBuilder();
    for (byte b : hash) {
      if (UNRESERVED.contains(b)) {
        encoded.append((char) b);
      } else {
        encoded.append('%').append(String.format("%02X", b));
      }
    }

    return encoded.toString();
  }

  // Getters
  public String getTrackerUrl() {
    return trackerUrl;
  }

  public int getPort() {
    return port;
  }

  public int getDownloadedFileSize() {
    return downloadedFileSize;
  }

  public byte[] getInfoHash() {
    return infoHash;
  }

  public String getPeerId() {
    return peerId;
  }

  public int getUploaded() {
    return uploaded;
  }

  public int getDownloaded() {
    return downloaded;
  }

  public int getLeft() {
    return left;
  }

  public int getCompactMode() {
    return compactMode;
  }

}
