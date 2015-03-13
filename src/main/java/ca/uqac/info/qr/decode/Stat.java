package ca.uqac.info.qr.decode;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang.StringUtils;

import ca.uqac.info.qr.utils.SpeedTester;

public class Stat {

  private int captured = 0;
  private int decoded = 0;
  private long decodedBytes = 0;

  private long startTime = System.currentTimeMillis();
  private SpeedTester capturedPerSec;
  private SpeedTester decodedBytesPerSec;

  private int matched = 0;
  private int missed = 0;
  private int duplicated = 0;

  boolean running;

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  @Override
  public String toString() {
    StringBuilder build = new StringBuilder();
    build.append(", Captured: ").append(captured).append(", Decoded: ")
        .append(decoded).append(", Matched: ").append(matched)
        .append(", Missed: ").append(missed).append(", Duplicated: ")
        .append(duplicated);

    return build.toString();
  }

  public String toCSV() {
    float diff = (float)(System.currentTimeMillis() - startTime) / 1000.0f;
    
    return StringUtils.join(
        new String[] {
            "" + diff,
            "" + captured,
            "" + decoded,
            "" + matched,
            "" + missed,
            "" + duplicated,
        }, ",");
  }

  public Stat() {
    running = false;

    reset();
  }

  public synchronized void reset() {
    captured = 0;
    decoded = 0;
    decodedBytes = 0;

    matched = 0;
    missed = 0;
    duplicated = 0;

    capturedPerSec = new SpeedTester();
    decodedBytesPerSec = new SpeedTester();
    startTime = System.currentTimeMillis();
  }

  // public synchronized void setFileNamePrefix(String prefix) {
  // if (csvWriter != null) {
  // try {
  // csvWriter.close();
  // } catch (IOException e) {
  // e.printStackTrace();
  // }
  // csvWriter = null;
  // }
  // fileNamePrefix = prefix;
  //
  // if (prefix.isEmpty()) {
  // return;
  // }
  // try {
  // csvWriter = new FileWriter(prefix + generateFileName());
  // } catch (IOException e) {
  // e.printStackTrace();
  // csvWriter = null;
  // }
  // }
  //
  // private String generateFileName() {
  // return new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar
  // .getInstance().getTime()) + ".csv";
  // }
  //
  // public synchronized void reset() {
  // lastSeq = -1;
  //
  // setFileNamePrefix(fileNamePrefix);
  // }

  // public synchronized void start() {
  // running = true;
  // new Thread(new RecordThread()).start();
  // }
  //
  // public synchronized void stop() {
  // if (!running) {
  // return;
  // }
  // running = false;
  //
  // if (csvWriter != null) {
  // try {
  // csvWriter.close();
  // } catch (IOException e) {
  // }
  // csvWriter = null;
  // }
  // }

  public synchronized int getCaptured() {
    return captured;
  }

  public synchronized int getDecoded() {
    return decoded;
  }

  public synchronized long getDecodedBytes() {
    return decodedBytes;
  }

  public synchronized int getMatched() {
    return matched;
  }

  public synchronized int getMissed() {
    return missed;
  }

  public synchronized int getDuplicated() {
    return duplicated;
  }

  public synchronized float getCapturedPerSec() {
    return capturedPerSec.speed();
  }

  public synchronized float getDecodedBytesPerSec() {
    return decodedBytesPerSec.speed() * 8.0f;
  }

  public synchronized long getRunningTime() {
    return System.currentTimeMillis() - startTime;
  }

  public synchronized void incCaptured() {
    ++captured;
    capturedPerSec.add(1);
  }

  public synchronized void incMatched(int n) {
    matched += n;
  }

  public synchronized void incDuplicated(int n) {
    duplicated += n;
  }

  public synchronized void incMissed(int n) {
    missed += n;
  }

  public synchronized void incDecoded(int length) {
    ++decoded;
    decodedBytes += length;
    decodedBytesPerSec.add(length);
  }

  // class RecordThread implements Runnable {
  //
  // @Override
  // public void run() {
  // while (running) {
  // synchronized (this) {
  // if (csvWriter != null) {
  // try {
  // csvWriter.write(info.toCSV() + "\n");
  // } catch (IOException e) {
  // e.printStackTrace();
  // }
  // }
  // }
  //
  // try {
  // Thread.sleep(1000);
  // } catch (InterruptedException e) {
  // }
  // }
  //
  // }
  //
  // }
}
