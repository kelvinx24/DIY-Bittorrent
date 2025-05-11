import com.google.gson.Gson;
// import com.dampcake.bencode.Bencode; - available if you need it!

public class Main {
  private static final Gson gson = new Gson();

  public static void main(String[] args) throws Exception {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");
    
    String command = args[0];
    if("decode".equals(command)) {
      //  Uncomment this block to pass the first stage
      String bencodedValue = args[1];
      Object decoded;
      try {
       decoded = decodeBencode(bencodedValue);
      } catch(RuntimeException e) {
         System.out.println(e.getMessage());
         return;
      }

      /*
      if (bencodedValue.charAt(0) == 'i') {
        long decodedLong = Long.parseLong(decoded);
        System.out.println(gson.toJson(decodedLong));
      }
      else {
        System.out.println(gson.toJson(decoded));
      }

       */
      System.out.println(gson.toJson(decoded));


    } else {
      System.out.println("Unknown command: " + command);
    }

  }

  static Object decodeBencode(String bencodedString) {
    Decoder<?> decoder = null;
    if (Character.isDigit(bencodedString.charAt(0))) {
      decoder = new TextDecoder();
    }
    else if (bencodedString.charAt(0) == 'i' && bencodedString.charAt(bencodedString.length() - 1) == 'e') {
      decoder = new NumberDecoder();
    }
    else if (bencodedString.charAt(0) == 'l' && bencodedString.charAt(bencodedString.length() - 1) == 'e') {
      decoder = new ListDecoder();
    }
    else if (bencodedString.charAt(0) == 'd' && bencodedString.charAt(bencodedString.length() - 1) == 'e') {
      //decoder = new DictionaryDecoder();
    }
    else {
      throw new RuntimeException("Only strings are supported at the moment");
    }

    assert decoder != null;
    return decoder.decode(bencodedString);
  }
  
}
