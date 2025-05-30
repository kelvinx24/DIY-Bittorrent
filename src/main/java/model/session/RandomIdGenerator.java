package model.session;

/**
 * A simple interface for generating random IDs.
 *
 * @author KX
 */
public interface RandomIdGenerator {

  /**
   * Generates a random ID.
   *
   * @return a randomly generated ID as a String.
   */
  String generate();
}