import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TorrentFileHandler {
	private static final int FILE_HASH_LENGTH = 20; // SHA-1

	private final Path torrentFilePath;
	private byte[] fileContent;
	private Map<String, Object> fileContentMap;
	private Map<String, Object> infoMap;

	private byte[] fileHash;
	private int fileLength;
	private String trackerUrl;
	private int pieceLength;
	private List<byte[]> hashedPieces = new ArrayList<>();

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

	private void loadAndParseTorrentFile() {
		try {
			this.fileContent = Files.readAllBytes(torrentFilePath);

			DecoderDispatcher decoderDispatcher = new DecoderDispatcher();
			DecoderByteDTO<?> decoded = decoderDispatcher.decode(fileContent, 0);

			this.fileContentMap = safeCastMap(decoded.getDecoderDTO().getValue(), "top-level bencoded map");
			this.infoMap = safeCastMap(fileContentMap.get("info"), "'info' dictionary");

			this.trackerUrl = extractString(fileContentMap, "announce");
			this.pieceLength = extractInt(infoMap, "piece length");
			this.fileLength = extractInt(infoMap, "length");

			computeInfoHash(decoded);
			extractPieceHashes(decoded);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read torrent file", e);
		}
	}

	private void computeInfoHash(DecoderByteDTO<?> dto) {
		NumberPair range = dto.getInfoByteRange();
		byte[] infoBytes = Arrays.copyOfRange(fileContent, range.first(), range.second());
		this.fileHash = sha1Hash(infoBytes);
	}

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

	@SuppressWarnings("unchecked")
	private static Map<String, Object> safeCastMap(Object obj, String context) {
		if (!(obj instanceof Map)) {
			throw new IllegalArgumentException("Expected a Map for " + context);
		}
		return (Map<String, Object>) obj;
	}

	private static String extractString(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (!(value instanceof String)) {
			throw new IllegalArgumentException("Expected a string for key: " + key);
		}
		return (String) value;
	}

	private static int extractInt(Map<String, Object> map, String key) {
		Object value = map.get(key);
		if (!(value instanceof Integer)) {
			throw new IllegalArgumentException("Expected an integer for key: " + key);
		}
		return (Integer) value;
	}

	public static byte[] sha1Hash(byte[] data) {
		try {
			return java.security.MessageDigest.getInstance("SHA-1").digest(data);
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new RuntimeException("SHA-1 algorithm not available", e);
		}
	}

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

	public byte[] getFileHash() {
		return fileHash;
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
