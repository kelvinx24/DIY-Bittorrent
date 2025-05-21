import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class DecoderTests {

  private final DecoderDispatcher dispatcher = new DecoderDispatcher();

  @Test
  public void testNumberDecoder() {
    Decoder<Integer> intDecoder = new NumberDecoder();
    DecoderDTO<Integer> result = intDecoder.decode("i42e", 0);
    assertEquals(42, result.getValue());
    assertEquals(4, result.getNextIndex());
  }

  @Test
  public void testStringDecoder() {
    Decoder<String> strDecoder = new TextDecoder();
    DecoderDTO<String> result = strDecoder.decode("4:spam", 0);
    assertEquals("spam", result.getValue());
    assertEquals(6, result.getNextIndex());

    // Test 0 length string
    result = strDecoder.decode("0:", 0);
    assertEquals("", result.getValue());
    assertEquals(2, result.getNextIndex());
  }

  @Test
  public void testListDecoder() {
    Decoder<List<Object>> listDecoder = new ListDecoder(dispatcher);
    DecoderDTO<List<Object>> result = listDecoder.decode("l4:spam4:eggse", 0);
    List<?> list = result.getValue();

    assertEquals(2, list.size());
    assertEquals("spam", list.get(0));
    assertEquals("eggs", list.get(1));
    assertEquals(14, result.getNextIndex());
  }

  @Test
  public void testDictionaryDecoder() {
    Decoder<Map<String, Object>> dictDecoder = new DictionaryDecoder(dispatcher);
    String input = "d3:bar4:spam3:fooi42ee";
    DecoderDTO<Map<String, Object>> result = dictDecoder.decode(input, 0);
    Map<?, ?> dict =  result.getValue();

    assertEquals(2, dict.size());
    assertEquals("spam", dict.get("bar"));
    assertEquals(42, dict.get("foo"));
    assertEquals(input.length(), result.getNextIndex());
  }

  @Test
  public void testNestedStructures() {
    // Dictionary with list and integer: d4:listl4:spami7ee3:numi10ee
    String input = "d4:listl4:spami7ee3:numi10ee";
    DecoderDTO<?> result = dispatcher.decode(input, 0);
    Map<?, ?> dict = (Map<?, ?>) result.getValue();

    assertTrue(dict.containsKey("list"));
    List<?> list = (List<?>) dict.get("list");
    assertEquals("spam", list.get(0));
    assertEquals(7, list.get(1));

    assertEquals(10, dict.get("num"));
    assertEquals(input.length(), result.getNextIndex());
  }

  @Test
  public void testDispatcherWithAllTypes() {
    assertEquals(123, dispatcher.decode("i123e", 0).getValue());
    assertEquals("hello", dispatcher.decode("5:hello", 0).getValue());
    assertInstanceOf(List.class, dispatcher.decode("l5:apple6:bananai99ee", 0).getValue());
    assertInstanceOf(Map.class, dispatcher.decode("d3:key5:valuee", 0).getValue());
  }

  @Test
  public void testDispatcherWithInvalidInput() {
    Exception ex = assertThrows(IllegalArgumentException.class, () ->
        dispatcher.decode("x42e", 0) // invalid type prefix
    );
    assertEquals("Unknown bencode type at index 0", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () ->
        dispatcher.decode((String) null, 0) // null input
    );
    assertEquals("Input string cannot be null or empty", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () ->
        dispatcher.decode("", 0)
    );
    assertEquals("Input string cannot be null or empty", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () ->
        dispatcher.decode("i42e", -1) // negative index
    );
    assertEquals("Start index out of bounds: -1", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () ->
        dispatcher.decode("i42e", 10) // index out of bounds
    );
    assertEquals("Start index out of bounds: 10", ex.getMessage());

  }

  @Test
  public void testUnclosedTypes() {
    Exception ex = assertThrows(IllegalArgumentException.class, () ->
        dispatcher.decode("i42", 0) // missing 'e' for integer
    );

    assertEquals("Invalid bencoded number: missing 'e' at index 0", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () ->
        dispatcher.decode("l4:spam4:eggs", 0) // missing 'e' for list
    );
    assertEquals("Invalid bencoded list: missing 'e' at index 0", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () ->
        dispatcher.decode("d3:spai42e", 0) // missing 'e' for dictionary
    );

    assertEquals("Invalid bencoded dictionary: missing 'e' at index 0", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () ->
        dispatcher.decode("10:spam", 0)
    );
    assertEquals("String content exceeds input bounds.", ex.getMessage());
  }

  @Test
  public void testByteArrayNumberDecoding() {
    byte[] bencodedBytes = "i42e".getBytes();
    DecoderByteDTO<?> result = dispatcher.decode(bencodedBytes, 0);
    assertEquals(42, result.getDecoderDTO().getValue());
    assertEquals(4, result.getNextIndex());
  }

  @Test
  public void testByteArrayStringDecoding() {
    byte[] bencodedBytes = "4:spam".getBytes();
    DecoderByteDTO<?> result = dispatcher.decode(bencodedBytes, 0);
    assertEquals("spam", result.getDecoderDTO().getValue());
    assertEquals(6, result.getNextIndex());

    // Test 0 length string
    bencodedBytes = "0:".getBytes();
    result = dispatcher.decode(bencodedBytes, 0);
    assertEquals("", result.getDecoderDTO().getValue());
    assertEquals(2, result.getNextIndex());
  }

  @Test
  public void testByteArrayListDecoding() {
    byte[] bencodedBytes = "l4:spam4:eggse".getBytes();
    DecoderByteDTO<?> result = dispatcher.decode(bencodedBytes, 0);
    List<?> list = (List<?>) result.getDecoderDTO().getValue();

    assertEquals(2, list.size());
    assertEquals("spam", list.get(0));
    assertEquals("eggs", list.get(1));
    assertEquals(14, result.getNextIndex());
  }

  @Test
  public void testByteArrayDictionaryDecoding() {
    byte[] bencodedBytes = "d3:bar4:spam3:fooi42ee".getBytes();
    DecoderByteDTO<?> result = dispatcher.decode(bencodedBytes, 0);
    Map<?, ?> dict = (Map<?, ?>) result.getDecoderDTO().getValue();

    assertEquals(2, dict.size());
    assertEquals("spam", dict.get("bar"));
    assertEquals(42, dict.get("foo"));
    assertEquals(bencodedBytes.length, result.getNextIndex());

    String input = "d4:listl4:spami7ee3:numi10ee";
    result = dispatcher.decode(input.getBytes(), 0);
    Map<?, ?> dict2 = (Map<?, ?>) result.getDecoderDTO().getValue();

    assertTrue(dict2.containsKey("list"));
    List<?> list2 = (List<?>) dict2.get("list");
    assertEquals("spam", list2.get(0));
    assertEquals(7, list2.get(1));

    assertEquals(10, dict2.get("num"));
    assertEquals(input.length(), result.getNextIndex());
  }

  @Test
  public void testParsingByteRanges() {
    byte[] bencodedBytes = "i42e".getBytes();
    DecoderByteDTO<?> result = dispatcher.decode(bencodedBytes, 0);
    assertEquals(0, result.getByteRanges().size());

    bencodedBytes = "4:spam".getBytes();
    result = dispatcher.decode(bencodedBytes, 0);
    assertEquals(0, result.getByteRanges().size());

    bencodedBytes = "l4:spam4:eggse".getBytes();
    result = dispatcher.decode(bencodedBytes, 0);
    assertEquals(0, result.getByteRanges().size());

    bencodedBytes = "d3:bar4:spam3:fooi42ee".getBytes();
    result = dispatcher.decode(bencodedBytes, 0);
    assertEquals(2, result.getByteRanges().size());
    assertTrue(result.getByteRanges().containsKey("bar"));
    assertTrue(result.getByteRanges().containsKey("foo"));

    NumberPair barRange = result.getByteRanges().get("bar");
    // Add 1 to the end index since the end index is exclusive for Arrays.copyOfRange
    byte[] barBytes = Arrays.copyOfRange(bencodedBytes, barRange.first(), barRange.second() + 1);
    assertEquals("spam", new String(barBytes));

    NumberPair fooRange = result.getByteRanges().get("foo");
    byte[] fooBytes = Arrays.copyOfRange(bencodedBytes, fooRange.first(), fooRange.second() + 1);
    assertEquals("42", new String(fooBytes));

    // Nested dictionary
    bencodedBytes = "d4:dictd3:bari42eee".getBytes();
    result = dispatcher.decode(bencodedBytes, 0);
    NumberPair dictRange = result.getByteRanges().get("dict");
    byte[] dictBytes = Arrays.copyOfRange(bencodedBytes, dictRange.first(), dictRange.second() + 1);

    assertEquals("3:bari42e", new String(dictBytes));
    assertEquals(2, result.getByteRanges().size());

    // Nested list
    bencodedBytes = "d4:listl4:spami7ee3:numi10ee".getBytes();
    result = dispatcher.decode(bencodedBytes, 0);
    NumberPair listRange = result.getByteRanges().get("list");
    byte[] listBytes = Arrays.copyOfRange(bencodedBytes, listRange.first(), listRange.second() + 1);
    assertEquals("4:spami7e", new String(listBytes));
    assertEquals(2, result.getByteRanges().size());

  }

}
