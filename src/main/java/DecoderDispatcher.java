import java.util.List;
import java.util.Map;

/**
 * DecoderDispatcher is responsible for dispatching the decoding of bencoded data to the appropriate
 * decoder based on the prefix character of the input string or byte array. It supports decoding
 * integers, strings, lists, and dictionaries.
 *
 * @author KX
 */
public class DecoderDispatcher {

  private final Decoder<Integer> numberDecoder = new NumberDecoder();

  private final Decoder<String> textDecoder = new TextDecoder();

  private final Decoder<List<Object>> listDecoder;

  private final Decoder<Map<String, Object>> dictionaryDecoder;

  /**
   * Default constructor initializes the list and dictionary decoders with itself.
   * The list decoder is responsible for decoding bencoded lists,
   * and the dictionary decoder is responsible for decoding bencoded dictionaries.
   */
  public DecoderDispatcher() {
    this.listDecoder = new ListDecoder(this);
    this.dictionaryDecoder = new DictionaryDecoder(this);
  }

  /**
   * Decodes a bencoded string starting from the specified index using recursive dispatching.
   * @param input the bencoded input string
   * @param startIndex the index in the input string to start decoding from.
   *                   Necessary for recursive decoding of lists and dictionaries.
   * @return a {@link DecoderDTO} containing the decoded value and the next index
   */
  public DecoderDTO<?> decode(String input, int startIndex) {
    if (input == null || input.isEmpty()) {
      throw new IllegalArgumentException("Input string cannot be null or empty");
    }

    if (startIndex < 0 || startIndex >= input.length()) {
      throw new IllegalArgumentException("Start index out of bounds: " + startIndex);
    }

    char prefix = input.charAt(startIndex);

		if (prefix == 'i') {
			return numberDecoder.decode(input, startIndex);
		}
		if (Character.isDigit(prefix)) {
			return textDecoder.decode(input, startIndex);
		}
		if (prefix == 'l') {
			return listDecoder.decode(input, startIndex);
		}
		if (prefix == 'd') {
			return dictionaryDecoder.decode(input, startIndex);
		}

    throw new IllegalArgumentException("Unknown bencode type at index " + startIndex);
  }

  /**
   * Decodes a bencoded byte array starting from the specified index using recursive dispatching.
   * @param input the bencoded input byte array
   * @param startIndex the index in the input byte array to start decoding from.
   *                   Necessary for recursive decoding of lists and dictionaries.
   * @return a {@link DecoderByteDTO} containing the decoded value and the next index
   */
  public DecoderByteDTO<?> decode(byte[] input, int startIndex) {
    if (input == null || input.length == 0) {
      throw new IllegalArgumentException("Input byte array cannot be null or empty");
    }

    if (startIndex < 0 || startIndex >= input.length) {
      throw new IllegalArgumentException("Start index out of bounds: " + startIndex);
    }

    byte prefix = input[startIndex];

		if (prefix == 'i') {
			return numberDecoder.decode(input, startIndex);
		}
		if (prefix >= '0' && prefix <= '9') {
			return textDecoder.decode(input, startIndex);
		}
		if (prefix == 'l') {
			return listDecoder.decode(input, startIndex);
		}
		if (prefix == 'd') {
			return dictionaryDecoder.decode(input, startIndex);
		}

    throw new IllegalArgumentException("Unknown bencode type at index " + startIndex);
  }

}
