import java.util.LinkedHashMap;
import java.util.Map;

public class DecoderByteDTO<T> {
  private final DecoderDTO<T> decoderDTO;
  private final Map<String, NumberPair> byteRanges;

  private final int valueStartIndex;

  public DecoderByteDTO(DecoderDTO<T> decoderDTO, Map<String, NumberPair> byteRanges, int valueStartIndex) {
    this.decoderDTO = decoderDTO;
    this.byteRanges = byteRanges;
    this.valueStartIndex = valueStartIndex;
  }

  public DecoderByteDTO(DecoderDTO<T> decoderDTO) {
    this(decoderDTO, new LinkedHashMap<>(), -1);
  }

  public DecoderByteDTO(DecoderByteDTO<T> decoderByteDTO) {
    this(decoderByteDTO.getDecoderDTO(), new LinkedHashMap<>(decoderByteDTO.getByteRanges()), decoderByteDTO.valueStartIndex);
  }

  public DecoderByteDTO(T value, int nextIndex) {
    this(new DecoderDTO<>(value, nextIndex), new LinkedHashMap<>(), -1);
  }


  public DecoderDTO<T> getDecoderDTO() {
    return decoderDTO;
  }

  public Map<String, NumberPair> getByteRanges() {
    return byteRanges;
  }

  public void addByteRange(String key, int start, int end) {
    this.byteRanges.put(key, new NumberPair(start, end));
  }

  public NumberPair getByteRange(String key) {
    return this.byteRanges.get(key);
  }

  public NumberPair getInfoByteRange() {
    return this.byteRanges.get("info");
  }

  public int getNextIndex() {
    return this.decoderDTO.getNextIndex();
  }

  public T getValue() {
    return this.decoderDTO.getValue();
  }

  public int getValueStartIndex() {
    return valueStartIndex;
  }
}
