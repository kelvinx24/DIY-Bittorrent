public class DecoderDTO<T> {
	private final T value;      // Can be Integer, String, List, Map
	private final int nextIndex;     // Index after the decoded value

	public DecoderDTO(T value, int nextIndex) {
		this.value = value;
		this.nextIndex = nextIndex;
	}

	public T getValue() {
		return value;
	}

	public int getNextIndex() {
		return nextIndex;
	}
}
