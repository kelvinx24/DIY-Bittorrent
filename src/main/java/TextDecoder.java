public class TextDecoder implements Decoder<String> {
	/**
	 * Decodes a bencoded string. Looks for ':' to find the end of the length.
	 * The string part is then extracted based on the length.
	 * The format is: <length>:<string>
	 * For example, "4:spam" means the string "spam" with length 4.
	 *
	 * @param input      The bencoded string to decode.
	 * @param startIndex The index to start decoding from.
	 * @return A DecoderDTO containing the decoded string and the next index.
	 */
	@Override
	public DecoderDTO decode(String input, int startIndex) {
		int colonIndex = input.indexOf(':', startIndex);
		int length = Integer.parseInt(input.substring(startIndex, colonIndex));
		int strStart = colonIndex + 1;
		int strEnd = strStart + length;
		String value = input.substring(strStart, strEnd);
		return new DecoderDTO(value, strEnd);
	}


}
