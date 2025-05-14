import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class TorrentFileHandler {
	private final String fileName;
	private int fileLength;
	private String trackerUrl;

	private byte[] fileContent;
	private Map<String, Object> fileContentMap;
	private byte[] fileHash;

	public TorrentFileHandler(String fileName) {
		this.fileName = fileName;
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

	private void readFile() throws RuntimeException {
		try {
			fileContent = Files.readAllBytes(Paths.get(fileName));

			DecoderDispatcher decoderDispatcher = new DecoderDispatcher();
			TorrentInfoDTO torrentInfoDTO = new TorrentInfoDTO();
			DecoderDTO<?> decoded = decoderDispatcher.decode(fileContent, 0, torrentInfoDTO);
			fileContentMap = (Map<String, Object>) decoded.getValue();
			Map<String, Object> infoMap = (Map<String, Object>) fileContentMap.get("info");
			fileLength = (int) infoMap.get("length"); 
			trackerUrl = (String) fileContentMap.get("announce");

			// Calculate the SHA-1 hash of the info dictionary
			byte[] infoBytes = Arrays.copyOfRange(fileContent, torrentInfoDTO.getStartIndex(), torrentInfoDTO.getEndIndex());
			fileHash = sha1Hash(infoBytes);

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
