import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

public class TrackerClientTests {

  @Test
  public void testTrackerClientInitialization() {
    String trackerUrl = "http://example.com:8080/announce";
    int port = 6881;
    String peerId = "12345678901234567890";
    int fileSize = 1024;
    byte[] infoHash = new byte[20];

    TrackerClient trackerClient = new TrackerClient(trackerUrl, port, fileSize, infoHash, peerId);
    assertNotNull(trackerClient);

    assertEquals(0, trackerClient.getUploaded());
    assertEquals(0, trackerClient.getDownloaded());
    assertEquals(fileSize, trackerClient.getLeft());
    assertEquals(6881, trackerClient.getPort());

    assertEquals(trackerUrl, trackerClient.getTrackerUrl());
    assertEquals(port, trackerClient.getPort());
    assertEquals(peerId, trackerClient.getPeerId());
    assertEquals(fileSize, trackerClient.getDownloadedFileSize());
    assertArrayEquals(infoHash, trackerClient.getInfoHash());
    assertEquals(1, trackerClient.getCompactMode());
  }

  @Test
  public void testTrackerClientBadInitialization() {
    // null values
    String trackerUrl = "http://example.com:8080/announce";
    int port = 6881;
    String peerId = "12345678901234567890";
    int fileSize = 1024;
    byte[] infoHash = new byte[20];

    //TrackerClient trackerClient = new TrackerClient(trackerUrl, port, fileSize, infoHash, peerId);
    Exception ex = assertThrows(IllegalArgumentException.class,
        () -> new TrackerClient(null, port, fileSize, infoHash, peerId));

    assertEquals("Tracker URL cannot be null or empty", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class,
        () -> new TrackerClient("", port, fileSize, infoHash, peerId));
    assertEquals("Tracker URL cannot be null or empty", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class,
        () -> new TrackerClient(trackerUrl, port, fileSize, infoHash, null));
    assertEquals("Peer ID cannot be null or empty", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class,
        () -> new TrackerClient(trackerUrl, port, fileSize, infoHash, ""));
    assertEquals("Peer ID cannot be null or empty", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class,
        () -> new TrackerClient(trackerUrl, -1, fileSize, infoHash, peerId));
    assertEquals("Port must be a positive integer", ex.getMessage());
    ex = assertThrows(IllegalArgumentException.class,
        () -> new TrackerClient(trackerUrl, port, -1, infoHash, peerId));

    assertEquals("File size must be a positive integer", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class,
        () -> new TrackerClient(trackerUrl, port, fileSize, new byte[0], peerId));
    assertEquals("Info hash must be 20 bytes long", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class,
        () -> new TrackerClient(trackerUrl, port, fileSize, infoHash, "sjfkldasjs"));
    assertEquals("Peer ID must be 20 bytes long", ex.getMessage());

  }

  @Test
  public void testBadURLEncodedHash() {
    // null hash
    Exception ex = assertThrows(IllegalArgumentException.class,
        () -> TrackerClient.urlEncodeHash(null));
    assertEquals("Hash cannot be null or empty", ex.getMessage());

    // empty hash
    ex = assertThrows(IllegalArgumentException.class,
        () -> TrackerClient.urlEncodeHash(new byte[0]));
    assertEquals("Hash cannot be null or empty", ex.getMessage());
  }

  @Test
  public void testURLEncodedHashNoUnreserved() {
    String output = "%00%01%02%03%04%05%06%07%08%09";
    int length = output.length();

    byte[] hash = new byte[10];
    for (int i = 0; i < hash.length; i++) {
      hash[i] = (byte) i;
    }
    String encodedHash = TrackerClient.urlEncodeHash(hash);
    assertEquals(output, encodedHash);
    assertEquals(length, encodedHash.length());
  }

