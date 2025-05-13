import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class DecoderTests {

	private final DecoderDispatcher dispatcher = new DecoderDispatcher();

	@Test
	public void testNumberDecoder() {
		Decoder intDecoder = new NumberDecoder();
		DecoderDTO result = intDecoder.decode("i42e", 0);
		assertEquals(42, result.getValue());
		assertEquals(4, result.getNextIndex());
	}

	@Test
	public void testStringDecoder() {
		Decoder strDecoder = new TextDecoder();
		DecoderDTO result = strDecoder.decode("4:spam", 0);
		assertEquals("spam", result.getValue());
		assertEquals(6, result.getNextIndex());
	}

	@Test
	public void testListDecoder() {
		Decoder listDecoder = new ListDecoder(dispatcher);
		DecoderDTO result = listDecoder.decode("l4:spam4:eggse", 0);
		List<?> list = (List<?>) result.getValue();

		assertEquals(2, list.size());
		assertEquals("spam", list.get(0));
		assertEquals("eggs", list.get(1));
		assertEquals(14, result.getNextIndex());
	}

	@Test
	public void testDictionaryDecoder() {
		Decoder dictDecoder = new DictionaryDecoder(dispatcher);
		String input = "d3:bar4:spam3:fooi42ee";
		DecoderDTO result = dictDecoder.decode(input, 0);
		Map<?, ?> dict = (Map<?, ?>) result.getValue();

		assertEquals(2, dict.size());
		assertEquals("spam", dict.get("bar"));
		assertEquals(42, dict.get("foo"));
		assertEquals(input.length(), result.getNextIndex());
	}

	@Test
	public void testNestedStructures() {
		// Dictionary with list and integer: d4:listl4:spami7ee3:numi10ee
		String input = "d4:listl4:spami7ee3:numi10ee";
		DecoderDTO result = dispatcher.decode(input, 0);
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
		assertTrue(dispatcher.decode("l5:apple6:bananai99ee", 0).getValue() instanceof List);
		assertTrue(dispatcher.decode("d3:key5:valuee", 0).getValue() instanceof Map);
	}

	@Test
	public void testInvalidInput() {
		Exception ex = assertThrows(IllegalArgumentException.class, () -> {
			dispatcher.decode("x42e", 0); // invalid type prefix
		});
		assertTrue(ex.getMessage().contains("Unknown bencode type"));
	}
}
