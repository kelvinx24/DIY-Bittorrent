import java.security.KeyPair;
import java.util.LinkedHashMap;
import java.util.Map;

public class TorrentInfoDTO {
	private int startIndex;
	private int endIndex;

	private final Map<String, NumberPair> byteRangeMap;

	public TorrentInfoDTO() {
		this.startIndex = -1;
		this.endIndex = -1;
		this.byteRangeMap = new LinkedHashMap<>();
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public int setEndIndex(int endIndex) {
		return this.endIndex = endIndex;
	}

	public int setStartIndex(int startIndex) {
		return this.startIndex = startIndex;
	}

	public void addByteRange(String key, int start, int end) {
		this.byteRangeMap.put(key, new NumberPair(start, end));
	}

}
