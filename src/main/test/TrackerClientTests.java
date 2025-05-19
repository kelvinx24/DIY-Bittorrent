import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    Exception ex = assertThrows(IllegalArgumentException.class, () -> {
      new TrackerClient(null, port, fileSize, infoHash, peerId);
    });

    assertEquals("Tracker URL cannot be null or empty", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () -> {
      new TrackerClient("", port, fileSize, infoHash, peerId);
    });
    assertEquals("Tracker URL cannot be null or empty", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () -> {
      new TrackerClient(trackerUrl, port, fileSize, infoHash, null);
    });
    assertEquals("Peer ID cannot be null or empty", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () -> {
      new TrackerClient(trackerUrl, port, fileSize, infoHash, "");
    });
    assertEquals("Peer ID cannot be null or empty", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () -> {
      new TrackerClient(trackerUrl, -1, fileSize, infoHash, peerId);
    });
    assertEquals("Port must be a positive integer", ex.getMessage());
    ex = assertThrows(IllegalArgumentException.class, () -> {
      new TrackerClient(trackerUrl, port, -1, infoHash, peerId);
    });

    assertEquals("File size must be a positive integer", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () -> {
      new TrackerClient(trackerUrl, port, fileSize, new byte[0], peerId);
    });
    assertEquals("Info hash must be 20 bytes long", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () -> {
      new TrackerClient(trackerUrl, port, fileSize, infoHash, "sjfkldasjs");
    });
    assertEquals("Peer ID must be 20 bytes long", ex.getMessage());

  }

  @Test
  public void testBadURLEncodedHash() {
    // null hash
    Exception ex = assertThrows(IllegalArgumentException.class, () -> {
      TrackerClient.urlEncodeHash(null);
    });
    assertEquals("Hash cannot be null or empty", ex.getMessage());

    // empty hash
    ex = assertThrows(IllegalArgumentException.class, () -> {
      TrackerClient.urlEncodeHash(new byte[0]);
    });
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
  public void testRequestTracker() {

  }


}
