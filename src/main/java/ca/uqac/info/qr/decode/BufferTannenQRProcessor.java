package ca.uqac.info.qr.decode;

import ca.uqac.info.buffertannen.protocol.Receiver;
import ca.uqac.info.buffertannen.protocol.Sender.SendingMode;

public class BufferTannenQRProcessor implements QRProcessor {
  private Receiver receiver;
  private boolean completed = false;

  public BufferTannenQRProcessor() {
    receiver = new Receiver();
  }

  @Override
  public void process(String code) {
    receiver.putBitSequence(code);

    if (receiver.getSendingMode() == SendingMode.LAKE) {
      float progress = getProgress();
      if (Math.abs(progress - 1.0f) < 0.00001) {
        completed = true;
      }
    }
  }

  @Override
  public boolean hasCompleted() {
    return completed;
  }

  public float getProgress() {
    boolean[] status = receiver.getBufferStatus();
    int cnt = 0;
    for (int i = 0; i < status.length; ++i) {
      if (status[i]) {
        ++cnt;
      }
    }
    return (float) cnt / (float) status.length;
  }

  public int getReceived() {
    boolean[] status = receiver.getBufferStatus();
    if (status == null) {
      return 0;
    }
    int cnt = 0;
    for (int i = 0; i < status.length; ++i) {
      if (status[i]) {
        ++cnt;
      }
    }
    return cnt;
  }

}
