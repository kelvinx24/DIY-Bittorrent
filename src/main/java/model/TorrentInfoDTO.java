package model;

import java.util.LinkedHashMap;
import java.util.Map;
import model.decoder.NumberPair;

public class TorrentInfoDTO {
	private final Map<String, NumberPair> byteRangeMap;

	public TorrentInfoDTO() {
		this.byteRangeMap = new LinkedHashMap<>();
	}

	public Map<String, NumberPair> getByteRangeMap() {
		return byteRangeMap;
	}

	public NumberPair getByteRange(String key) {
		return this.byteRangeMap.get(key);
	}

	public NumberPair getInfoByteRange() {
		return this.byteRangeMap.get("info");
	}

	public void addByteRange(String key, int start, int end) {
		this.byteRangeMap.put(key, new NumberPair(start, end));
	}

}
