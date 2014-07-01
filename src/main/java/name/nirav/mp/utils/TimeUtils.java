package name.nirav.mp.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtils {
  private static ThreadLocal<Calendar> TL = new ThreadLocal<Calendar>() {
                                            protected Calendar initialValue() {
                                              return reset(Calendar.getInstance());
                                            }
                                          };

  private static Calendar reset(Calendar instance) {
    instance.set(Calendar.DAY_OF_MONTH, 0);
    instance.set(Calendar.MILLISECOND, 0);
    instance.set(Calendar.MINUTE, 0);
    instance.set(Calendar.SECOND, 0);
    instance.set(Calendar.HOUR_OF_DAY, 0);
    instance.set(Calendar.YEAR, 0);
    return instance;
  }

  public static Calendar getCalendar() {
    return TL.get();
  }

  public static int getToday() {
    Calendar instance = Calendar.getInstance();
    return instance.get(Calendar.DAY_OF_YEAR);
  }

  public static long getDayFromToday(int day) {
    Calendar instance = Calendar.getInstance();
    instance.set(Calendar.MILLISECOND, 0);
    instance.set(Calendar.MINUTE, 0);
    instance.set(Calendar.SECOND, 0);
    instance.set(Calendar.HOUR_OF_DAY, 0);
    instance.add(Calendar.DAY_OF_YEAR, day);
    return instance.getTimeInMillis();
  }

  public static String readableDate(long ts) {
    return new SimpleDateFormat("dd-MMM").format(new Date(ts));
  }

  public static long getYear(Integer yr) {
    if (yr == null) return -1L;
    Calendar calendar = TL.get();
    calendar.set(Calendar.YEAR, yr);
    return calendar.getTimeInMillis();
  }
}
