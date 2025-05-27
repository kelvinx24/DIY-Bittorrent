public class RandomAlphaPeerIdGenerator implements RandomIdGenerator {
  public String generate() {
    StringBuilder builder = new StringBuilder(20);
    for (int i = 0; i < 20; i++) {
      builder.append((char) ('a' + (int)(Math.random() * 26)));
    }
    return builder.toString();
  }
}