package ca.uqac.info.qr.encode;

import java.io.IOException;
import java.io.InputStream;

import ca.uqac.info.buffertannen.message.BitFormatException;
import ca.uqac.info.buffertannen.message.BitSequence;
import ca.uqac.info.buffertannen.protocol.BlobSegment;
import ca.uqac.info.buffertannen.protocol.Sender;
import ca.uqac.info.buffertannen.protocol.Sender.SendingMode;

public class BufferTannenFrameLoader implements FrameLoader {

  private Sender sender;
  private SendingMode sendingMode = SendingMode.LAKE;
  private boolean completed;

  public BufferTannenFrameLoader(InputStream inStream, int frameSize) {
    this.completed = false;
    this.sender = newSender(inStream, frameSize);
  }

  public void setSendingMode(SendingMode mode) {
    this.sendingMode = mode;
    if (sender != null) {
      sender.setSendingMode(mode);
    }
  }

  public void rewind() {
    sender.rewind();
    completed = false;
  }

  private Sender newSender(InputStream inStream, int frameSize) {
    Sender sender = new Sender();
    sender.setSendingMode(sendingMode);
    sender.setFrameMaxLength((frameSize + BlobSegment.getHeaderSize()) * 8);
    sender.setLakeLoop(false);

    byte[] frameBuffer = new byte[frameSize];

    while (true) {
      try {
        int len = inStream.read(frameBuffer, 0, frameSize);
        if (len == 0) {
          break;
        } else if (len < 0) {
          break;
        }
        sender.addBlob(new BitSequence(frameBuffer, len * 8));
      } catch (IOException | BitFormatException e) {
        e.printStackTrace();
        return null;
      }
    }
    return sender;
  }

  public int getUniqueFrameCount() {
    return sender.getNumberOfSegments();
  }

  @Override
  public String nextFrame() {
    BitSequence frame = sender.pollBitSequence();
    if (frame != null) {
      return frame.toBase64();
    } else {
      completed = true;
      return null;
    }
  }
  
  public boolean hasCompleted() {
    return completed;
  }
}