  @Test
  public void testURLEncodedHashWithUnreserved() {
    String hex = "d69f91e6b2ae4c542468d1073a71d4ea13879a7f";
    // Convert hex to byte array
    byte[] hash = new byte[20];
    for (int i = 0; i < hash.length; i++) {
      hash[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
    }
    String shortenedOutput = "%D6%9F%91%E6%B2%AELT%24h%D1%07%3Aq%D4%EA%13%87%9A%7F";
    String encodedHash = TrackerClient.urlEncodeHash(hash);
    assertEquals(shortenedOutput, encodedHash);
    assertEquals(shortenedOutput.length(), encodedHash.length());
  }

  @Test
  public void testRequestTrackerInvalidRequest() {
    MockTorrentFileHandler tfh = new MockTorrentFileHandler();
    HttpClient fastFailClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(3))
        .build();

    TrackerClient invalidClient = new TrackerClient(
        "http://example.com:8080/announce",
        6881, tfh.getFileLength(),
        tfh.getInfoHash(), "12345678901234567890", fastFailClient
    );
    Exception ex = assertThrows(TrackerCommunicationException.class,
        invalidClient::requestTracker);
    assertTrue(ex.getMessage().contains("Failed to contact tracker"));

    String tracker404Url = tfh.getTrackerUrl() +  "/404";
    invalidClient = new TrackerClient(
        tracker404Url,
        6881, tfh.getFileLength(),
        tfh.getInfoHash(), "12345678901234567890", fastFailClient
    );
    ex = assertThrows(TrackerCommunicationException.class,
        invalidClient::requestTracker);
    assertTrue(ex.getMessage().contains("Tracker returned non-200 response: "));

  }

  @Test
  public void testRequestTrackerInvalidRequestMockClient()
      throws IOException, InterruptedException {
    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(404);

    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    MockTorrentFileHandler tfh = new MockTorrentFileHandler();

    TrackerClient invalidClient = new TrackerClient(
        tfh.getTrackerUrl(),
        6881, tfh.getFileLength(),
        tfh.getInfoHash(), "12345678901234567890", mockHttpClient
    );

    Exception ex = assertThrows(TrackerCommunicationException.class,
        invalidClient::requestTracker);
    assertTrue(ex.getMessage().contains("Tracker returned non-200 response: "));
  }

  @Test
  public void testRequestTrackerInvalidResponse()
      throws MalformedTrackerResponseException, TrackerCommunicationException {
    MockTorrentFileHandler tfh = new MockTorrentFileHandler();
    //TorrentFileHandler tfh = new TorrentFileHandler("sample.torrent");

    TrackerClient invalidClient = new TrackerClient(
        tfh.getTrackerUrl(),
        60000000, tfh.getFileLength(),
        tfh.getInfoHash(), "12345678901234567890");
    Exception ex = assertThrows(MalformedTrackerResponseException.class,
        invalidClient::requestTracker);
    assertTrue(ex.getMessage().contains("Missing 'peers' or 'interval' in tracker response"));

    byte[] wrongHash = new byte[20];
    for (int i = 0; i < wrongHash.length; i++) {
      wrongHash[i] = (byte) i;
    }
    invalidClient = new TrackerClient(
        tfh.getTrackerUrl(),
        6881, tfh.getFileLength(),
        wrongHash, "12345678901234567890");
    ex = assertThrows(MalformedTrackerResponseException.class,
        invalidClient::requestTracker);
    assertTrue(ex.getMessage().contains("Missing 'peers' or 'interval' in tracker response"));

  }

  @Test
  public void testRequestTrackerInvalidResponseMockClient()
      throws IOException, InterruptedException {
    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);

    byte[] fakeTrackerResponse = "d6:failure reason:Invalid request".getBytes();
    when(mockResponse.body()).thenReturn(fakeTrackerResponse);

    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    MockTorrentFileHandler tfh = new MockTorrentFileHandler();

    TrackerClient invalidClient = new TrackerClient(
        tfh.getTrackerUrl(),
        6881, tfh.getFileLength(),
        tfh.getInfoHash(), "12345678901234567890", mockHttpClient
    );

    Exception ex = assertThrows(IllegalArgumentException.class,
        invalidClient::requestTracker);
    assertTrue(ex.getMessage().contains("Failed to decode tracker response"));

