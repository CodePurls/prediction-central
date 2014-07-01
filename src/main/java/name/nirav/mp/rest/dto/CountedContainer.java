/**
 * 
 */
package name.nirav.mp.rest.dto;

import java.util.Collection;

/**
 * @author Nirav Thaker <nirav.thaker@gmail.com>
 *
 */
public class CountedContainer<E> {
  private final Collection<E> results;
  private final int           size;
  private final int           pages;

  public CountedContainer(Collection<E> r, int total, int pageSize) {
    results = r;
    size = total;
    pages = (size / pageSize) + ((size % pageSize) > 0 ? 1 : 0);
  }

  public Collection<E> getResults() {
    return results;
  }

  public int getSize() {
    return size;
  }

  public int getPages() {
    return pages;
  }

  public static <E> CountedContainer<E> wrap(Collection<E> col, int total, int pageSize) {
    return new CountedContainer<>(col, total, pageSize);
  }
}
