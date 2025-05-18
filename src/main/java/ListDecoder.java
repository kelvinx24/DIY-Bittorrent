import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ListDecoder implements Decoder<List<Object>> {
	private final DecoderDispatcher dispatcher;

	public ListDecoder(DecoderDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	/**
	 * Decodes a bencoded list from the input string starting at the given index.
	 * Repeatedly calls the dispatcher to decode each element until it finds 'e'.
	 * The list is expected to be in the format "l<element1><element2>...e".
	 * For example, "l4:spam4:eggs" means a list with two strings: "spam" and "eggs".
	 *
	 * @param input      The bencoded string to decode.
	 * @param startIndex The index to start decoding from.
	 * @return
	 */
	public DecoderDTO<List<Object>> decode(String input,
			int startIndex) throws IllegalArgumentException {
		validateInput(input, startIndex, 'l');

		List<Object> list = new ArrayList<>();
		int index = startIndex + 1; // Skip 'l'

		while (input.charAt(index) != 'e') {
			DecoderDTO<?> element = dispatcher.decode(input, index);
			list.add(element.getValue());
			// index is updated in the decode method based on decoded type
			index = element.getNextIndex();
		}

		return new DecoderDTO<List<Object>>(list, index + 1); // Skip 'e'
	}

	@Override
	public DecoderDTO<List<Object>> decode(byte[] bencodedBytes,
			int startIndex, TorrentInfoDTO infoDTO) throws RuntimeException {
		validateInput(bencodedBytes, startIndex, 'l');

		List<Object> list = new ArrayList<>();
		int index = startIndex + 1; // Skip 'l'
		while (bencodedBytes[index] != 'e') {
			DecoderDTO<?> element = dispatcher.decode(bencodedBytes, index, infoDTO);
			list.add(element.getValue());
			// index is updated in the decode method based on decoded type
			index = element.getNextIndex();
		}
		return new DecoderDTO<List<Object>>(list, index + 1); // Skip 'e'
	}
}
