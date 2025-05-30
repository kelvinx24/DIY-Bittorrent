import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DecoderByteDTO is a data transfer object that encapsulates the details of a byte decoder. It
 * contains a {@link DecoderDTO}, a map of byte ranges, and a value range.
 *
 * @param <T> the type of value that the decoder handles
 * @author KX
 */
public class DecoderByteDTO<T> {

  private final DecoderDTO<T> decoderDTO;
  private final Map<String, NumberPair> byteRanges;

  /**
   * The value range indicates the start and end indices of the value within the byte array. This is
   * useful for tracking where the actual value is located in the byte data.
   */
  private final NumberPair valueRange;

  /**
   * Constructor for DecoderByteDTO.
   *
   * @param decoderDTO the {@link DecoderDTO} containing the decoded value and next index
   * @param byteRanges a map of byte ranges associated with the decoded value
   * @param valueRange the range of indices that represent the value within the byte array
   */
  public DecoderByteDTO(DecoderDTO<T> decoderDTO, Map<String, NumberPair> byteRanges,
      NumberPair valueRange) {
    this.decoderDTO = decoderDTO;
    this.byteRanges = byteRanges;
    this.valueRange = valueRange;
  }

  /**
   * Constructor for DecoderByteDTO with an empty byte range map.
   *
   * @param decoderDTO the DecoderDTO containing the decoded value and next index
   * @param valueRange the range of indices that represent the value within the byte array
   */
  public DecoderByteDTO(DecoderDTO<T> decoderDTO, NumberPair valueRange) {
    this(decoderDTO, new LinkedHashMap<>(), valueRange);
  }

  /**
   * Copy constructor for DecoderByteDTO. This creates a new instance with the same values as the
   * provided decoderByteDTO.
   *
   * @param decoderByteDTO the DecoderByteDTO to copy
   */
  public DecoderByteDTO(DecoderByteDTO<T> decoderByteDTO) {
    this(decoderByteDTO.getDecoderDTO(), new LinkedHashMap<>(decoderByteDTO.getByteRanges()),
        decoderByteDTO.valueRange);
  }

  /**
   * Constructor for DecoderByteDTO with a value and its associated indices. This is useful when you
   * have a value and want to specify values within {@link DecoderDTO} and value range.
   *
   * @param value           the decoded value
   * @param nextIndex       the index to continue decoding from after this value
   * @param valueStartIndex the start index of the value in the byte array
   * @param valueEndIndex   the end index of the value in the byte array
   */
  public DecoderByteDTO(T value, int nextIndex, int valueStartIndex, int valueEndIndex) {
    this(new DecoderDTO<>(value, nextIndex), new LinkedHashMap<>(),
        new NumberPair(valueStartIndex, valueEndIndex));
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

  public NumberPair getValueRange() {
    return this.valueRange;
  }
}
