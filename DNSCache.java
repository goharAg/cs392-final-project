
import java.util.HashMap;
import java.util.Map;

public class DNSCache {
  private final Map<String, String> cache = new HashMap<>();

  public void put(String domainName, String ipAddress) {
    cache.put(domainName, ipAddress);
  }

  public String get(String domainName) {
    return cache.get(domainName);
  }

  public void remove(String domainName) {
    cache.remove(domainName);
  }

  public void clear() {
    cache.clear();
  }

  public Map<String, String> getAll() {
    return new HashMap<>(cache);
  }
}
