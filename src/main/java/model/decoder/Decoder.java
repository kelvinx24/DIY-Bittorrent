package model.decoder;

/**
 * model.decoder.Decoder interface for decoding bencoded strings and byte arrays.
 * @param <T> the type of object that will be returned after decoding
 * @author KX
 */
public interface Decoder<T> {
	/**
	 * Decodes a bencoded string starting from the specified index.
	 *
	 * @param bencodedString the bencoded string to decode
	 * @param startIndex     the index to start decoding from
	 * @return a model.decoder.DecoderDTO containing the decoded object and the next index
	 * @throws IllegalArgumentException if the input is invalid
	 */
	DecoderDTO<T> decode(String bencodedString, int startIndex) throws IllegalArgumentException;

	/**
	 * Decodes a bencoded byte array starting from the specified index.
	 * @param bencodedBytes the bencoded byte array to decode
	 * @param startIndex the index to start decoding from
	 * @return a model.decoder.DecoderByteDTO containing the decoded object and the next index
	 * also includes byte ranges for the decoded object
	 * @throws IllegalArgumentException if the input is invalid
	 */
	DecoderByteDTO<T> decode(byte[] bencodedBytes, int startIndex) throws IllegalArgumentException;

	/**
	 * Validates the input string or byte array at the specified index for a specific prefix.
	 *
	 * @param input         the input string or byte array to validate
	 * @param startIndex    the index to check for the expected prefix
	 * @param expectedPrefix the expected prefix character
	 * @throws IllegalArgumentException if the input is invalid or does not match the expected prefix
	 */
	default void validateInput(String input, int startIndex, char expectedPrefix) {
		if (input == null || input.isEmpty()) {
			throw new IllegalArgumentException("Input string cannot be null or empty");
		}

		if (startIndex < 0 || startIndex >= input.length()) {
			throw new IllegalArgumentException("Start index out of bounds: " + startIndex);
		}

		if (input.charAt(startIndex) != expectedPrefix) {
			throw new IllegalArgumentException("Expected prefix '" + expectedPrefix + "', found '" + input.charAt(startIndex) + "'");
		}
	}

	/**
	 * Validates the input byte array at the specified index for a specific prefix.
	 *
	 * @param input         the input byte array to validate
	 * @param startIndex    the index to check for the expected prefix
	 * @param expectedPrefix the expected prefix character
	 * @throws IllegalArgumentException if the input is invalid or does not match the expected prefix
	 */
	default void validateInput(byte[] input, int startIndex, char expectedPrefix) {
		if (input == null || input.length == 0) {
			throw new IllegalArgumentException("Input byte array cannot be null or empty");
		}

		if (startIndex < 0 || startIndex >= input.length) {
			throw new IllegalArgumentException("Start index out of bounds: " + startIndex);
		}

		if (input[startIndex] != expectedPrefix) {
			throw new IllegalArgumentException("Expected prefix '" + expectedPrefix + "', found '" + (char) input[startIndex] + "'");
		}
	}
}
