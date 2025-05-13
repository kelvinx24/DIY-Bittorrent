import java.util.List;
import java.util.Map;

public class DecoderDispatcher {
	private final Decoder<Long> numberDecoder = new NumberDecoder();

	private final Decoder<String> textDecoder = new TextDecoder();

	private final Decoder<List<Object>> listDecoder;

	private final Decoder<Map<Object, Object>> dictionaryDecoder;

	public DecoderDispatcher() {
		this.listDecoder = new ListDecoder(this);
		this.dictionaryDecoder = new DictionaryDecoder(this);
	}

	public DecoderDTO decode(String input, int startIndex) {
		char prefix = input.charAt(startIndex);

		if (prefix == 'i') return numberDecoder.decode(input, startIndex);
		if (Character.isDigit(prefix)) return textDecoder.decode(input, startIndex);
		if (prefix == 'l') return listDecoder.decode(input, startIndex);
		if (prefix == 'd') return dictionaryDecoder.decode(input, startIndex);

		throw new IllegalArgumentException("Unknown bencode type at index " + startIndex);
	}

}
