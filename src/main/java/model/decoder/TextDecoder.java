package model.decoder;

/**
 * model.decoder.Decoder for bencoded strings. This class implements the {@link Decoder} interface
 *
 * @author KX
 */
public class TextDecoder implements Decoder<String> {

  /**
   * Decodes a bencoded string from the given input starting at the specified index. Looks for the
   * colon character (':') to separate the length of the string and the string content. The length
   * is specified as a decimal integer before the colon, and the string content follows the colon.
   *
   * @param input      the bencoded string to decode
   * @param startIndex the index to start decoding from
   * @return a model.decoder.DecoderDTO containing the decoded string and the next index to continue decoding from
   * @throws IllegalArgumentException if the input is invalid or the start index is out of bounds
   */
  @Override
  public DecoderDTO<String> decode(String input, int startIndex) throws IllegalArgumentException {
    // Does not use validateInput() because it's prefix is not static
    if (input == null || input.isEmpty()) {
      throw new IllegalArgumentException("Input string cannot be null or empty");
    }
    if (startIndex < 0 || startIndex >= input.length()) {
      throw new IllegalArgumentException("Start index out of bounds: " + startIndex);
    }
    if (!Character.isDigit(input.charAt(startIndex))) {
      throw new IllegalArgumentException(
          "Invalid bencoded string format. Expected digit at index " + startIndex);
    }

    // Find the colon that separates the length from the string content
    int colonIndex = input.indexOf(':', startIndex);
    if (colonIndex == -1) {
      throw new IllegalArgumentException(
          "Invalid bencoded string: missing ':' after length at index " + startIndex);
    }

    // Extract the length of the string
    int length;
    try {
      length = Integer.parseInt(input.substring(startIndex, colonIndex));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid string length format at index " + startIndex, e);
    }

    // Validate the length and extract the string content
    int strStart = colonIndex + 1;
    int strEnd = strStart + length;
    if (strEnd > input.length()) {
      throw new IllegalArgumentException("String content exceeds input bounds.");
    }
    String value = input.substring(strStart, strEnd);

    return new DecoderDTO<>(value, strEnd);
  }

  @Override
  public DecoderByteDTO<String> decode(byte[] bencodedBytes, int startIndex)
      throws IllegalArgumentException {
    // Does not use validateInput() because it's prefix is not static
    if (startIndex < 0 || startIndex >= bencodedBytes.length) {
      throw new IllegalArgumentException("Start index out of bounds: " + startIndex);
    }
    if (bencodedBytes[startIndex] < '0' || bencodedBytes[startIndex] > '9') {
      throw new IllegalArgumentException(
          "Invalid bencoded string format. Expected digit at index " + startIndex);
    }

    // Find the colon that separates the length from the string content
    int colonIndex = startIndex;
    while (colonIndex < bencodedBytes.length && bencodedBytes[colonIndex] != ':') {
      if (bencodedBytes[colonIndex] < '0' || bencodedBytes[colonIndex] > '9') {
        throw new IllegalArgumentException("Non-digit character in length field");
      }
      colonIndex++;
    }

    if (colonIndex >= bencodedBytes.length || bencodedBytes[colonIndex] != ':') {
      throw new IllegalArgumentException("Missing ':' after string length");
    }

    // Extract the length of the string
    int length;
    try {
      length = Integer.parseInt(new String(bencodedBytes, startIndex, colonIndex - startIndex));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid string length", e);
    }

    // Validate the length and extract the string content
    int strStart = colonIndex + 1;
    int strEnd = strStart + length;
    if (strEnd > bencodedBytes.length) {
      throw new IllegalArgumentException("String content exceeds input bounds");
    }
    String value = new String(bencodedBytes, strStart, length);

    return new DecoderByteDTO<>(value, strEnd, strStart, strEnd - 1);
  }
}
