import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class DictionaryDecoder implements Decoder<Map<String, Object>> {

	private final DecoderDispatcher dispatcher;

	public DictionaryDecoder(DecoderDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	/**
	 * Decodes a bencoded dictionary from the input string starting at the given index.
	 * Repeatedly calls the dispatcher to decode each key-value pair until it finds 'e'.
	 * Recursively decodes the keys and values using the dispatcher.
	 * The dictionary is expected to be in the format "d<key1><value1><key2><value2>...e".
	 * For example, "d3:spami42e3:eggs4:spam" means a dictionary with two entries:
	 */
	public DecoderDTO<Map<String, Object>> decode(String input, int startIndex) {
		Map<String, Object> dict = new LinkedHashMap<>();
		int index = startIndex + 1; // Skip 'd'

		while (input.charAt(index) != 'e') {
			DecoderDTO<?> keyResult = dispatcher.decode(input, index);
			String key = (String) keyResult.getValue(); // Keys are always strings
			// Move the index to the end of the key
			index = keyResult.getNextIndex();

			DecoderDTO<?> valueResult = dispatcher.decode(input, index);
			dict.put(key, valueResult.getValue());
			// Move the index to the end of the value
			index = valueResult.getNextIndex();
		}

		return new DecoderDTO<Map<String, Object>>(dict, index + 1); // Skip 'e'
	}

	@Override
	public DecoderDTO<Map<String, Object>> decode(byte[] bencodedBytes, int startIndex,
			TorrentInfoDTO infoDTO) throws RuntimeException {
		if (bencodedBytes[startIndex] != 'd') {
			throw new RuntimeException("Invalid bencoded dictionary format");
		}

		Map<String, Object> dict = new LinkedHashMap<>();
		int index = startIndex + 1; // Skip 'd'
		int infoIndexStart = index; // Store the start index for info
		int infoIndexEnd = -1; // Initialize the end index for info


		while (bencodedBytes[index] != 'e') {
			DecoderDTO<?> keyResult = dispatcher.decode(bencodedBytes, index, infoDTO);
			String key = (String) keyResult.getValue(); // Keys are always strings
			// Move the index to the end of the key
			index = keyResult.getNextIndex();

			infoIndexStart = index;

			DecoderDTO<?> valueResult = dispatcher.decode(bencodedBytes, index, infoDTO);
			dict.put(key, valueResult.getValue());
			// Move the index to the end of the value
			index = valueResult.getNextIndex();

			if (key.equals("info") && infoDTO != null) {
				infoIndexEnd = index;
				infoDTO.setStartIndex(infoIndexStart);
				infoDTO.setEndIndex(infoIndexEnd);
			}

		}

		return new DecoderDTO<Map<String, Object>>(dict, index + 1); // Skip 'e'
	}

}
