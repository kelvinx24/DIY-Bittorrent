import java.util.Optional;

public interface Decoder<T> {
	DecoderDTO<T> decode(String bencodedString, int startIndex) throws RuntimeException;
	DecoderDTO<T> decode(byte[] bencodedBytes, int startIndex, TorrentInfoDTO infoDTO) throws RuntimeException;
}
