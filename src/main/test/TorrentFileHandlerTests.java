import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;


public class TorrentFileHandlerTests
{
	private TorrentFileHandler torrentFileHandler;

	@Test
	public void testIllegalFileInitialization()
	{
		String nonExistentFile = "non_existent_file.torrent";
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			torrentFileHandler = new TorrentFileHandler(nonExistentFile);
		});

		assertEquals("File does not exist: " + nonExistentFile, exception.getMessage());


		String invalidFileType = "invalid_file.txt";
		exception = assertThrows(IllegalArgumentException.class, () -> {
			torrentFileHandler = new TorrentFileHandler(invalidFileType);
		});
		assertEquals("File must have a .torrent extension", exception.getMessage());

		String emptyFileName = "";
		exception = assertThrows(IllegalArgumentException.class, () -> {
			torrentFileHandler = new TorrentFileHandler(emptyFileName);
		});

		assertEquals("File name cannot be empty", exception.getMessage());

		String nullFileName = null;
		exception = assertThrows(NullPointerException.class, () -> {
			torrentFileHandler = new TorrentFileHandler(nullFileName);
		});

		assertEquals("File name cannot be null", exception.getMessage());
	}

	@Test
	public void testValidFileInitialization()
	{
		String validFileName = "sample.torrent";
		torrentFileHandler = new TorrentFileHandler(validFileName);
		assertNotNull(torrentFileHandler);
		assertEquals(validFileName, torrentFileHandler.getFileName());
		assertTrue(torrentFileHandler.getFileContent().length > 0);
		assertNotNull(torrentFileHandler.getFileContentMap());
		assertNotNull(torrentFileHandler.getInfoMap());
		assertNotNull(torrentFileHandler.getTrackerUrl());
		assertNotNull(torrentFileHandler.getFileHash());
		assertNotNull(torrentFileHandler.getHashedPieces());
		assertTrue(torrentFileHandler.getHashedPieces().size() > 0);
		assertTrue(torrentFileHandler.getPieceLength() > 0);
		assertTrue(torrentFileHandler.getFileLength() > 0);
		assertEquals(torrentFileHandler.getFileLength(), torrentFileHandler.getInfoMap().get("length"));
		assertEquals(torrentFileHandler.getPieceLength(), torrentFileHandler.getInfoMap().get("piece length"));
		assertEquals(torrentFileHandler.getTrackerUrl(), torrentFileHandler.getFileContentMap().get("announce"));
	}
}
