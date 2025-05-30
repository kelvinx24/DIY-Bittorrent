This is a project build based on the information given in
["Build Your Own BitTorrent" Challenge](https://app.codecrafters.io/courses/bittorrent/overview).

BitTorrent Java Client
This is a simple Java-based BitTorrent client that supports decoding bencoded strings, reading .torrent metadata, connecting to peers, performing handshakes, and downloading pieces or full files.

📦 Features

✅ Bencode decoding

✅ Torrent file metadata inspection (.torrent)

✅ Tracker communication and peer discovery

✅ BitTorrent handshake with peers

✅ Piece-wise file download

✅ Full file download using multiple peers concurrently

🛠️ Requirements
- Java 8 or higher
- Gson (for JSON output)
- Internet access (to reach public trackers/peers)

🚀 Usage
Compile the project using your preferred Java build tool (Maven), then run the Main class with one of the supported commands.

`java model.Main <command> [args...]`

🧪 Commands
  1. Decode bencoded string
    `java model.Main decode "d3:cow3:moo4:spam4:eggse"`
  Output:
    `{"cow":"moo","spam":"eggs"}`

2. Display torrent file info
  `java model.Main info path/to/file.torrent`
  Output includes:
    - File name
    - File length
    - Tracker URL
    - Info hash
    - Piece length
    - Hashed pieces (SHA-1)
    - Parsed info dictionary (JSON)

3. List tracker peers
  `java model.Main peers path/to/file.torrent`
Output:
  - Tracker interval
  - List of peer IPs and ports

4. Perform BitTorrent handshake
  `java model.Main handshake path/to/file.torrent <peer_ip:port>`
Output:
  - Handshake peer ID in hex format

5. Download a single piece
  `java model.Main download_piece -o output_file path/to/file.torrent <piece_index>`
Output:
  - Saves the piece data to output_file
  - Confirms success or failure

6. Download the full file
  `java model.Main download -o output_file path/to/file.torrent`
Output:
  - Downloads all pieces concurrently using available peers
  - Writes complete file to output_file

📝 Notes
  - Uses RandomAlphaPeerIdGenerator for unique peer IDs.

  - Tracker requests use hardcoded port 6881 for simplicity.

  - Currently assumes single-file torrents.

🧪 Example .torrent File

  To test file downloads, you'll need a valid .torrent file referencing an available public tracker and seeders. Use sample.torrent.

🧼 Known Limitations
  - No GUI — CLI only.

  - No support for magnet links.

  - No DHT or peer exchange.
