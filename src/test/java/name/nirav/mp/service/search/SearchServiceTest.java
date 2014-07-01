package name.nirav.mp.service.search;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import name.nirav.mp.config.OpenCalaisConfig;
import name.nirav.mp.config.SearchConfiguration;
import name.nirav.mp.db.PredictionDB;
import name.nirav.mp.service.analytics.EntityExtractionService;

import org.apache.lucene.util.Version;
import org.junit.Test;

/**
 * @author Nirav Thaker
 */
public class SearchServiceTest {
  @Test
  public void testFacetedSearch() throws IOException {
    SearchService service = getSearchService();
    Map<String, FacetedSearchResults> results = service.getCountsForLastNDays(1);
    assertEquals(2, results.size());
    results = service.getCountsForLastNDays(30);
    assertEquals(31, results.size());
  }

  @SuppressWarnings("unused")
  private SearchService getSearchService(boolean initSuggest) throws IOException {
    SearchConfiguration conf = new SearchConfiguration();
    PredictionDB db = null;
    IndexingService indexingService = new IndexingService(conf, db, new EntityExtractionService(new OpenCalaisConfig(), db));
    if (initSuggest) {
      return new SearchService(indexingService) {
        protected void scheduleIndexRefresh() {
        }
      };
    } else {
      return new SearchService(indexingService) {
        protected void scheduleIndexRefresh() {
        }

        protected void initSuggester(File indexDir, Version version) throws IOException {
        }
      };
    }
  }

  private SearchService getSearchService() throws IOException {
    return getSearchService(false);
  }

  @Test
  public void testAutoComplete() throws IOException {
    SearchService service = getSearchService(true);
    assertEquals(2, service.suggest("pre", 2).size());
  }

  @Test
  public void testRangeSearch() throws Exception {
    SearchService service = getSearchService();
    FacetedSearchResults results = service.aggregateBySource();
    System.out.println(results);

  }
}
