package name.nirav.mp.service.search;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import name.nirav.mp.service.dto.Source;
import name.nirav.mp.service.search.Schema.PredictionDoc;
import name.nirav.mp.utils.TimeUtils;

import org.apache.lucene.document.Document;
import org.apache.lucene.facet.params.FacetSearchParams;
import org.apache.lucene.facet.range.LongRange;
import org.apache.lucene.facet.range.RangeAccumulator;
import org.apache.lucene.facet.range.RangeFacetRequest;
import org.apache.lucene.facet.search.FacetResult;
import org.apache.lucene.facet.search.FacetResultNode;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.suggest.Lookup;

import com.google.common.base.Preconditions;

public class SearchService {
  private final IndexingService indexingService;

  public SearchService(IndexingService indexingService) throws IOException {
    this.indexingService = indexingService;
  }

  public List<WordsWithFreq> suggest(String q, int n) {
    if (q == null) return Collections.emptyList();
    List<Lookup.LookupResult> lookupResults = indexingService.lookupSuggestion(q, false, n);
    List<WordsWithFreq> list = new ArrayList<>(n);
    for (Lookup.LookupResult lookupResult : lookupResults) {
      list.add(new WordsWithFreq(lookupResult.key, lookupResult.value));
    }
    return list;
  }

  public Map<String, FacetedSearchResults> getCountsForLastNDays(int n) {
    return getCountsForLastNDays(n, null);
  }

  public Map<String, FacetedSearchResults> getCountsForLastNDays(Integer days, String field) {
    return aggregateByDateField(days, 0, field);
  }

  public Map<String, FacetedSearchResults> aggregateByDateField(int start, int end, String field) {
    if (field == null) {
      field = PredictionDoc.CREATED_ON.fieldName();
    }
    Map<String, FacetedSearchResults> resultsMap = new HashMap<>();
    try {
      resultsMap.put("ALL", runFaceted(new MatchAllDocsQuery(), start, end, field));
      for (Source source : Source.values()) {
        NumericRangeQuery<Integer> query;
        if (source == Source.WEB) {
          query = NumericRangeQuery.newIntRange(PredictionDoc.CREATED_BY.fieldName(), Source.GOOGLEPLUS.getId(), Integer.MAX_VALUE, true, false);
        } else {
          query = NumericRangeQuery.newIntRange(PredictionDoc.CREATED_BY.fieldName(), source.getId(), source.getId(), true, true);
        }
        resultsMap.put(source.name(), runFaceted(query, start, end, field));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return resultsMap;
  }

  public FacetedSearchResults aggregateBySource() throws IOException {
    FacetedSearchResults results = new FacetedSearchResults(PredictionDoc.CREATED_BY.fieldName());
    List<LongRange> groups = new ArrayList<>(4);
    for (Source source : Source.values()) {
      if (source == Source.WEB) {
        groups.add(new LongRange(source.getPrettyName(), Source.GOOGLEPLUS.getId(), true, Long.MAX_VALUE, false));
      } else {
        groups.add(new LongRange(source.getPrettyName(), source.getId(), true, source.getId(), true));
      }
    }
    IndexSearcher searcher = indexingService.getSearcher();
    RangeFacetRequest<LongRange> facetRequest = new RangeFacetRequest<>(PredictionDoc.CREATED_BY.fieldName(), groups);
    FacetSearchParams fsp = new FacetSearchParams(facetRequest);
    RangeAccumulator accum = new RangeAccumulator(fsp, searcher.getIndexReader());
    FacetsCollector collector = FacetsCollector.create(accum);
    searcher.search(new MatchAllDocsQuery(), collector);

    List<FacetResult> facetResults = collector.getFacetResults();
    for (FacetResult r : facetResults) {
      List<FacetResultNode> subResults = r.getFacetResultNode().subResults;
      for (FacetResultNode subResult : subResults) {
        results.getResults().add(new AbstractMap.SimpleEntry<>(subResult.label.toString(), Math.round(subResult.value)));
      }
    }
    return results;
  }

  private FacetedSearchResults runFaceted(Query query, int start, int end, String field) throws IOException {
    IndexSearcher searcher = indexingService.getSearcher();
    FacetedSearchResults res = new FacetedSearchResults(field);
    List<LongRange> days = new ArrayList<>(60);
    start = start == 0 ? 0 : -start;
    end = end == 0 ? 0 : -end;
    Preconditions.checkArgument(start <= end);
    long last = Math.max(TimeUtils.getDayFromToday(end), Long.MIN_VALUE);
    for (int i = start; i < end; i++) {
      long startDay = TimeUtils.getDayFromToday(i);
      long endDay = TimeUtils.getDayFromToday(i + 1);
      days.add(new LongRange(TimeUtils.readableDate(startDay), startDay, true, endDay, false));
      last = endDay;
    }
    days.add(new LongRange(String.format("%s", TimeUtils.readableDate(last)), last, true, Long.MAX_VALUE, false));
    RangeFacetRequest<LongRange> facetRequest = new RangeFacetRequest<>(field, days);
    FacetSearchParams fsp = new FacetSearchParams(facetRequest);
    RangeAccumulator accum = new RangeAccumulator(fsp, searcher.getIndexReader());
    FacetsCollector collector = FacetsCollector.create(accum);
    searcher.search(query, collector);
    List<FacetResult> facetResults = collector.getFacetResults();
    for (FacetResult r : facetResults) {
      List<FacetResultNode> subResults = r.getFacetResultNode().subResults;
      for (FacetResultNode subResult : subResults) {
        res.getResults().add(new AbstractMap.SimpleEntry<>(subResult.label.toString(), Math.round(subResult.value)));
      }
    }
    return res;
  }

  public SearchResults search(String q, int size) {
    try {
      IndexSearcher searcher = indexingService.getSearcher();
      TermQuery queryText = new TermQuery(new Term(PredictionDoc.TEXT.fieldName(), q));
      TermQuery queryAuthor = new TermQuery(new Term(PredictionDoc.SOURCE_AUTHOR.fieldName(), q));
      BooleanQuery query = new BooleanQuery();
      query.add(queryText, BooleanClause.Occur.SHOULD);
      query.add(queryAuthor, BooleanClause.Occur.SHOULD);
      TopDocs docs = searcher.search(query, size);
      SearchResults res = new SearchResults();
      res.hits = docs.totalHits;
      ScoreDoc[] scoreDocs = docs.scoreDocs;
      List<Integer> list = new ArrayList<>(size);
      Set<Integer> dups = new HashSet<>();
      for (ScoreDoc scoreDoc : scoreDocs) {
        Document doc = searcher.doc(scoreDoc.doc);
        int pId = Integer.valueOf(doc.getField(PredictionDoc.ID.fieldName()).stringValue());
        if (dups.contains(pId)) continue;
        dups.add(pId);
        list.add(pId);
      }
      res.predictions = list;
      return res;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<WordsWithFreq> getTopTags(int top, Schema field) {
    return indexingService.getTopTags(top, field);
  }
}
