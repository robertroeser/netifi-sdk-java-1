import net.openhft.hashing.LongHashFunction;
import org.junit.Test;
import reactor.core.publisher.Flux;

/** Created by robertroeser on 6/25/17. */
public class TestHashing {
  @Test
  public void test() {
    LongHashFunction xx = LongHashFunction.xx();

    long namespace = xx.hashChars("io.generated");
    long SearchService = xx.hashChars("SearchService");
    long SearchService2 = xx.hashChars("SearchService2");
    long rr = xx.hashChars("rr");
    long ff = xx.hashChars("ff");
    long streaming = xx.hashChars("streaming");
    long channel = xx.hashChars("channel");
    long channel2 = xx.hashChars("channel2");
    long search = xx.hashChars("search");
    long search2 = xx.hashChars("search2");

    System.out.println("namespace -> " + namespace);
    System.out.println("SearchService -> " + SearchService);
    System.out.println("SearchService2 -> " + SearchService2);
    System.out.println("rr -> " + rr);
    System.out.println("ff -> " + ff);
    System.out.println("stream -> " + streaming);
    System.out.println("channel -> " + channel);
    System.out.println("channel2 -> " + channel2);
    System.out.println("search -> " + search);
    System.out.println("search2 -> " + search2);
  }
  
  @Test
  public void test2() {
  
  }
}
