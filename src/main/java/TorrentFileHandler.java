import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TorrentFileHandler {
	private final String fileName;
	private int fileLength;
	private String trackerUrl;

	private byte[] fileContent;
	private Map<String, Object> fileContentMap;
	private Map<String, Object> infoMap;
	private byte[] fileHash;

	private int fileHashLength = 20; // SHA-1 hash length in bytes
	private int pieceLength;
	private List<byte[]> hashedPieces;

	public TorrentFileHandler(String fileName) {
		this.fileName = fileName;
		this.hashedPieces = new ArrayList<>();
		readFile();
	}

	public String getFileName() {
		return fileName;
	}

	public int getFileLength() {
		return fileLength;
	}

	public String getTrackerUrl() {
		return trackerUrl;
	}

	public byte[] getFileContent() {
		return fileContent;
	}

	public Map<String, Object> getFileContentMap() {
		return fileContentMap;
	}

	public byte[] getFileHash() {
		return fileHash;
	}

	public int getPieceLength() {
		return pieceLength;
	}

	public List<byte[]> getHashedPieces() {
		return hashedPieces;
	}

	public Map<String, Object> getInfoMap() {
		return infoMap;
	}

	private void readFile() throws RuntimeException {
		try {
			fileContent = Files.readAllBytes(Paths.get(fileName));

			DecoderDispatcher decoderDispatcher = new DecoderDispatcher();
			TorrentInfoDTO torrentInfoDTO = new TorrentInfoDTO();
			DecoderDTO<?> decoded = decoderDispatcher.decode(fileContent, 0, torrentInfoDTO);

			fileContentMap = (Map<String, Object>) decoded.getValue();
			infoMap = (Map<String, Object>) fileContentMap.get("info");
			fileLength = (int) infoMap.get("length"); 
			trackerUrl = (String) fileContentMap.get("announce");
			pieceLength = (int) infoMap.get("piece length");

			// Calculate the SHA-1 hash of the info dictionary
			NumberPair infoByteRange = torrentInfoDTO.getInfoByteRange();
			byte[] infoBytes = Arrays.copyOfRange(fileContent, infoByteRange.first(), infoByteRange.second());
			fileHash = sha1Hash(infoBytes);

			// Calculate the SHA-1 hash of each piece
			NumberPair pieceByteRange = torrentInfoDTO.getByteRange("pieces");
			for (int i = pieceByteRange.first(); i < pieceByteRange.second(); i += fileHashLength) {
				byte[] piece = Arrays.copyOfRange(fileContent, i, i + fileHashLength);
				hashedPieces.add(piece);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] sha1Hash(byte[] data) {
		try {
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
			return digest.digest(data);
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
