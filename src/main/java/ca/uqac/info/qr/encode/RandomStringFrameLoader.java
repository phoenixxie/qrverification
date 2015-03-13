package ca.uqac.info.qr.encode;

import org.apache.commons.lang3.RandomStringUtils;

public class RandomStringFrameLoader implements FrameLoader {

  private boolean completed;
  private int frameSize;
  private int leftSize;

  public RandomStringFrameLoader(int frameSize, int totalSize) {
    this.completed = false;
    this.leftSize = totalSize;
    this.frameSize = frameSize;
  }

  @Override
  public String nextFrame() {
    if (leftSize == 0) {
      completed = true;
      return null;
    }

    int len = Math.min(frameSize, leftSize);
    String s = RandomStringUtils.randomAlphabetic(len);
    leftSize -= len;
    
    return s;
  }

  public boolean hasCompleted() {
    return completed;
  }
}
