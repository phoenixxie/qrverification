package ca.uqac.info.qr.decode;

import java.awt.image.BufferedImage;

import org.opencv.core.Mat;

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.protocol.Receiver;
import ca.uqac.info.buffertannen.protocol.Sender.SendingMode;
import ca.uqac.lif.qr.FrameDecoder;
import ca.uqac.lif.qr.ZXingReader;

public class QRCollector implements QRCapturer.Handler {

  private ZXingReader reader;

  private Stat stat;
  private int lastSeq;
  private QRProcessor processor;

  public QRCollector() {
    reader = new ZXingReader();
    stat = new Stat();
    lastSeq = -1;
    processor = null;
  }

  public void setStatFrame(StatFrame frame) {
    frame.setStat(stat);
  }
  
  public void reset() {
    lastSeq = -1;
    resetStat();
  }
  
  public void resetStat() {
    stat.reset();
  }
  
  public String getStatCSV() {
    return stat.toCSV();
  }
  
  public void setQRProcessor(QRProcessor processor) {
    this.processor = processor;
  }

  @Override
  public void decoded(BufferedImage img) {
    String msg = reader.readCode(img);
    if (msg == null) {
      return;
    }

    // skip the random code
    int start = msg.indexOf(' ');
    if (start == -1) {
      return;
    }
    int end = msg.indexOf(' ', start + 1);
    if (end == -1) {
      return;
    }

    // skip the sequence code
    int seq = -1;
    try {
      seq = Integer.parseInt(msg.substring(start + 1, end));
    } catch (NumberFormatException e) {
      return;
    }
    stat.incDecoded(msg.length());

    boolean isMatched = false;
    synchronized (this) {
      if (lastSeq == -1) {
        lastSeq = seq;
        stat.incMatched(1);
        isMatched = true;

        if (seq == 232) {
          System.err.println("BBB " + seq);
        }
      } else if (seq == lastSeq) {
        stat.incDuplicated(1);
      } else if (seq > lastSeq) {
        stat.incMatched(1);
        isMatched = true;
        if (seq > lastSeq + 1) {
          stat.incMissed(seq - lastSeq - 1);
        }
        lastSeq = seq;
      } else {
        System.err.println("Weird... seq < lastSeq: " + seq + " < " + lastSeq);
      }
    }

    if (!isMatched) {
      return;
    }
    
    msg = msg.substring(end + 1);
    if (processor == null) {
      return;
    }
    processor.process(msg);
  }

  @Override
  public void captured(Mat frame) {
    stat.incCaptured();
  }
}
