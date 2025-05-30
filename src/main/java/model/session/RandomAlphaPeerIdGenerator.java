package model.session;

/**
 * Randomly generates a random peer ID consisting of 20 lowercase alphabetic characters.
 * This class implements the {@link RandomIdGenerator} interface.
 *
 * @author KX
 */
public class RandomAlphaPeerIdGenerator implements RandomIdGenerator {
  public String generate() {
    StringBuilder builder = new StringBuilder(20);
    for (int i = 0; i < 20; i++) {
      builder.append((char) ('a' + (int)(Math.random() * 26)));
    }
    return builder.toString();
  }
}