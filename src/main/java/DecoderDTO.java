public class DecoderDTO {
	private final Object value;      // Can be Integer, String, List, Map
	private final int nextIndex;     // Index after the decoded value

	public DecoderDTO(Object value, int nextIndex) {
		this.value = value;
		this.nextIndex = nextIndex;
	}

	public Object getValue() {
		return value;
	}

	public int getNextIndex() {
		return nextIndex;
	}
}
