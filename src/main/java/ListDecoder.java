import java.util.ArrayList;
import java.util.List;

/**
 * Decodes a bencoded list from a string or byte array. This class implements the {@link Decoder}
 * interface and uses a {@link DecoderDispatcher} to assist in decoding
 *
 * @author KX
 */
public class ListDecoder implements Decoder<List<Object>> {

  private final DecoderDispatcher dispatcher;

  /**
   * Constructor for ListDecoder that initializes the dispatcher.
   *
   * @param dispatcher The {@link DecoderDispatcher} used to decode elements within the list.
   */
  public ListDecoder(DecoderDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  /**
   * Decodes a bencoded list from the input string starting at the given index. Repeatedly calls the
   * dispatcher to decode each element until it finds 'e'. The list is expected to be in the format
   * "l<element1><element2>...e". For example, "l4:spam4:eggs" means a list with two strings: "spam"
   * and "eggs".
   *
   * @param input      The bencoded string to decode.
   * @param startIndex The index to start decoding from.
   * @return A DecoderDTO containing the decoded list and the next index to read from.
   */
  public DecoderDTO<List<Object>> decode(String input,
      int startIndex) throws IllegalArgumentException {
    validateInput(input, startIndex, 'l');

    List<Object> list = new ArrayList<>();
    int index = startIndex + 1; // Skip 'l'

    // Decode elements until we hit 'e'
    while (index < input.length() && input.charAt(index) != 'e') {
      DecoderDTO<?> element = dispatcher.decode(input, index);
      list.add(element.getValue());
      // index is updated in the decode method based on decoded type
      index = element.getNextIndex();
    }

    if (index >= input.length() || input.charAt(index) != 'e') {
      throw new IllegalArgumentException(
          "Invalid bencoded list: missing 'e' at index " + startIndex);
    }

    return new DecoderDTO<>(list, index + 1); // Skip 'e'
  }

  @Override
  public DecoderByteDTO<List<Object>> decode(byte[] bencodedBytes,
      int startIndex) throws IllegalArgumentException {
    validateInput(bencodedBytes, startIndex, 'l');

    List<Object> list = new ArrayList<>();
    int index = startIndex + 1; // Skip 'l'
    // Decode elements until we hit 'e'
    while (index < bencodedBytes.length && bencodedBytes[index] != 'e') {
      DecoderByteDTO<?> element = dispatcher.decode(bencodedBytes, index);
      list.add(element.getDecoderDTO().getValue());
      // index is updated in the decode method based on decoded type
      index = element.getDecoderDTO().getNextIndex();
    }

    if (index >= bencodedBytes.length || bencodedBytes[index] != 'e') {
      throw new IllegalArgumentException(
          "Invalid bencoded list: missing 'e' at index " + startIndex);
    }

    return new DecoderByteDTO<>(list, index + 1, startIndex + 1, index - 1); // Skip 'e'
  }
}
