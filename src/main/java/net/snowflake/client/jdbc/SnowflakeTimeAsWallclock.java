package net.snowflake.client.jdbc;

import java.sql.Time;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class SnowflakeTimeAsWallclock extends Time {

  int nanos = 0;
  boolean useWallclockTime = false;
  ZoneOffset offset = ZoneOffset.UTC;

  public SnowflakeTimeAsWallclock(long time, int nanos, boolean useWallclockTime) {
    super(time);
    this.nanos = nanos;
    this.useWallclockTime = useWallclockTime;
  }

  public SnowflakeTimeAsWallclock(
      long time, int nanos, boolean useWallclockTime, ZoneOffset offset) {
    this(time, nanos, useWallclockTime);
    this.offset = offset;
  }

  /**
   * Returns a string representation in UTC so as to display "wallclock time"
   *
   * @return a string representation of the object
   */
  public synchronized String toString() {
    if (!useWallclockTime) {
      return super.toString();
    }
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    LocalDateTime ldt =
        LocalDateTime.ofEpochSecond(
            SnowflakeUtil.getSecondsFromMillis(this.getTime()), this.nanos, this.offset);
    return ldt.format(formatter);
  }
}
