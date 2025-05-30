/**
 * Data Transfer Object for storing the result of a decoding operation.
 * @param <T> the type of the decoded value, which can be Integer, String, List, Map
 *
 * @author KX
 */
public class DecoderDTO<T> {
	private final T value;      // Can be Integer, String, List, Map
	private final int nextIndex;     // Index after the decoded value

	/**
	 * Constructor for DecoderDTO.
	 *
	 * @param value      the decoded value, which can be Integer, String, List, Map
	 * @param nextIndex  the index to continue decoding from after this value
	 */
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
