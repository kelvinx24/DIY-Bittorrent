public interface Decoder<T> {
	DecoderDTO decode(String bencodedString, int startIndex) throws RuntimeException;
}