    byte[] missingIntervalResponse = "d5:peers0:e".getBytes();
    when(mockResponse.body()).thenReturn(missingIntervalResponse);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);
    ex = assertThrows(MalformedTrackerResponseException.class,
        invalidClient::requestTracker);
    assertTrue(ex.getMessage().contains("Missing 'peers' or 'interval' in tracker response"));

    byte[] missingPeersResponse = "d8:intervali60ee".getBytes();
    when(mockResponse.body()).thenReturn(missingPeersResponse);
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);
    ex = assertThrows(MalformedTrackerResponseException.class,
        invalidClient::requestTracker);
    assertTrue(ex.getMessage().contains("Missing 'peers' or 'interval' in tracker response"));

  }

  @Test
  public void testRequestTrackerValidRequest()
      throws MalformedTrackerResponseException, TrackerCommunicationException {
    Map<String, Integer> expectedPeers = Map.of(
        "165.232.38.164", 51433,
        "165.232.41.73", 51430,
        "165.232.35.114", 51531
    );

    int expectedInterval = 60;

    MockTorrentFileHandler tfh = new MockTorrentFileHandler();
    //TorrentFileHandler tfh = new TorrentFileHandler("sample.torrent");

    TrackerClient trc = new TrackerClient(
        tfh.getTrackerUrl(),
        6881, tfh.getFileLength(),
        tfh.getInfoHash(), "12345678901234567890");


    TrackerResponse response = trc.requestTracker();
    assertNotNull(response);
    assertEquals(expectedInterval, response.getInterval());
    assertNotNull(response.getPeers());
    assertEquals(expectedPeers.size(), response.getPeersMap().size());
    for (Map.Entry<String, Integer> entry : expectedPeers.entrySet()) {
      assertTrue(response.getPeersMap().containsKey(entry.getKey()));
      assertEquals(entry.getValue(), response.getPeersMap().get(entry.getKey()));
    }

    assertNotNull(response.getPeersMap());
    assertNotNull(response.getPeers());
    assertTrue(response.getPeers().length > 0);
    assertTrue(response.getPeersMap().size() > 0);
  }

  @Test
  public void testRequestTrackerValidRequestMockClient()
      throws IOException, InterruptedException, MalformedTrackerResponseException, TrackerCommunicationException {
    Map<String, Integer> expectedPeers = Map.of(
        "165.232.38.164", 51433
    );

    HttpClient mockHttpClient = mock(HttpClient.class);
    HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);

    byte[] fakeTrackerResponse = "d8:intervali60e5:peers6:".getBytes();
    byte[] peerAddress = new byte[] {
        -91, -24, 38, -92, -56, -23
    };
    byte[] ending = "e".getBytes();

    byte[] fakeTrackerResponseFull = new byte[fakeTrackerResponse.length + peerAddress.length + ending.length];
    System.arraycopy(fakeTrackerResponse, 0, fakeTrackerResponseFull, 0, fakeTrackerResponse.length);
    System.arraycopy(peerAddress, 0, fakeTrackerResponseFull, fakeTrackerResponse.length, peerAddress.length);
    System.arraycopy(ending, 0, fakeTrackerResponseFull, fakeTrackerResponse.length + peerAddress.length, ending.length);

    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn(fakeTrackerResponseFull);

    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    MockTorrentFileHandler tfh = new MockTorrentFileHandler();

    TrackerClient trc = new TrackerClient(
        tfh.getTrackerUrl(),
        6881, tfh.getFileLength(),
        tfh.getInfoHash(), "12345678901234567890", mockHttpClient
    );

    TrackerResponse response = trc.requestTracker();
    assertNotNull(response);
    assertEquals(60, response.getInterval());
    assertNotNull(response.getPeers());
    assertTrue(response.getPeers().length > 0);

    assertEquals(expectedPeers.size(), response.getPeersMap().size());
    for (Map.Entry<String, Integer> entry : expectedPeers.entrySet()) {
      assertTrue(response.getPeersMap().containsKey(entry.getKey()));
      assertEquals(entry.getValue(), response.getPeersMap().get(entry.getKey()));
    }
  }


}
