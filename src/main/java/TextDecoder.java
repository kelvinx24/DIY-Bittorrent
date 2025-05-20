public class TextDecoder implements Decoder<String> {

	@Override
	public DecoderDTO<String> decode(String input, int startIndex) throws IllegalArgumentException {
		if (input == null || input.isEmpty()) {
			throw new IllegalArgumentException("Input string cannot be null or empty");
		}
		if (startIndex < 0 || startIndex >= input.length()) {
			throw new IllegalArgumentException("Start index out of bounds: " + startIndex);
		}
		if (!Character.isDigit(input.charAt(startIndex))) {
			throw new IllegalArgumentException("Invalid bencoded string format. Expected digit at index " + startIndex);
		}

		int colonIndex = input.indexOf(':', startIndex);
		if (colonIndex == -1) {
			throw new IllegalArgumentException("Invalid bencoded string: missing ':' after length at index " + startIndex);
		}

		int length;
		try {
			length = Integer.parseInt(input.substring(startIndex, colonIndex));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid string length format at index " + startIndex, e);
		}

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
		if (startIndex < 0 || startIndex >= bencodedBytes.length) {
			throw new IllegalArgumentException("Start index out of bounds: " + startIndex);
		}
		if (bencodedBytes[startIndex] < '0' || bencodedBytes[startIndex] > '9') {
			throw new IllegalArgumentException("Invalid bencoded string format. Expected digit at index " + startIndex);
		}

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

		int length;
		try {
			length = Integer.parseInt(new String(bencodedBytes, startIndex, colonIndex - startIndex));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid string length", e);
		}

		int strStart = colonIndex + 1;
		int strEnd = strStart + length;

		if (strEnd > bencodedBytes.length) {
			throw new IllegalArgumentException("String content exceeds input bounds");
		}

		String value = new String(bencodedBytes, strStart, length);
		return new DecoderByteDTO<String>(value, strEnd, startIndex, strEnd - 1);
	}
}
