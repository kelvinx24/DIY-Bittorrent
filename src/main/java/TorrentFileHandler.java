import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Handles the parsing and management of .torrent files. This class reads a torrent file, extracts
 * its metadata, and stores it within its fields.
 *
 * @author KX
 */
public class TorrentFileHandler {

  private static final int FILE_HASH_LENGTH = 20; // SHA-1

  private final Path torrentFilePath;
  private byte[] fileContent;
  private Map<String, Object> fileContentMap;
  private Map<String, Object> infoMap;

  private byte[] infoHash;
  private int fileLength;
  private String trackerUrl;
  private int pieceLength;
  private final List<byte[]> hashedPieces = new ArrayList<>();

  /**
   * Constructs a TorrentFileHandler for the specified torrent file.
   *
   * @param fileName the name of the torrent file to parse
   * @throws NullPointerException     if fileName is null
   * @throws IllegalArgumentException if fileName is empty, does not end with .torrent, or does not
   *                                  exist
   */
  public TorrentFileHandler(String fileName) {
    Objects.requireNonNull(fileName, "File name cannot be null");
    if (fileName.isEmpty()) {
      throw new IllegalArgumentException("File name cannot be empty");
    }
    if (!fileName.endsWith(".torrent")) {
      throw new IllegalArgumentException("File must have a .torrent extension");
    }

    this.torrentFilePath = Path.of(fileName);
    if (!Files.exists(torrentFilePath)) {
      throw new IllegalArgumentException("File does not exist: " + fileName);
    }

    loadAndParseTorrentFile();
  }

  /**
   * Parses through the torrent file and extracts its metadata with {@link Decoder}s.
   */
  private void loadAndParseTorrentFile() {
    try {
      this.fileContent = Files.readAllBytes(torrentFilePath);

      DecoderDispatcher decoderDispatcher = new DecoderDispatcher();
      DecoderByteDTO<?> decoded = decoderDispatcher.decode(fileContent, 0);

      // Extracts the top-level bencoded map and the 'info' dictionary
      this.fileContentMap = safeCastMap(decoded.getDecoderDTO().getValue(),
          "top-level bencoded map");
      this.infoMap = safeCastMap(fileContentMap.get("info"), "'info' dictionary");

      // Extracts the tracker URL, piece length, and file length from the maps
      this.trackerUrl = extractString(fileContentMap, "announce");
      this.pieceLength = extractInt(infoMap, "piece length");
      this.fileLength = extractInt(infoMap, "length");

      computeInfoHash(decoded);
      extractPieceHashes(decoded);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read torrent file", e);
    }
  }

  /**
   * Computes the SHA-1 hash of the 'info' dictionary in the torrent file. Uses
   * the byte range found in the {@link DictionaryDecoder} to extract the bytes for 'info'.
   *
   * @param dto the decoded DTO containing the byte range for 'info' which comes from the torrent
   *            file
   */
  private void computeInfoHash(DecoderByteDTO<?> dto) {
    NumberPair range = dto.getInfoByteRange();
    byte[] infoBytes = Arrays.copyOfRange(fileContent, range.first(), range.second());
    this.infoHash = sha1Hash(infoBytes);
  }

  /**
   * Extracts the piece hashes from the torrent file. The pieces are expected to be in a specific
   * byte range defined in the DTO.
   *
   * @param dto the decoded DTO containing the byte range for 'pieces'
   */
  private void extractPieceHashes(DecoderByteDTO<?> dto) {
    NumberPair range = dto.getByteRange("pieces");
    int adjustedEnd = range.second() + 1;
    for (int i = range.first(); i < adjustedEnd; i += FILE_HASH_LENGTH) {
      if (i + FILE_HASH_LENGTH > adjustedEnd) {
        throw new IllegalArgumentException("Invalid pieces field: not a multiple of 20 bytes");
      }
      hashedPieces.add(Arrays.copyOfRange(fileContent, i, i + FILE_HASH_LENGTH));
    }
  }

  /**
   * Safely casts an object to a Map<String, Object>. Throws an exception if the object is not a
   * Map.
   *
   * @param obj     the object to cast
   * @param context a description of the context for better error messages
   * @return the casted map
   * @throws IllegalArgumentException if obj is not a Map
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> safeCastMap(Object obj, String context) {
    if (!(obj instanceof Map)) {
      throw new IllegalArgumentException("Expected a Map for " + context);
    }
    return (Map<String, Object>) obj;
  }

  /**
   * Extracts a string value from a map for the given key. Throws an exception if the value is not a
   * string.
   *
   * @param map the map to extract from
   * @param key the key to look for
   * @return the string value associated with the key
   * @throws IllegalArgumentException if the value is not a string
   */
  public static String extractString(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (!(value instanceof String)) {
      throw new IllegalArgumentException("Expected a string for key: " + key);
    }
    return (String) value;
  }

  /**
   * Extracts an integer value from a map for the given key. Throws an exception if the value is not
   * an integer.
   * @param map the map to extract from
   * @param key the key to look for
   * @return the integer value associated with the key
   */
  public static int extractInt(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (!(value instanceof Integer)) {
      throw new IllegalArgumentException("Expected an integer for key: " + key);
    }
    return (Integer) value;
  }

  /**
   * Computes the SHA-1 hash of the given byte array.
   *
   * @param data the byte array to hash
   * @return the SHA-1 hash as a byte array
   */
  public static byte[] sha1Hash(byte[] data) {
    try {
      return java.security.MessageDigest.getInstance("SHA-1").digest(data);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-1 algorithm not available", e);
    }
  }

  /**
   * Converts a byte array to a hexadecimal string representation.
   * This is useful for displaying the info hash or piece hashes in a human-readable format.
   * @param bytes the byte array to convert
   * @return the hexadecimal string representation of the byte array
   */
  public static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  // Getters
  public String getFileName() {
    return torrentFilePath.toString();
  }

  public byte[] getFileContent() {
    return fileContent;
  }

  public Map<String, Object> getFileContentMap() {
    return fileContentMap;
  }

  public Map<String, Object> getInfoMap() {
    return infoMap;
  }

  public byte[] getInfoHash() {
    return infoHash;
  }

  public String getTrackerUrl() {
    return trackerUrl;
  }

  public int getFileLength() {
    return fileLength;
  }

  public int getPieceLength() {
    return pieceLength;
  }

  public List<byte[]> getHashedPieces() {
    return Collections.unmodifiableList(hashedPieces);
  }
}
