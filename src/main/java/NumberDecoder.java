import java.util.Optional;

public class NumberDecoder implements Decoder<Integer>{
	/**
	 * Decodes a bencoded number. Looks for 'e' to find the end of the number.
	 * The number is expected to be in the format "i<number>e".
	 * For example, "i42e" means the number 42.
	 * @param input The bencoded string to decode.
	 * @param startIndex The index to start decoding from.
	 * @return
	 */
	@Override
	public DecoderDTO<Integer> decode(String input, int startIndex) throws IllegalArgumentException{
		validateInput(input, startIndex, 'i');

		int endIndex = input.indexOf('e', startIndex);
		if (endIndex == -1) {
			throw new IllegalArgumentException("Invalid bencoded number: missing 'e' at index " + startIndex);
		}
		String numberStr = input.substring(startIndex + 1, endIndex);
		int value;
		try {
			value = Integer.parseInt(numberStr);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid number format at index " + startIndex, e);
		}

		return new DecoderDTO<Integer>(Integer.valueOf(value), endIndex + 1);
	}

	@Override
	public DecoderDTO<Integer> decode(byte[] bencodedBytes, int startIndex, TorrentInfoDTO infoDTO) throws IllegalArgumentException {
		validateInput(bencodedBytes, startIndex, 'i');

		int endIndex = startIndex + 1;

		while (endIndex < bencodedBytes.length && bencodedBytes[endIndex] != 'e') {
			if (bencodedBytes[endIndex] < '0' || bencodedBytes[endIndex] > '9') {
				throw new IllegalArgumentException("Invalid character in bencoded number at index " + endIndex);
			}
			endIndex++;
		}

		if (endIndex >= bencodedBytes.length || bencodedBytes[endIndex] != 'e') {
			throw new IllegalArgumentException("Invalid bencoded number: missing 'e' at index " + startIndex);
		}

		String numberStr = new String(bencodedBytes, startIndex + 1, endIndex - startIndex - 1);
		int value;
		try {
			value = Integer.parseInt(numberStr);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid number format at index " + startIndex, e);
		}

		return new DecoderDTO<Integer>(Integer.valueOf(value), endIndex + 1);

	}

}
