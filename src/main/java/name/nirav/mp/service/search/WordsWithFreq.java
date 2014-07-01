package name.nirav.mp.service.search;

public final class WordsWithFreq implements Comparable<WordsWithFreq> {
  public final CharSequence word;
  public final long         freq;

  WordsWithFreq(CharSequence word, long freq) {
    this.word = word;
    this.freq = freq;
  }

  public int compareTo(WordsWithFreq o) {
    return Long.compare(freq, o.freq);
  }
}