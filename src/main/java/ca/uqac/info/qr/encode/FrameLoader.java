package ca.uqac.info.qr.encode;

public interface FrameLoader {
  public String nextFrame();
  public boolean hasCompleted();
}
