import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A client for interacting with a BitTorrent tracker. This class handles sending requests to the
 * tracker and parsing the responses.
 *
 * @author KX
 */
public class TrackerClient {

  public static final Set<Byte> UNRESERVED = new HashSet<>();
  private static final String PEERS_KEY = "peers";
  private static final String INTERVAL_KEY = "interval";

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
  private final int compactMode;

  private final HttpClient client;

  /**
   * Constructs a TrackerClient with default parameters. Uses a HTTP client with a 20-second
   * timeout.
   *
   * @param trackerUrl         the URL of the tracker
   * @param port               the port to connect to
   * @param downloadedFileSize the size of the downloaded file in bytes
   * @param infoHash           the info hash of the torrent file, must be 20 bytes long
   * @param peerId             the peer ID, must be 20 bytes long
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public TrackerClient(String trackerUrl, int port, int downloadedFileSize, byte[] infoHash,
      String peerId) throws IllegalArgumentException {
    this(trackerUrl, port, downloadedFileSize, infoHash, peerId,
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build());
  }

  /**
   * Constructs a TrackerClient with specified parameters. Uses the provided HttpClient instance.
   *
   * @param trackerUrl         the URL of the tracker
   * @param port               the port to connect to
   * @param downloadedFileSize the size of the downloaded file in bytes
   * @param infoHash           the info hash of the torrent file, must be 20 bytes long
   * @param peerId             the peer ID, must be 20 bytes long
   * @param client             an HttpClient instance to use for requests
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public TrackerClient(String trackerUrl, int port, int downloadedFileSize, byte[] infoHash,
      String peerId, HttpClient client) throws IllegalArgumentException {
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

    this.client = client;
  }

  /**
   * Requests the tracker for peer information.
   *
   * @return {@link TrackerResponse} containing the interval and peer list in binary format
   * @throws TrackerCommunicationException     if there is an error communicating with the tracker
   * @throws IllegalArgumentException          if any parameter is invalid
   * @throws MalformedTrackerResponseException if the tracker response is malformed or missing
   *                                           required fields
   */
  public TrackerResponse requestTracker()
      throws TrackerCommunicationException, IllegalArgumentException, MalformedTrackerResponseException {
    String trackerUrl = buildTrackerUrl();
    System.out.println("Requesting tracker: " + trackerUrl);

    try {
      HttpResponse<byte[]> response = sendTrackerRequest(trackerUrl);
      return parseTrackerResponse(response.body());
    } catch (IOException | InterruptedException e) {
      throw new TrackerCommunicationException("Failed to contact tracker" + e.getMessage());
    }
  }

  private HttpResponse<byte[]> sendTrackerRequest(String trackerUrl)
      throws IOException, InterruptedException, TrackerCommunicationException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(trackerUrl))
        .GET()
        .build();

    HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() != 200) {
      throw new TrackerCommunicationException(
          "Tracker returned non-200 response: " + response.statusCode());
    }
    return response;
  }

  /**
   * Parses the tracker response and extracts the interval and peer list. Using
   * {@link DecoderDispatcher}
   *
   * @param responseBody the raw byte array of the tracker response
   * @return a TrackerResponse containing the interval and peer list in binary format
   * @throws IllegalArgumentException          if the response cannot be decoded
   * @throws MalformedTrackerResponseException if the response is missing required fields
   */
  private TrackerResponse parseTrackerResponse(byte[] responseBody)
      throws IllegalArgumentException, MalformedTrackerResponseException {
    try {
      DecoderDispatcher decoderDispatcher = new DecoderDispatcher();
      DictionaryDecoder dictionaryDecoder = new DictionaryDecoder(decoderDispatcher);
      DecoderByteDTO<Map<String, Object>> decoded = dictionaryDecoder.decode(responseBody, 0);
      Map<String, Object> decodedResponse = decoded.getValue();

      // Validate required fields
      if (!decodedResponse.containsKey(PEERS_KEY) || !decodedResponse.containsKey(INTERVAL_KEY)) {
        StringBuilder exceptionMessage = new StringBuilder(
            "Missing 'peers' or 'interval' in tracker response");
        exceptionMessage.append("\n").append("Current Response: ").append(decodedResponse);
        throw new MalformedTrackerResponseException(exceptionMessage.toString());
      }

      // Extract peers
      NumberPair peersByteRange = decoded.getByteRange(PEERS_KEY);
      int start = peersByteRange.first();
      int endExclusive = peersByteRange.second() + 1; // ensure contract is correct
      byte[] peersArray = Arrays.copyOfRange(responseBody, start, endExclusive);

      // Extract interval
      int interval = TorrentFileHandler.extractInt(decodedResponse, INTERVAL_KEY);

      return new TrackerResponse(interval, peersArray);
    } catch (MalformedTrackerResponseException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to decode tracker response: "
          + e.getMessage() + "\n" + "Response: " + Arrays.toString(responseBody), e);
    }
  }

  private String buildTrackerUrl() {
    StringBuilder url = new StringBuilder(trackerUrl);
    //url.append("?info_hash=").append(URLEncoder.encode(new String(infoHash, StandardCharsets.ISO_8859_1), "ISO-8859-1"));
    url.append("?info_hash=").append(urlEncodeHash(infoHash));
    url.append("&peer_id=").append(peerId);
    url.append("&port=").append(port);
    url.append("&uploaded=").append(uploaded);
    url.append("&downloaded=").append(downloaded);
    url.append("&left=").append(left);
    url.append("&compact=").append(compactMode); // compact = 1 means peer list is in binary format
    return url.toString();
  }

  /**
   * URL-encodes a byte array representing a hash. This method encodes each byte in the hash to a
   * percent-encoded format if it is not an unreserved character.
   *
   * @param hash the byte array to encode, must not be null or empty
   * @return a URL-encoded string representation of the hash
   * @throws IllegalArgumentException if the hash is null or empty
   */
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
