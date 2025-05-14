import java.util.Optional;

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
	public DecoderDTO<String> decode(String input, int startIndex) {
		int colonIndex = input.indexOf(':', startIndex);
		int length = Integer.parseInt(input.substring(startIndex, colonIndex));
		int strStart = colonIndex + 1;
		int strEnd = strStart + length;
		String value = input.substring(strStart, strEnd);
		return new DecoderDTO<String>(value, strEnd);
	}

	@Override
	public DecoderDTO<String> decode(byte[] bencodedBytes, int startIndex, TorrentInfoDTO infoDTO) throws RuntimeException {
		if (bencodedBytes[startIndex] >= '0' && bencodedBytes[startIndex] <= '9') {
			int colonIndex = startIndex + 1;
			while (bencodedBytes[colonIndex] != ':') {
				colonIndex++;
			}
			int length = Integer.parseInt(new String(bencodedBytes, startIndex, colonIndex - startIndex));
			int strStart = colonIndex + 1;
			int strEnd = strStart + length;
			String value = new String(bencodedBytes, strStart, length);
			return new DecoderDTO<String>(value, strEnd);
		} else {
			throw new RuntimeException("Invalid bencoded string format");
		}
	}


}
