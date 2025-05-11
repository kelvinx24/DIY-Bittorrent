import java.util.ArrayList;
import java.util.List;

public class ListDecoder implements Decoder<List<Object>>{

	private int trackingIndex = 0;

	@Override
	public List<Object> decode(String bencodedString) throws RuntimeException {
		if (bencodedString.charAt(0) == 'l' && bencodedString.charAt(bencodedString.length() - 1) == 'e') {
			trackingIndex = 1; // Skip the initial 'l'
			return parseList(bencodedString);
		} else {
			throw new RuntimeException("Invalid bencoded string");
		}
	}

	private List<Object> parseList(String bencodedString) {
		List<Object> list = new ArrayList<>();

		while (bencodedString.charAt(trackingIndex) != 'e') {
			char currentChar = bencodedString.charAt(trackingIndex);
			if (currentChar == 'i') {
				int endIndex = bencodedString.indexOf('e', trackingIndex);
				//list.add(Long.parseLong(bencodedString.substring(i + 1, endIndex)));
				NumberDecoder numDecoder = new NumberDecoder();

				Object value = numDecoder.decode(bencodedString.substring(trackingIndex, endIndex + 1));
				list.add(value);

				trackingIndex = endIndex + 1;
			} else if (Character.isDigit(currentChar)) {
				int firstColonIndex = bencodedString.indexOf(':', trackingIndex);
				int length = Integer.parseInt(bencodedString.substring(trackingIndex, firstColonIndex));
				list.add(bencodedString.substring(firstColonIndex + 1, firstColonIndex + 1 + length));
				trackingIndex = firstColonIndex + 1 + length;
			} else if (currentChar == 'l') {
				list.add(parseList(bencodedString));
			} else {
				throw new RuntimeException("Invalid bencoded string");
			}
		}
		return list;
	}

	@Override
	public boolean isValid(String bencodedString) {
		return bencodedString != null && !bencodedString.isEmpty() && bencodedString.charAt(0) == 'l';
	}
}
