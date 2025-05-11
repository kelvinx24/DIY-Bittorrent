public class TextDecoder implements Decoder<String> {

	private int colonIndex = 0;
	private int textLength = 0;

	@Override
	public String decode(String bencodedString) throws RuntimeException {
		if (Character.isDigit(bencodedString.charAt(0))) {
			colonIndex = 0;
			for (int i = 0; i < bencodedString.length(); i++) {
				if (bencodedString.charAt(i) == ':') {
					colonIndex = i;
					break;
				}
			}
			int textLength = Integer.parseInt(bencodedString.substring(0, colonIndex));

			assert bencodedString.length() == colonIndex + 1 + textLength;
			return bencodedString.substring(colonIndex + 1, colonIndex + 1 + textLength);
		}
		else {
			throw new RuntimeException("Only strings are supported at the moment");
		}
	}

	@Override
	public boolean isValid(String bencodedString) {
		if (bencodedString == null || bencodedString.isEmpty()) {
			return false;
		}
		if (Character.isDigit(bencodedString.charAt(0))) {
			int firstColonIndex = 0;
			for (int i = 0; i < bencodedString.length(); i++) {
				if (bencodedString.charAt(i) == ':') {
					firstColonIndex = i;
					break;
				}
			}
			int length = Integer.parseInt(bencodedString.substring(0, firstColonIndex));
			return bencodedString.length() == firstColonIndex + 1 + length;
		}
		else {
			return false;
		}
	}

	public int getTextLength() {
		return textLength;
	}

	public int getColonIndex() {
		return colonIndex;
	}
}
