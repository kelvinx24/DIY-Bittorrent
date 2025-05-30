package model;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import model.decoder.DecoderByteDTO;
import model.decoder.DecoderDispatcher;
import model.decoder.DictionaryDecoder;
import model.decoder.NumberPair;
import model.session.TorrentFileHandler;
import model.session.TrackerResponse;

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

	private Socket peerSocket;
	private OutputStream outputStream;
	private InputStream inputStream;
	private boolean firstTimeHandshake;

	private record PeerMessage(int id, byte[] payload) {}

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

		this.firstTimeHandshake = true;
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
			DecoderByteDTO<Map<String, Object>> decoded = dictionaryDecoder.decode(response.body(), 0);
			Map<String, Object> decodedResponse = decoded.getValue();

			NumberPair peersByteRange = decoded.getByteRange("peers");
			byte[] peersArray = Arrays.copyOfRange(response.body(), peersByteRange.first(), peersByteRange.second() + 1);
			int interval = (int) decodedResponse.get("interval");

			System.out.println("Interval: " + interval);
			return new TrackerResponse(interval, peersArray);

		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}


	}

	public byte[] peerHandshake(String ipAddr, int port) throws IOException{
		peerSocket = new Socket();
		peerSocket.connect(new InetSocketAddress(ipAddr, port), 5000);

		// Will automatically close the streams when done
			this.outputStream = peerSocket.getOutputStream();
			this.inputStream = peerSocket.getInputStream();
			byte[] handshake = buildHandshake();
			outputStream.write(handshake);
			outputStream.flush();

			byte[] response = new byte[68];
			int bytesRead = inputStream.read(response);
			if (bytesRead == -1) {
				throw new IOException("No response from peer");
			}


			// Check if the response is valid
			String protocol = new String(response, 1, 19);
			if (!"BitTorrent protocol".equals(protocol)) {
				throw new IOException("Invalid response from peer");
			}
			byte[] infoHash = Arrays.copyOfRange(response, 28, 48);
			byte[] peerId = Arrays.copyOfRange(response, 48, 68);
			if (!Arrays.equals(this.infoHash, infoHash)) {
				throw new IOException("Info hash mismatch");
			}



			// Return the peer ID
			return peerId;
	}

	public void closeConnection() {
		try {
			if (outputStream != null) {
				outputStream.close();
			}
			if (inputStream != null) {
				inputStream.close();
			}
			if (peerSocket != null) {
				peerSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private byte[] buildHandshake() {
		int protocolLength = 19;
		String protocol = "BitTorrent protocol";
		byte[] reserved = new byte[8];

		// Used to create a byte array (like StringBuilder is for string)
		ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();

		try {
			byteArrayStream.write(protocolLength);
			byteArrayStream.write(protocol.getBytes());
			byteArrayStream.write(reserved);
			byteArrayStream.write(infoHash);
			byteArrayStream.write(peerId.getBytes());

			return byteArrayStream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void establishConnection() throws IOException{
		int bitfieldID = 5;
		int unchokeID = 1;

		while (true) {
			PeerMessage message = readMessage(inputStream);

			if (message != null && message.id == bitfieldID) {
				break;
			}
		}

		sendInterested(outputStream);

		while (true) {
			PeerMessage message = readMessage(inputStream);

			if (message != null && message.id == unchokeID) {
				break;
			}
		}

		firstTimeHandshake = false;
	}

	public byte[] downloadPiece(int pieceIndex, int pieceLength, byte[] pieceHash, int fileLength) throws IOException {
		int blockSize = 16384;

		if (firstTimeHandshake) {
			establishConnection();
		}

		int remaining = fileLength - pieceIndex * pieceLength;
		if (remaining < pieceLength) {
			pieceLength = Math.abs(remaining);
		}
		// Send request for the piece
		for (int offset = 0; offset < pieceLength; offset += blockSize) {
			int blockLen = Math.min(blockSize, pieceLength - offset);
			sendRequest(outputStream, pieceIndex, offset, blockLen);
		}

		byte[] pieceData = new byte[pieceLength];
		int totalReceived = 0;

		while (totalReceived < pieceLength) {
			PeerMessage msg = readMessage(inputStream);
			if (msg == null || msg.id != 7) continue;

			ByteBuffer payload = ByteBuffer.wrap(msg.payload);
			int index = payload.getInt();       // you may want to verify it == pieceIndex
			int begin = payload.getInt();
			byte[] block = new byte[msg.payload.length - 8];
			payload.get(block);

			System.arraycopy(block, 0, pieceData, begin, block.length);
			totalReceived += block.length;
		}

		// Verify the piece hash
		byte[] dlHash = TorrentFileHandler.sha1Hash(pieceData);

		if (!Arrays.equals(dlHash, pieceHash)) {
			throw new IOException("Piece hash mismatch");
		}

		return pieceData;
	}

	public void writeToFile(byte[] data, String filePath) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			fos.write(data);
		}
	}



	private void sendInterested(OutputStream out) throws IOException {
		ByteArrayOutputStream msg = new ByteArrayOutputStream();
		msg.write(intToBytes(1)); // length
		msg.write(2);             // ID = interested
		out.write(msg.toByteArray());
		out.flush();
	}

	private void sendRequest(OutputStream out, int index, int begin, int length) throws IOException {
		ByteArrayOutputStream msg = new ByteArrayOutputStream();
		msg.write(intToBytes(13)); // 1 (ID) + 12 (payload)
		msg.write(6);              // ID = request
		msg.write(intToBytes(index));
		msg.write(intToBytes(begin));
		msg.write(intToBytes(length));
		out.write(msg.toByteArray());
		out.flush();
	}

	private static PeerMessage readMessage(InputStream in) throws IOException {
		byte[] lenBytes = in.readNBytes(4);
		if (lenBytes.length != 4) return null;

		int length = ByteBuffer.wrap(lenBytes).getInt();
		if (length == 0) return null; // keep-alive

		int id = in.read();
		byte[] payload = in.readNBytes(length - 1);
		return new PeerMessage(id, payload);
	}

	private static byte[] intToBytes(int val) {
		return ByteBuffer.allocate(4).putInt(val).array();
	}


}
