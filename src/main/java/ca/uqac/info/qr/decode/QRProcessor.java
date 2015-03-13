package ca.uqac.info.qr.decode;

public interface QRProcessor {
  void process(String code);
  boolean hasCompleted();
}
