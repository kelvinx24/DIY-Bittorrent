public class NumberDecoder implements Decoder<Long>{
	private int endindex = 0;

	@Override
	public Long decode(String bencodedString) throws RuntimeException {
		if (bencodedString.charAt(0) == 'i') {
			endindex = bencodedString.indexOf('e');
			if (endindex != bencodedString.length() - 1) {
				throw new RuntimeException("Invalid bencoded string");
			}
			else {
				return Long.parseLong(bencodedString.substring(1, endindex));
			}
		}
		else {
			throw new RuntimeException("Only integers are supported at the moment");
		}
	}

	@Override
	public boolean isValid(String bencodedString) {
		if (bencodedString == null || bencodedString.isEmpty()) {
			return false;
		}
		if (bencodedString.charAt(0) == 'i') {
			int endindex = bencodedString.indexOf('e');
			return endindex != -1 && endindex == bencodedString.length() - 1;
		}
		else {
			return false;
		}
	}
}
