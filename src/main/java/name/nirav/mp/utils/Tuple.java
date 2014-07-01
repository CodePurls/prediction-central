package name.nirav.mp.utils;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;

public class Tuple<K, V> extends SimpleImmutableEntry<K, V> implements Entry<K, V> {
  private static final long serialVersionUID = 1L;

  public Tuple(K k, V v) {
    super(k, v);
  }

  public static <K, V> Tuple<K, V> of(K k, V v) {
    return new Tuple<K, V>(k, v);
  }
}
