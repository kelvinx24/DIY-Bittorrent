import model.session.RandomIdGenerator;

/**
 * Mock implementation of {@link RandomIdGenerator} that always returns a fixed ID.
 */
public class MockIdGenerator implements RandomIdGenerator {

  @Override
  public String generate() {
    return "0".repeat(20);
  }
}
