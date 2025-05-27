public class MockIdGenerator implements RandomIdGenerator {

  @Override
  public String generate() {
    return "0".repeat(20);
  }
}
