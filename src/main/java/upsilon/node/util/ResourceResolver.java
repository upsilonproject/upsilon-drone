package upsilon.node.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import upsilon.node.Main;
import upsilon.node.util.UPath;

public abstract class ResourceResolver {
	public static class ResourceResolverJar extends ResourceResolver {
		@Override
		public InputStream getInternalFromFilename(final String filename) throws FileNotFoundException {
			final InputStream is = this.getClass().getResourceAsStream("/" + filename);

			return is;
		}
	}

	private transient static final Logger LOG = LoggerFactory.getLogger(ResourceResolver.class);

	public static ResourceResolver getInstance() {
		return new ResourceResolverJar();
	}

	public UPath getConfigDirAsUPath() {
		try {
			if (Main.getConfigurationOverridePath() != null) {
				return new UPath(Main.getConfigurationOverridePath());
			} else {
				return new UPath(this.getOsConfigDir());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public File getConfigDir() {
		return this.getConfigDirAsUPath().toFile();
	}

	public abstract InputStream getInternalFromFilename(String filename) throws Exception;

	public File getOsConfigDir() {
		final String os = System.getProperty("os.name");
		File f;

		if (os.contains("Windows")) {
			f = new File(System.getenv("PROGRAMDATA"), "/upsilon-node/");
		} else if (os.contains("Linux")) {
			f = new File("/etc/upsilon-drone/");

			if (!f.exists()) {
				if (!f.mkdirs()) {
					f = new File(System.getenv("HOME") + "/.upsilon-node/");
				}
			}
		} else {
			ResourceResolver.LOG.warn("Could not find OS specific directory.");
			f = new File("./");
		}

		if (!f.exists()) {
			if (!f.mkdir()) {
				ResourceResolver.LOG.warn("Could not create configuration directory on this platform, which should be: " + f.getAbsolutePath());
			}
		}

		return f;
	}

}
