public interface Decoder<T> {
	T decode(String bencodedString) throws RuntimeException;

	boolean isValid(String bencodedString);
}
