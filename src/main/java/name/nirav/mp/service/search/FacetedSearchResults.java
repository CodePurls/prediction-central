package name.nirav.mp.service.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nirav Thaker
 */
public class FacetedSearchResults {
  private final String                  facetField;

  private List<Map.Entry<String, Long>> results = new ArrayList<>();

  public FacetedSearchResults(String facetField) {
    this.facetField = facetField;
  }

  public List<Map.Entry<String, Long>> getResults() {
    return results;
  }

  public void setResults(List<Map.Entry<String, Long>> results) {
    this.results = results;
  }

  public String getFacetField() {
    return facetField;
  }

}
