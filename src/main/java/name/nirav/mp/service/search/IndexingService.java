package name.nirav.mp.service.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.nirav.mp.config.SearchConfiguration;
import name.nirav.mp.db.PredictionDB;
import name.nirav.mp.service.analytics.EntityExtractionService;
import name.nirav.mp.service.analytics.EntityObjects;
import name.nirav.mp.service.dto.Prediction;
import name.nirav.mp.service.dto.PredictionIndexStatus.Status;
import name.nirav.mp.service.monitoring.MonitoringService;
import name.nirav.mp.service.search.Schema.EntityDoc;
import name.nirav.mp.service.search.Schema.PredictionDoc;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.spell.TermFreqIterator;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.yammer.metrics.core.TimerContext;

/**
 * @author Nirav Thaker
 */
public class IndexingService implements Runnable {
  private static final int                  INDEX_BATCH_SIZE          = 1000;
  private static final Logger               LOG                       = LoggerFactory.getLogger("Indexer");
  private static final Logger               ENTITY_LOG                = LoggerFactory.getLogger("EntityIndexer");
  private static final Pattern              WEBURL                    = Pattern
                                                                          .compile(
                                                                              "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)",
                                                                              Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
  private static final Comparator<BytesRef> comparator                = BytesRef::compareTo;

  private static final Object               LOCK                      = new Object();
  private static final Object               ENTITY_LOCK               = new Object();
  private static final long                 INACTIVITY_SLEEP_DURATION = Duration.ofMinutes(5).toMillis();
  private static final int                  OPTIMIZE_THRESHOLD        = 25000;

  private final Set<String>                 smartStopWords;
  private final SearchConfiguration         conf;
  private final PredictionDB                db;
  private final EntityExtractionService     entityExtractionService;
  private final Thread                      entityExtractionThread;

  private FSDirectory                       fsDir, entityDir;
  private StandardAnalyzer                  analyzer, entityAnalyzer;
  private IndexWriterConfig                 indexWriterConfig, entityIndexWriterConfig;
  private IndexWriter                       indexWriter, entityIndexWriter;

  private AnalyzingInfixSuggester           suggester;

  private int                               lastIndexedId             = -1, lastIndexedIdWithEntities = -1;
  private boolean                           keepRunning               = true;

  public IndexingService(SearchConfiguration conf, PredictionDB db, EntityExtractionService entityExtractionService) throws IOException {
    this.conf = conf;
    this.db = db;
    this.entityExtractionService = entityExtractionService;
    BufferedReader reader = new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("search/smart_stopwords.txt"), "UTF-8"));
    smartStopWords = new TreeSet<>();
    String line;
    while ((line = reader.readLine()) != null) {
      smartStopWords.add(line.trim());
    }
    init();
    if (entityExtractionService.isEnabled()) {
      entityExtractionThread = new Thread(this::runEntityExtraction, "entity-extraction");
      entityExtractionThread.start();
    } else {
      ENTITY_LOG.warn("Entity extraction service is disabled.");
      entityExtractionThread = null;
    }
  }

  private void init() throws IOException {
    File indexDir = new File(conf.getIndexDir(), "main");
    IndexWriterConfig.OpenMode mode = IndexWriterConfig.OpenMode.CREATE_OR_APPEND;
    Version version = getVersion();
    this.fsDir = FSDirectory.open(indexDir);
    this.analyzer = new StandardAnalyzer(version);
    this.indexWriterConfig = new IndexWriterConfig(version, analyzer);
    this.indexWriterConfig.setOpenMode(mode);
    this.indexWriterConfig.setRAMBufferSizeMB(4.0);
    this.indexWriter = new IndexWriter(fsDir, indexWriterConfig);
    initSuggester(indexDir.getParentFile(), version);
    initEntityIndex(indexDir.getParentFile());
    try {
      List<String> strings = Files.readLines(getCheckpointFile(), Charsets.UTF_8);
      lastIndexedId = Integer.parseInt(strings.get(0));
      lastIndexedIdWithEntities = Integer.parseInt(strings.get(1));
    } catch (Exception e) {
      LOG.warn("Unable to read checkpoint file, will ignore", e);
      checkpoint();
    }
    Thread t = new Thread(this, "indexer");
    t.start();
  }

  private void initEntityIndex(File indexDir) throws IOException {
    File entityIndexDir = new File(indexDir, "entity-index");
    if (!entityIndexDir.exists() && !entityIndexDir.mkdirs()) {
      ENTITY_LOG.error("Unable to create entity index {}", entityIndexDir);
    }
    this.entityAnalyzer = new StandardAnalyzer(getVersion());
    this.entityIndexWriterConfig = new IndexWriterConfig(getVersion(), entityAnalyzer);
    this.entityIndexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    this.entityIndexWriterConfig.setRAMBufferSizeMB(1.0);
    this.entityDir = FSDirectory.open(entityIndexDir);
    this.entityIndexWriter = new IndexWriter(entityDir, entityIndexWriterConfig);
  }

  protected void initSuggester(File indexDir, Version version) throws IOException {
    File suggestIndexDir = new File(indexDir, "suggest");
    if (!suggestIndexDir.exists() && !suggestIndexDir.mkdir()) {
      LOG.error("unable to create suggest index dir {}", suggestIndexDir);
    }
    suggester = new AnalyzingInfixSuggester(version, suggestIndexDir, this.analyzer);
  }

  public int reindex() throws FileNotFoundException {
    try {
      LOG.info("Purging index.");
      purgeIndex();
      LOG.info("Index purged.");
    } catch (Exception e) {
      LOG.error("Error purging index", e);
      throw new RuntimeException(e);
    }
    LOG.info("Resetting index status of all predictions.");
    db.resetIndexStatus(Status.UNINDEXED);
    LOG.info("Done.");
    checkpoint();
    return 0;

  }

  private void rebuildSuggestions() {
    for (PredictionDoc suggestField : PredictionDoc.SUGGEST_FIELDS) {
      List<WordsWithFreq> topTags = getTopTags(OPTIMIZE_THRESHOLD, suggestField);
      final Iterator<WordsWithFreq> iterator = topTags.iterator();
      TermFreqIterator dict = new TermFreqIterator() {
        WordsWithFreq next;

        public long weight() {
          return next.freq;
        }

        public BytesRef next() throws IOException {
          if (iterator.hasNext()) {
            this.next = iterator.next();
            return new BytesRef(next.word);
          }
          return null;
        }

        public Comparator<BytesRef> getComparator() {
          return comparator;
        }
      };
      try {
        suggester.build(dict);
      } catch (IOException e) {
        LOG.error("Error building dictionary", e);
      }
    }
  }

  private void optimizeIndex() throws IOException {
    LOG.info("Optimizing index");
    synchronized (LOCK) {
      reopenIndex();
      rebuildSuggestions();
    }
  }

  private void optimizeEntityIndex() throws IOException {
    synchronized (ENTITY_LOCK) {
      reopenEntityIndex();
    }
  }

  private void reopenEntityIndex() throws IOException {
    entityIndexWriter.commit();
    entityIndexWriter.forceMerge(1);
    entityIndexWriter.close(true);
    synchronized (ENTITY_LOCK) {
      entityIndexWriter = new IndexWriter(entityDir, entityIndexWriterConfig);
    }
  }

  private Version getVersion() {
    return Version.parseLeniently(conf.getVersion());
  }

  @Override
  public void run() {
    LockSupport.parkNanos(Duration.ofSeconds(10).toNanos());
    LOG.info("Starting indexer loop");
    long itemsIndexed = 0;
    while (keepRunning) {
      try {
        LOG.info("Retreiving {} predictions from id {}.", INDEX_BATCH_SIZE, lastIndexedId);
        List<Prediction> unindexedPredictions = db.getUnindexedPredictions(Status.UNINDEXED, INDEX_BATCH_SIZE);
        if (unindexedPredictions.isEmpty()) {
          LOG.info("No unindexed predictions, sleeping for five minutes.");
          Thread.sleep(INACTIVITY_SLEEP_DURATION);
        } else {
          LOG.info("Found {} unindexed predictions, will index.", unindexedPredictions.size());
          List<Integer> indexedIds = index(unindexedPredictions);
          itemsIndexed += indexedIds.size();
          lastIndexedId = indexedIds.get(indexedIds.size() - 1);
          checkpoint();
          LOG.info("Indexed {} predictions, last id is {}. Will mark it as {}", indexedIds.size(), lastIndexedId, Status.INDEXED_WITHOUT_ENTITIES);
          db.markIndexed(indexedIds, Status.INDEXED_WITHOUT_ENTITIES);
          LOG.info("Marked {} predictions as {}.", indexedIds.size(), Status.INDEXED_WITHOUT_ENTITIES);
          if (itemsIndexed > OPTIMIZE_THRESHOLD) {
            LOG.info("Optimizing index because too many items are indexed");
            optimizeIndex();
            itemsIndexed = 0;
          }
        }
      } catch (InterruptedException e) {
        Thread.interrupted();
        LOG.warn("Interrupted while indexing, probably shutting down");
      } catch (Exception e) {
        LOG.error("Error during indexing, will sleep one minute", e);
        LockSupport.parkNanos(Duration.ofMinutes(1).toNanos());
      }
    }
    LOG.info("Stopping indexing service.");
  }

  protected void runEntityExtraction() {
    LockSupport.parkNanos(Duration.ofSeconds(30).toNanos());
    ENTITY_LOG.info("Starting entity extraction loop");
    long itemsIndexed = 0;
    while (keepRunning) {
      try {
        ENTITY_LOG.info("Starting entity extraction and reindex from {} of {} predictions", lastIndexedIdWithEntities, INDEX_BATCH_SIZE);
        List<Integer> successfulIds = new ArrayList<>(INDEX_BATCH_SIZE);
        List<Prediction> list = db.getUnindexedPredictions(Status.INDEXED_WITHOUT_ENTITIES, INDEX_BATCH_SIZE);
        if (list.isEmpty()) {
          ENTITY_LOG.info("No unindexed predictions, sleeping for five minutes.");
          Thread.sleep(INACTIVITY_SLEEP_DURATION);
        } else {
          ENTITY_LOG.info("Found {} predictions with no entities.", list.size());
          for (Prediction prediction : list) {
            try {
              EntityObjects entities = null;
              try {
                entities = entityExtractionService.getEntities(prediction.getText());
              } catch (EntityExtractionService.UnsupportedLanguageException e) {
                successfulIds.add(prediction.getId());
              }
              if (entities == null) continue;
              Document doc = new Document();
              addSocialTags(doc, entities, prediction.getId());
              synchronized (ENTITY_LOCK) {
                this.entityIndexWriter.addDocument(doc);
              }
              successfulIds.add(prediction.getId());
              lastIndexedIdWithEntities = prediction.getId();
            } catch (Exception e) {
              ENTITY_LOG.error("Error adding social tags", e);
            }
          }
          ENTITY_LOG.info("Marking {} predictions as {}", successfulIds.size(), Status.INDEXED);
          db.markIndexed(successfulIds, Status.INDEXED);
          checkpoint();
          if (itemsIndexed > OPTIMIZE_THRESHOLD) {
            ENTITY_LOG.info("Optimizing entity index because too many items are indexed");
            optimizeEntityIndex();
            itemsIndexed = 0;
          }
        }
      } catch (InterruptedException e) {
        Thread.interrupted();
        ENTITY_LOG.warn("Interrupted while indexing, probably shutdown call.");
      } catch (Exception e) {
        ENTITY_LOG.error("Error during indexing entities will sleep one minute", e);
        LockSupport.parkNanos(Duration.ofMinutes(1).toNanos());
      }
    }
  }

  private void addSocialTags(Document doc, EntityObjects entities, int id) {
    if (entities == null) return;
    for (EntityObjects.SocialTag socialTag : entities.getSocialTags()) {
      if (socialTag.tag == null) continue;
      doc.add(new StringField(EntityDoc.SOCIAL_TAG.fieldName(), socialTag.tag, Field.Store.NO));
      doc.add(new SortedSetDocValuesField(EntityDoc.SOCIAL_TAG.fieldName(), new BytesRef(socialTag.tag)));
    }
    for (EntityObjects.Topic topic : entities.getTopics()) {
      if (topic.tag == null) continue;
      doc.add(new StringField(EntityDoc.TOPIC.fieldName(), topic.tag, Field.Store.NO));
      doc.add(new SortedSetDocValuesField(EntityDoc.TOPIC.fieldName(), new BytesRef(topic.tag)));
    }
    for (EntityObjects.Entity entity : entities.getEntities()) {
      if (entity.name == null) continue;
      doc.add(new StringField(entity.type, entity.name, Field.Store.NO));
      doc.add(new SortedSetDocValuesField(entity.type, new BytesRef(entity.name)));
    }
    doc.add(new StringField(EntityDoc.ID.fieldName(), Integer.toString(id), Field.Store.YES));
  }

  private void checkpoint() {
    File checkPointFile = getCheckpointFile();
    PrintWriter writer;
    try {
      writer = new PrintWriter(checkPointFile);
    } catch (FileNotFoundException e) {
      LOG.error("Error opening checkpoint file", e);
      throw new RuntimeException(e);
    }
    writer.println(Integer.toString(lastIndexedId));
    writer.println(Integer.toString(lastIndexedIdWithEntities));
    writer.close();
  }

  private File getCheckpointFile() {
    File indexDir = new File(conf.getIndexDir());
    return new File(indexDir, "p-check-point.txt");
  }

  private int index(Prediction p) {
    try {
      synchronized (LOCK) {
        indexWriter.addDocument(createDocument(p));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return p.getId();
  }

  private List<Integer> index(Collection<Prediction> predictions) {
    TimerContext context = MonitoringService.time(getClass(), "index-batch");
    List<Integer> ids = new ArrayList<>(predictions.size());
    for (Prediction prediction : predictions) {
      ids.add(prediction.getId());
      index(prediction);
    }
    context.stop();
    return ids;
  }

  private Document createDocument(Prediction p) {
    Document doc = new Document();
    doc.add(new StringField(PredictionDoc.ID.fieldName(), Integer.toString(p.getId()), Field.Store.YES));
    doc.add(new LongField(PredictionDoc.CREATED_ON.fieldName(), p.getCreateTimestamp(), Field.Store.YES));
    doc.add(new LongField(PredictionDoc.TIME.fieldName(), p.getTime(), Field.Store.YES));
    doc.add(new IntField(PredictionDoc.CREATED_BY.fieldName(), p.getCreatedByUserId(), Field.Store.YES));
    doc.add(new NumericDocValuesField(PredictionDoc.CREATED_ON.fieldName(), p.getCreateTimestamp()));
    doc.add(new NumericDocValuesField(PredictionDoc.CREATED_BY.fieldName(), p.getCreatedByUserId()));
    doc.add(new NumericDocValuesField(PredictionDoc.TIME.fieldName(), p.getTime()));
    doc.add(new TextField(PredictionDoc.TITLE.fieldName(), getIndexableText(p.getTitle()), Field.Store.NO));
    doc.add(new TextField(PredictionDoc.TEXT.fieldName(), getIndexableText(p.getText()), Field.Store.NO));
    doc.add(new StringField(PredictionDoc.LOCATION.fieldName(), orEmpty(p.getLocation()), Field.Store.NO));
    doc.add(new StringField(PredictionDoc.ACTOR.fieldName(), orEmpty(p.getAbout()), Field.Store.NO));
    doc.add(new TextField(PredictionDoc.REASON.fieldName(), orEmpty(p.getReason()), Field.Store.NO));
    doc.add(new TextField(PredictionDoc.SOURCE_AUTHOR.fieldName(), getIndexableText(orEmpty(p.getSourceAuthor())), Field.Store.NO));
    doc.add(new TextField(PredictionDoc.SOURCE_REF.fieldName(), getIndexableText(orEmpty(p.getSourceRef())), Field.Store.NO));
    return doc;
  }

  private static String getIndexableText(String text) {
    text = text.replaceAll("\\s+", " ");
    text = StringEscapeUtils.unescapeHtml(text);
    text = StringEscapeUtils.unescapeJava(text);
    text = Jsoup.parse(text).text();
    Matcher m = WEBURL.matcher(text);
    int i = 0;
    while (m.find()) {
      String group = m.group(i);
      if (group != null) {
        text = text.replace(group, "").trim();
        i++;
      }
    }
    return text;
  }

  private static String orEmpty(String t) {
    return t == null ? "" : t;
  }

  public List<WordsWithFreq> getTopTags(int top, Schema field) {
    PriorityQueue<WordsWithFreq> heap = new PriorityQueue<>(top);
    List<WordsWithFreq> topTags = new ArrayList<>(top);
    int max = Integer.MIN_VALUE;
    try {
      Terms terms = getTerms(field);
      if (terms != null) {
        TermsEnum termsEnum = terms.iterator(null);
        BytesRef text;
        while ((text = termsEnum.next()) != null) {
          String word = text.utf8ToString();

          int docFreq = termsEnum.docFreq();
          if (!field.isDynamic()) {
            if (docFreq < 10) continue;
            if (word.contains("\'") || word.length() < 3 || word.length() > 32 || smartStopWords.contains(word)) continue;
          }

          heap.offer(new WordsWithFreq(word, docFreq));

          if (heap.size() >= top) {
            heap.poll();
          }
          if (max < docFreq) {
            max = docFreq;
          }
        }
      }
    } catch (IOException e) {
      LOG.error("Error building word histogram.", e);
    }
    LOG.info("Max freq found {}->{} ", field, Math.max(0, max));
    for (WordsWithFreq wordsWithFreq : new TreeSet<>(heap).descendingSet()) {
      LOG.trace("{} => {}", wordsWithFreq.freq, wordsWithFreq.word);
      topTags.add(wordsWithFreq);
    }
    return topTags;
  }

  private void purgeIndex() {
    try {
      synchronized (LOCK) {
        indexWriter.deleteAll();
        entityIndexWriter.deleteAll();

        reopenIndex();
        reopenEntityIndex();

        lastIndexedId = -1;
        lastIndexedIdWithEntities = -1;

        checkpoint();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void reopenIndex() throws IOException {
    indexWriter.forceMerge(1);
    indexWriter.commit();
    indexWriter.close(true);
    synchronized (LOCK) {
      indexWriter = new IndexWriter(fsDir, indexWriterConfig);
    }
  }

  private DirectoryReader getDirectoryReader() throws IOException {
    synchronized (LOCK) {
      return DirectoryReader.open(indexWriter, true);
    }
  }

  private DirectoryReader getEntityDirectoryReader() throws IOException {
    synchronized (ENTITY_LOCK) {
      return DirectoryReader.open(entityIndexWriter, true);
    }
  }

  protected Terms getTerms(Schema field) throws IOException {
    if (field instanceof PredictionDoc) return MultiFields.getTerms(getDirectoryReader(), field.fieldName());
    return MultiFields.getTerms(getEntityDirectoryReader(), field.fieldName());
  }

  protected IndexSearcher getSearcher() throws IOException {
    return new IndexSearcher(getDirectoryReader());
  }

  protected List<Lookup.LookupResult> lookupSuggestion(String q, boolean b, int n) {
    return suggester.lookup(q, b, n);
  }

  public void stop() {
    keepRunning = false;
    entityExtractionThread.interrupt();
  }

  public void reindexEntities() {
    ENTITY_LOG.info("Reindexing with entities.");
    db.resetIndexStatus(Status.INDEXED_WITHOUT_ENTITIES);
    lastIndexedIdWithEntities = -1;
    checkpoint();
    ENTITY_LOG.info("Marked all documents as {}", Status.INDEXED_WITHOUT_ENTITIES);
  }
}
