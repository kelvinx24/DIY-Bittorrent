package model;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import model.decoder.DecoderDTO;
import model.decoder.DecoderDispatcher;
import model.session.DefaultPeerSessionFactory;
import model.session.DefaultPieceWriter;
import model.session.DefaultTrackerClientFactory;
import model.session.PeerSession;
import model.session.RandomAlphaPeerIdGenerator;
import model.session.RandomIdGenerator;
import model.session.TorrentFileHandler;
import model.session.TorrentSession;
import model.session.TrackerClient;
import model.session.TrackerResponse;
// import com.dampcake.bencode.Bencode; - available if you need it!

public class Main {

  private static final Gson gson = new Gson();

  public static void main(String[] args) throws Exception {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");

    String command = args[0];
    if ("decode".equals(command)) {
      //  Uncomment this block to pass the first stage
      String bencodedValue = args[1];
      Object decoded;
      try {
        decoded = decodeBencode(bencodedValue);
      } catch (RuntimeException e) {
        System.out.println(e.getMessage());
        return;
      }

      System.out.println(gson.toJson(decoded));


    } else if ("info".equals(command)) {
      String filepath = args[1];
      TorrentFileHandler tfh = new TorrentFileHandler(filepath);
      System.out.println("File Name: " + tfh.getFileName());
      System.out.println("File Length: " + tfh.getFileLength());
      System.out.println("Tracker URL: " + tfh.getTrackerUrl());
      System.out.println("Hash: " + TorrentFileHandler.bytesToHex(tfh.getInfoHash()));
      System.out.println("Content: " + gson.toJson(tfh.getFileContentMap()));
      System.out.println("Piece Length: " + tfh.getPieceLength());
      System.out.println("Hashed Pieces: ");
      for (byte[] piece : tfh.getHashedPieces()) {
        System.out.println(TorrentFileHandler.bytesToHex(piece));
      }
      System.out.println("Info: " + gson.toJson(tfh.getInfoMap()));

    } else if (command.equals("peers")) {
      String filepath = args[1];
      TorrentFileHandler tfh = new TorrentFileHandler(filepath);
      DefaultTrackerClientFactory trackerClientFactory = new DefaultTrackerClientFactory();
      RandomIdGenerator peerIdGenerator = new RandomAlphaPeerIdGenerator();
      TrackerClient tc = trackerClientFactory.create(
          tfh.getTrackerUrl(), 6881, 1, tfh.getInfoHash(), peerIdGenerator.generate());

      TrackerResponse tr = tc.requestTracker();
      System.out.println("Interval: " + tr.getInterval());
      System.out.println("Peers: ");
      for (Map.Entry<String, Integer> entry : tr.getPeersMap().entrySet()) {
        System.out.println(entry.getKey() + ":" + entry.getValue());
      }
    } else if (command.equals("handshake")) {
      String filepath = args[1];
      String peer = args[2];
      String[] peerParts = peer.split(":");
      String ipAddr = peerParts[0];
      int port = Integer.parseInt(peerParts[1]);

      TorrentFileHandler tfh = new TorrentFileHandler(filepath);
      RandomIdGenerator peerIdGenerator = new RandomAlphaPeerIdGenerator();
      PeerSession peerSession = new DefaultPeerSessionFactory().create(ipAddr, port,
          peerIdGenerator.generate(), tfh.getInfoHash());
      byte[] handshakeResponse = peerSession.peerHandshake();
      System.out.println("Handshake: " + parsePeerIDHandshake(handshakeResponse));
    } else if (command.equals("download_piece")) {
      String outputIndicator = args[1];
      String outputFile = args[2];
      String filepath = args[3];
      int pieceIndex = Integer.parseInt(args[4]);

      if (!outputIndicator.equals("-o")) {
        System.out.println("Invalid output indicator. Use -o to specify output file.");
        return;
      }

      TorrentFileHandler tfh = new TorrentFileHandler(filepath);
      TorrentSession ts = new TorrentSession(tfh, Paths.get(outputFile),
          new DefaultTrackerClientFactory(), new DefaultPeerSessionFactory(),
          new DefaultPieceWriter(), new RandomAlphaPeerIdGenerator());

      byte[] data = ts.downloadPiece(pieceIndex);
      if (data != null) {
        writeBytesToFile(data, outputFile);
        System.out.println("Piece " + pieceIndex + " downloaded successfully to " + outputFile);
      } else {
        System.out.println("Failed to download piece " + pieceIndex);
      }

      ts.closeAllConnections();
    } else if (command.equals("download")) {
      String outputIndicator = args[1];
      String outputFile = args[2];
      String filepath = args[3];

      if (!outputIndicator.equals("-o")) {
        System.out.println("Invalid output indicator. Use -o to specify output file.");
        return;
      }

      TorrentFileHandler tfh = new TorrentFileHandler(filepath);
      DefaultTrackerClientFactory trackerClientFactory = new DefaultTrackerClientFactory();
      DefaultPeerSessionFactory peerSessionFactory = new DefaultPeerSessionFactory();
      DefaultPieceWriter pieceWriter = new DefaultPieceWriter();
      RandomAlphaPeerIdGenerator peerIdGenerator = new RandomAlphaPeerIdGenerator();
      TorrentSession ts = new TorrentSession(tfh, Paths.get(outputFile), trackerClientFactory,
          peerSessionFactory, pieceWriter, peerIdGenerator);

      ts.downloadAll();
    } else {
      System.out.println("Unknown command: " + command);
    }

  }

  static Object decodeBencode(String bencodedString) {

    DecoderDispatcher dispatcher = new DecoderDispatcher();
    DecoderDTO<?> dto = dispatcher.decode(bencodedString, 0);

    return dto.getValue();
  }

  static String openTorrent(String filepath) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(filepath));
    return new String(bytes);
  }

  static String parsePeerIDHandshake(byte[] handshake) {
    if (handshake.length < 48) {
      throw new IllegalArgumentException("Invalid handshake length");
    }

    byte[] peerIDBytes = Arrays.copyOfRange(handshake, 28, 48);


    return TorrentFileHandler.bytesToHex(peerIDBytes);
  }

  static void writeBytesToFile(byte[] bytes, String outputFile) throws IOException {
    Path path = Paths.get(outputFile);
    Files.write(path, bytes);
  }

}
