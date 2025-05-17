import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
// import com.dampcake.bencode.Bencode; - available if you need it!

public class Main {
  private static final Gson gson = new Gson();

  public static void main(String[] args) throws Exception {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");
    
    String command = args[0];
    if("decode".equals(command)) {
      //  Uncomment this block to pass the first stage
      String bencodedValue = args[1];
      Object decoded;
      try {
       decoded = decodeBencode(bencodedValue);
      } catch(RuntimeException e) {
         System.out.println(e.getMessage());
         return;
      }


      /*
      if (bencodedValue.charAt(0) == 'i') {
        long decodedLong = Long.parseLong(decoded);
        System.out.println(gson.toJson(decodedLong));
      }
      else {
        System.out.println(gson.toJson(decoded));
      }

       */
      System.out.println(gson.toJson(decoded));


    }
    else if ("info".equals(command)) {
        String filepath = args[1];
        TorrentFileHandler tfh = new TorrentFileHandler(filepath);
        System.out.println("File Name: " + tfh.getFileName());
        System.out.println("File Length: " + tfh.getFileLength());
        System.out.println("Tracker URL: " + tfh.getTrackerUrl());
        System.out.println("Hash: " + TorrentFileHandler.bytesToHex(tfh.getFileHash()));
        System.out.println("Content: " + gson.toJson(tfh.getFileContentMap()));
        System.out.println("Piece Length: " + tfh.getPieceLength());
        System.out.println("Hashed Pieces: ");
        for (byte[] piece : tfh.getHashedPieces()) {
          System.out.println(TorrentFileHandler.bytesToHex(piece));
        }
        System.out.println("Info: " + gson.toJson(tfh.getInfoMap()));

    }

    else if (command.equals("peers")) {
        String filepath = args[1];
        TorrentFileHandler tfh = new TorrentFileHandler(filepath);
        PeerRequester peerRequester = new PeerRequester(tfh.getTrackerUrl(), 6881, tfh.getFileLength(), tfh.getFileHash());
        TrackerResponse tr = peerRequester.requestTracker();
        System.out.println("Interval: " + tr.getInterval());
        System.out.println("Peers: ");
        for (Map.Entry<String, Integer> entry : tr.getPeersMap().entrySet()) {
          System.out.println(entry.getKey() + ":" + entry.getValue());
        }
    }
    else if (command.equals("handshake")) {
        String filepath = args[1];
        String peer = args[2];
        String[] peerParts = peer.split(":");
        String ipAddr = peerParts[0];
        int port = Integer.parseInt(peerParts[1]);

        TorrentFileHandler tfh = new TorrentFileHandler(filepath);
        PeerRequester peerRequester = new PeerRequester(tfh.getTrackerUrl(), 6881, tfh.getFileLength(), tfh.getFileHash());
        byte[] handshake = peerRequester.peerHandshake(ipAddr, port);
        System.out.println("Handshake: " + TorrentFileHandler.bytesToHex(handshake));
    }
    else if (command.equals("download_piece")) {
        String filepath = args[1];

        TorrentFileHandler tfh = new TorrentFileHandler(filepath);
        PeerRequester peerRequester = new PeerRequester(tfh.getTrackerUrl(), 6881, tfh.getFileLength(), tfh.getFileHash());
        TrackerResponse tr = peerRequester.requestTracker();
        Map.Entry<String, Integer> entr = tr.getPeersMap().entrySet().iterator().next();

        String ipAddr = entr.getKey();
        int port = entr.getValue();

        byte[] handshake = peerRequester.peerHandshake(ipAddr, port);
        System.out.println("Handshake: " + TorrentFileHandler.bytesToHex(handshake));
        byte[] piece = peerRequester.downloadPiece(2, tfh.getPieceLength(), tfh.getFileLength());

        peerRequester.writeToFile(piece, "piece_2.dat");
        System.out.println("Piece downloaded and saved as piece_2.dat");
        peerRequester.closeConnection();
    }
    else {
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
  
}
