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
		String numberStr = input.substring(startIndex + 1, endIndex);
		Integer value = Integer.parseInt(numberStr);
		return new DecoderDTO<Integer>(value, endIndex + 1);
	}

	@Override
	public DecoderDTO<Integer> decode(byte[] bencodedBytes, int startIndex, TorrentInfoDTO infoDTO) throws IllegalArgumentException {
		validateInput(bencodedBytes, startIndex, 'i');

		int endIndex = startIndex + 1;

		while (bencodedBytes[endIndex] != 'e') {
			endIndex++;
		}

		String numberStr = new String(bencodedBytes, startIndex + 1, endIndex - startIndex - 1);
		Integer value = Integer.parseInt(numberStr);
		return new DecoderDTO<Integer>(value, endIndex + 1);

	}

}
