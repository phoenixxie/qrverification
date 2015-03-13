package ca.uqac.info.qr.decode;

import java.util.ArrayList;
import java.util.List;

import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

public class CameraManager {

	public static class Config {
		private int index;
		private int width;
		private int height;

		private Config(int index, int width, int height) {
			this.index = index;
			this.width = width;
			this.height = height;
		}

		@Override
		public String toString() {
			return "Camera_" + index + "(" + width + "x" + height + ")";
		}

		public int index() {
			return index;
		}

		public int width() {
			return width;
		}

		public int height() {
			return height;
		}
	}

	private List<Config> cameraConfigs;
	private int maxIndex;

	private static CameraManager instance = null;

	public static CameraManager instance() {
		if (instance == null) {
			instance = new CameraManager();
		}

		return instance;
	}

	private CameraManager() {
		cameraConfigs = new ArrayList<Config>();
		maxIndex = 0;
		update();
	}

	private void testResolution(VideoCapture camera, int index, int newWidth,
			int newHeight) {

		camera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, newWidth);
		camera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, newHeight);

		double width = camera.get(Highgui.CV_CAP_PROP_FRAME_WIDTH);
		double height = camera.get(Highgui.CV_CAP_PROP_FRAME_HEIGHT);

		if (Math.abs(width - newWidth) < 0.0001
				&& Math.abs(height - newHeight) < 0.0001) {
			Config config = new Config(index, newWidth, newHeight);
			cameraConfigs.add(config);
		}
	}

	public void update() {
		VideoCapture camera = new VideoCapture();
		cameraConfigs.clear();

		maxIndex = 0;
		while (true) {
			boolean exists = camera.open(maxIndex);
			if (!exists) {
				break;
			}

			testResolution(camera, maxIndex, 1920, 1080);
			testResolution(camera, maxIndex, 1280, 1024);
			testResolution(camera, maxIndex, 1280, 800);
			testResolution(camera, maxIndex, 1280, 720);
			testResolution(camera, maxIndex, 1024, 768);
			testResolution(camera, maxIndex, 800, 600);
			testResolution(camera, maxIndex, 640, 480);

			camera.release();
			++maxIndex;
		}

	}

	public List<Config> getConfigs() {
		return cameraConfigs;
	}

	public int findConfig(int index, int width, int height) {
		int n = 0;
		for (Config c : cameraConfigs) {
			if (c.index == index && c.width == width && c.height == height) {
				return n;
			}
			++n;
		}
		return -1;
	}

	public Config getConfig(int n) {
		if (n >= cameraConfigs.size()) {
			return null;
		}
		return cameraConfigs.get(n);
	}
}
