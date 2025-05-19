import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class DictionaryDecoder implements Decoder<Map<String, Object>> {

  private final DecoderDispatcher dispatcher;

  public DictionaryDecoder(DecoderDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  @Override
  /**
   * Decodes a bencoded dictionary from the input string starting at the given index.
   * Repeatedly calls the dispatcher to decode each key-value pair until it finds 'e'.
   * Recursively decodes the keys and values using the dispatcher.
   * The dictionary is expected to be in the format "d<key1><value1><key2><value2>...e".
   * For example, "d3:spami42e3:eggs4:spam" means a dictionary with two entries:
   */
  public DecoderDTO<Map<String, Object>> decode(String input,
      int startIndex) throws IllegalArgumentException {

    validateInput(input, startIndex, 'd');

    Map<String, Object> dict = new LinkedHashMap<>();
    int index = startIndex + 1; // Skip 'd'

    while (index < input.length() && input.charAt(index) != 'e') {
      DecoderDTO<?> keyResult = dispatcher.decode(input, index);

      String key = null;
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

    return new DecoderDTO<Map<String, Object>>(dict, index + 1); // Skip 'e'
  }

  @Override
  public DecoderByteDTO<Map<String, Object>> decode(byte[] bencodedBytes, int startIndex)
      throws IllegalArgumentException {
    validateInput(bencodedBytes, startIndex, 'd');

    Map<String, Object> dict = new LinkedHashMap<>();
    int index = startIndex + 1; // Skip 'd'
    int infoIndexStart = index; // Store the start index for info
    int infoIndexEnd = index; // Initialize the end index for info

    LinkedHashMap<String, NumberPair> byteRanges = new LinkedHashMap<>();
    while (index < bencodedBytes.length && bencodedBytes[index] != 'e') {
      DecoderByteDTO<?> keyResultByte = dispatcher.decode(bencodedBytes, index);
      DecoderDTO<?> keyResult = keyResultByte.getDecoderDTO();
      String key = null;
      if (keyResult.getValue() == null || !(keyResult.getValue() instanceof String)) {
        throw new IllegalArgumentException(
            "Key cannot be null or non-string type at index " + index);
      }
      key = (String) keyResult.getValue(); // Keys are always strings

      // Move the index to the end of the key
      index = keyResult.getNextIndex();
      if (index >= bencodedBytes.length || bencodedBytes[index] == 'e') {
        throw new IllegalArgumentException(
            "Invalid bencoded dictionary: missing value for key '" + key + "' at index " + index);
      }

      infoIndexStart = index;

      DecoderByteDTO<?> valueResultByte = dispatcher.decode(bencodedBytes, index);
      DecoderDTO<?> valueResult = valueResultByte.getDecoderDTO();
      dict.put(key, valueResult.getValue());
      // Move the index to the end of the value
      index = valueResult.getNextIndex();

      if (valueResultByte != null) {
        if (!key.equals("info")) {
          // shift the infoIndexStart to the value start
          infoIndexStart = infoIndexStart + 3;
        }
        infoIndexEnd = index;
        byteRanges.put(key, new NumberPair(infoIndexStart, infoIndexEnd));
      }

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
    DecoderByteDTO<Map<String, Object>> byteDTO = new DecoderByteDTO<>(
        new DecoderDTO<>(dict, nextIndex), byteRanges, startIndex + 1);

    return byteDTO;
  }

}
