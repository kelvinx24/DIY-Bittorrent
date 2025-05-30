import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DictionaryDecoder is responsible for decoding bencoded dictionaries from a string or byte array.
 * It relies on a DecoderDispatcher to handle the decoding of keys and values within the dictionary.
 * This class implements the {@link DecoderDTO} interface
 *
 * @author KX
 */
public class DictionaryDecoder implements Decoder<Map<String, Object>> {

  private final DecoderDispatcher dispatcher;

  /**
   * Constructor for DictionaryDecoder. This initializes the decoder with a
   * {@link DecoderDispatcher}
   *
   * @param dispatcher the {@link DictionaryDecoder} used to decode keys and values within the
   *                   dictionary.
   */
  public DictionaryDecoder(DecoderDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  /**
   * Decodes a bencoded dictionary from the input string starting at the given index. Repeatedly
   * calls the dispatcher to decode each key-value pair until it finds 'e'. Recursively decodes the
   * keys and values using the dispatcher. The dictionary is expected to be in the format
   * "d<key1><value1><key2><value2>...e". For example, "d3:spami42e3:eggs4:spam" means a dictionary
   * with two entries:
   *
   * @param input      the bencoded string to decode.
   * @param startIndex the index to start decoding from.
   * @return A DecoderDTO containing the decoded dictionary and the next index to read from.
   * @throws IllegalArgumentException if the input is invalid or if the dictionary format is
   *                                  incorrect.
   */
  @Override
  public DecoderDTO<Map<String, Object>> decode(String input,
      int startIndex) throws IllegalArgumentException {

    validateInput(input, startIndex, 'd');

    Map<String, Object> dict = new LinkedHashMap<>();
    int index = startIndex + 1; // Skip 'd'

    while (index < input.length() && input.charAt(index) != 'e') {
      DecoderDTO<?> keyResult = dispatcher.decode(input, index);

      String key;
      if (keyResult.getValue() == null || !(keyResult.getValue() instanceof String)) {
        throw new IllegalArgumentException(
            "Key cannot be null or non-string type at index " + index);
      }

      key = (String) keyResult.getValue(); // Keys are always strings

      // Keys are always strings
      // Move the index to the end of the key
      index = keyResult.getNextIndex();
      if (index >= input.length() || input.charAt(index) == 'e') {
        throw new IllegalArgumentException(
            "Invalid bencoded dictionary: missing value for key '" + key + "' at index " + index);
      }

      DecoderDTO<?> valueResult = dispatcher.decode(input, index);
      dict.put(key, valueResult.getValue());
      // Move the index to the end of the value
      index = valueResult.getNextIndex();
    }

    if (index >= input.length() || input.charAt(index) != 'e') {
      throw new IllegalArgumentException(
          "Invalid bencoded dictionary: missing 'e' at index " + startIndex);
    }

    return new DecoderDTO<>(dict, index + 1); // Skip 'e'
  }

  @Override
  public DecoderByteDTO<Map<String, Object>> decode(byte[] bencodedBytes, int startIndex)
      throws IllegalArgumentException {
    validateInput(bencodedBytes, startIndex, 'd');

    Map<String, Object> dict = new LinkedHashMap<>();
    int index = startIndex + 1; // Skip 'd'
    int infoIndexStart = index;
    int infoIndexEnd = index;

    LinkedHashMap<String, NumberPair> byteRanges = new LinkedHashMap<>();
    // Continues to decode via the dispatcher until it finds 'e' - the end of the dictionary
    while (index < bencodedBytes.length && bencodedBytes[index] != 'e') {
      // Decode the key using the dispatcher
      DecoderByteDTO<?> keyResultByte = dispatcher.decode(bencodedBytes, index);
      DecoderDTO<?> keyResult = keyResultByte.getDecoderDTO();
      String key;
      if (keyResult.getValue() == null || !(keyResult.getValue() instanceof String)) {
        throw new IllegalArgumentException(
            "Key cannot be null or non-string type at index " + index);
      }
      key = (String) keyResult.getValue(); // Keys are always strings

      // Move the index to the start of the value
      index = keyResult.getNextIndex();
      if (index >= bencodedBytes.length || bencodedBytes[index] == 'e') {
        throw new IllegalArgumentException(
            "Invalid bencoded dictionary: missing value for key '" + key + "' at index " + index);
      }

      infoIndexStart = index;

      // Decode the value using the dispatcher
      DecoderByteDTO<?> valueResultByte = dispatcher.decode(bencodedBytes, index);
      DecoderDTO<?> valueResult = valueResultByte.getDecoderDTO();
      dict.put(key, valueResult.getValue());
      // Move the index to the end of the value
      index = valueResult.getNextIndex();

      // if key is "info", we store the entire byte range after the key
      if (key.equals("info")) {
        infoIndexEnd = index;
        byteRanges.put(key, new NumberPair(infoIndexStart, infoIndexEnd));
      } else {
        // For other keys, we store the byte range for the value only - no prefix or suffix
        infoIndexStart = valueResultByte.getValueRange().first();
        infoIndexEnd = valueResultByte.getValueRange().second();
        byteRanges.put(key, new NumberPair(infoIndexStart, infoIndexEnd));
      }

      // Add the byte ranges for the keys and values to the byteRanges map from the valueResultByte
      // This will keep track of the byte ranges for each key-value pair after each recursive call
      for (Map.Entry<String, NumberPair> entry : valueResultByte.getByteRanges().entrySet()) {
        String keyByteRange = entry.getKey();
        NumberPair valueByteRange = entry.getValue();
        byteRanges.put(keyByteRange, valueByteRange);
      }

    }

    if (index >= bencodedBytes.length || bencodedBytes[index] != 'e') {
      throw new IllegalArgumentException(
          "Invalid bencoded dictionary: missing 'e' at index " + startIndex);
    }

    int nextIndex = index + 1; // Skip 'e'

    return new DecoderByteDTO<>(
        new DecoderDTO<>(dict, nextIndex),
        byteRanges, new NumberPair(startIndex + 1, index - 1));
  }

}
