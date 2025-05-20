public interface Decoder<T> {
	DecoderDTO<T> decode(String bencodedString, int startIndex) throws IllegalArgumentException;
	DecoderByteDTO<T> decode(byte[] bencodedBytes, int startIndex) throws IllegalArgumentException;

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
