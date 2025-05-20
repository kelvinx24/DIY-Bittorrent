import java.util.List;
import java.util.Map;

public class DecoderDispatcher {
	private final Decoder<Integer> numberDecoder = new NumberDecoder();

	private final Decoder<String> textDecoder = new TextDecoder();

	private final Decoder<List<Object>> listDecoder;

	private final Decoder<Map<String, Object>> dictionaryDecoder;

	public DecoderDispatcher() {
		this.listDecoder = new ListDecoder(this);
		this.dictionaryDecoder = new DictionaryDecoder(this);
	}

	public DecoderDTO<?> decode(String input, int startIndex) {
		if (input == null || input.isEmpty()) {
			throw new IllegalArgumentException("Input string cannot be null or empty");
		}

		if (startIndex < 0 || startIndex >= input.length()) {
			throw new IllegalArgumentException("Start index out of bounds: " + startIndex);
		}

		char prefix = input.charAt(startIndex);

		if (prefix == 'i') return numberDecoder.decode(input, startIndex);
		if (Character.isDigit(prefix)) return textDecoder.decode(input, startIndex);
		if (prefix == 'l') return listDecoder.decode(input, startIndex);
		if (prefix == 'd') return dictionaryDecoder.decode(input, startIndex);

		throw new IllegalArgumentException("Unknown bencode type at index " + startIndex);
	}

	public DecoderByteDTO<?> decode(byte[] input, int startIndex) {
		if (input == null || input.length == 0) {
			throw new IllegalArgumentException("Input byte array cannot be null or empty");
		}

		if (startIndex < 0 || startIndex >= input.length) {
			throw new IllegalArgumentException("Start index out of bounds: " + startIndex);
		}

		byte prefix = input[startIndex];

		if (prefix == 'i') return numberDecoder.decode(input, startIndex);
		if (prefix >= '0' && prefix <= '9') return textDecoder.decode(input, startIndex);
		if (prefix == 'l') return listDecoder.decode(input, startIndex);
		if (prefix == 'd') return dictionaryDecoder.decode(input, startIndex);

		throw new IllegalArgumentException("Unknown bencode type at index " + startIndex);
	}

}
