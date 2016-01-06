package upsilon.node.configuration;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import upsilon.node.util.DirectoryWatcher;
import upsilon.node.util.GlobalConstants;
import upsilon.node.util.UPath;

public class FileChangeWatcher {
	public interface Listener {
		public void fileChanged(UPath url, boolean isAux);
	}

	private static final HashMap<UPath, FileChangeWatcher> fileChangeRegistry = new HashMap<UPath, FileChangeWatcher>();

	private static final transient Logger LOG = LoggerFactory.getLogger(FileChangeWatcher.class);

	private static final HashMap<UPath, Long> fileModificationTimes = new HashMap<>();

	public static long getMtime(final UPath path) {
		long mtime;

		if (FileChangeWatcher.fileModificationTimes.containsKey(path)) {
			mtime = FileChangeWatcher.fileModificationTimes.get(path);
		}

		mtime = path.getMtime();

		return mtime;
	}

	public static boolean isAlreadyMonitoring(final UPath path) {
		return FileChangeWatcher.fileChangeRegistry.containsKey(path);
	}

	private static boolean isChanged(final UPath url) throws IllegalStateException {
		final long mtime = FileChangeWatcher.getMtime(url);
		final long dbmtime = FileChangeWatcher.fileModificationTimes.get(url);

		FileChangeWatcher.LOG.trace("Checking file for modification: " + dbmtime + " vs " + mtime);

		return mtime > dbmtime;
	}

	public static void stopAll() {
		for (final FileChangeWatcher fcw : FileChangeWatcher.fileChangeRegistry.values()) {
			fcw.continueMonitoring = false;
		}
	}

	public static void updateMtime(final UPath url, final long newTime) {
		FileChangeWatcher.fileModificationTimes.put(url, newTime);
	}

	private final Listener l;

	private Thread monitoringThread;

	private boolean continueMonitoring = true;
	private UPath path;
	private final boolean isAux;

	public FileChangeWatcher(final UPath path, final Listener l, final boolean isAux) {
		this.path = path;
		this.l = l;
		this.isAux = isAux;

		FileChangeWatcher.LOG.info("Constructed file watcher for: " + path);

		FileChangeWatcher.updateMtime(path, path.lastModified());

		this.setupMonitoringThread();
	}

	public void checkForModification() {
		if (FileChangeWatcher.isChanged(this.path)) {
			FileChangeWatcher.updateMtime(this.path, FileChangeWatcher.getMtime(this.path));
			FileChangeWatcher.LOG.debug("Configuration file has changed, notifying listeners.");

			this.l.fileChanged(this.path, this.isAux);
		}
	}

	public void setupMonitoringThread() {
		FileChangeWatcher.fileChangeRegistry.put(this.path, this);

		this.monitoringThread = new Thread("File watcher for: " + this.path.getFilename()) {
			@Override
			public void run() {
				FileChangeWatcher.this.watchForChanges();
			}
		};
	}

	public void setWatchedFile(final UPath path) {
		FileChangeWatcher.fileModificationTimes.put(this.path, path.getMtime() - 1);
		FileChangeWatcher.fileModificationTimes.put(path, path.getMtime() - 1);

		FileChangeWatcher.LOG.debug("Watched file changed from " + this.path + " to " + path);

		this.path = path;
	}

	public void start() {
		this.monitoringThread.start();
	}

	public synchronized void stop() {
		this.continueMonitoring = false;
		this.notify();
	}

	private synchronized void watchForChanges() {
		while (FileChangeWatcher.this.continueMonitoring) {
			try {
				this.wait(GlobalConstants.CONFIG_WATCHER_DELAY.getMillis());

				this.checkForModification();
			} catch (final InterruptedException | IllegalStateException e) {
				this.continueMonitoring = false;
			}
		}

		FileChangeWatcher.LOG.info("No longer watching file for changes: " + this.path);
		FileChangeWatcher.fileChangeRegistry.remove(this.path);
		DirectoryWatcher.allowReloading(this.path);
	}
}
