public class NumberDecoder implements Decoder<Long>{
	private int endindex = 0;

	/**
	 * Decodes a bencoded number. Looks for 'e' to find the end of the number.
	 * The number is expected to be in the format "i<number>e".
	 * For example, "i42e" means the number 42.
	 * @param input The bencoded string to decode.
	 * @param startIndex The index to start decoding from.
	 * @return
	 */
	@Override
	public DecoderDTO decode(String input, int startIndex) {
		int endIndex = input.indexOf('e', startIndex);
		String numberStr = input.substring(startIndex + 1, endIndex);
		int value = Integer.parseInt(numberStr);
		return new DecoderDTO(value, endIndex + 1);
	}

}
