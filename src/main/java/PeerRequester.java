import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PeerRequester {
	public static final Set<Byte> UNRESERVED = new HashSet<>();

	static {
		// Populate unreserved byte set
		for (byte b = 'A'; b <= 'Z'; b++) UNRESERVED.add(b);
		for (byte b = 'a'; b <= 'z'; b++) UNRESERVED.add(b);
		for (byte b = '0'; b <= '9'; b++) UNRESERVED.add(b);
		UNRESERVED.add((byte) '-');
		UNRESERVED.add((byte) '_');
		UNRESERVED.add((byte) '.');
		UNRESERVED.add((byte) '~');
	}

	private final HttpClient client;

	private final String peerIp;
	private final byte[] infoHash;
	private final String peerId;
	private final int port;
	private int uploaded;
	private int downloaded;
	private int left;
	private final int compactMode;

	public PeerRequester(String peerIp, int port, int fileSize, byte[] infoHash) {
		this.peerIp = peerIp;
		this.infoHash = infoHash;
		this.peerId = randomPeerId();
		this.port = port;
		this.uploaded = 0;
		this.downloaded = 0;
		this.left = fileSize;
		this.compactMode = 1;

		this.client = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.build();
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

	private String buildTrackerUrl() {
		StringBuilder url = new StringBuilder(peerIp);
		//url.append("?info_hash=").append(URLEncoder.encode(new String(infoHash, StandardCharsets.ISO_8859_1), "ISO-8859-1"));
		url.append("?info_hash=").append(urlEncodeHash(infoHash));
		url.append("&peer_id=").append(peerId);
		url.append("&port=6881");
		url.append("&uploaded=0");
		url.append("&downloaded=0");
		url.append("&left=").append(left);
		url.append("&compact=1"); // compact = 1 means peer list is in binary format
		return url.toString();
	}

	private String urlEncodeHash(byte[] hash) {
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

	public TrackerResponse requestTracker() {
		String trackerUrl = buildTrackerUrl();
		System.out.println("Requesting tracker: " + trackerUrl);

		// Send HTTP GET request to the tracker
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(trackerUrl))
				.GET()
				.build();
		try {
			HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

			DecoderDispatcher decoderDispatcher = new DecoderDispatcher();
			DictionaryDecoder dictionaryDecoder = new DictionaryDecoder(decoderDispatcher);
			TorrentInfoDTO torrentInfoDTO = new TorrentInfoDTO();
			DecoderDTO<Map<String, Object>> decoded = dictionaryDecoder.decode(response.body(), 0, torrentInfoDTO);
			Map<String, Object> decodedResponse = decoded.getValue();

			NumberPair peersByteRange = torrentInfoDTO.getByteRange("peers");
			byte[] peersArray = Arrays.copyOfRange(response.body(), peersByteRange.first(), peersByteRange.second());
			int interval = (int) decodedResponse.get("interval");
			return new TrackerResponse(interval, peersArray);

		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}


	}
}
