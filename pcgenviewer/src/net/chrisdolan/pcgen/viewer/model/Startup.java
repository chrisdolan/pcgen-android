package net.chrisdolan.pcgen.viewer.model;

import gmgen.pluginmgr.PluginManager;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

import pcgen.core.prereq.PrerequisiteTestFactory;
import pcgen.gui2.converter.TokenConverter;
import pcgen.io.ExportHandler;
import pcgen.persistence.CampaignFileLoader;
import pcgen.persistence.GameModeFileLoader;
import pcgen.persistence.PersistenceLayerException;
import pcgen.persistence.lst.TokenStore;
import pcgen.persistence.lst.output.prereq.PrerequisiteWriterFactory;
import pcgen.persistence.lst.prereq.PreParserFactory;
import pcgen.rules.persistence.TokenLibrary;
import pcgen.system.ConfigurationSettings;
import pcgen.system.PluginClassLoader;
import pcgen.util.Logging;
import pcgen.util.PJEP;
import android.content.Context;
import android.os.Environment;

/**
 * Lots of bootstrap code, like exploding the flat files into the cache dir
 * and tweaking the paths in the core config.
 * 
 * @author chris
 */
public class Startup {
    private static final Logger logger = Logger.getLogger(Startup.class.getName());
	private static final AtomicBoolean initedContext = new AtomicBoolean(false);
	private static final AtomicBoolean inited = new AtomicBoolean(false);
	private static PluginClassLoader contextLoader = null; 

	public static void initFromContext(final Context context) throws IOException {
		if (initedContext.compareAndSet(false, true)) {
            splat(context);
			explodeFiles(context);
			String pluginsDir = ConfigurationSettings.getPluginsDir();
			LazyStreamOpener opener = new LazyStreamOpener() {
				@Override
				public InputStream open() throws IOException {
					return context.getAssets().open("pluginclasses.properties");
				}
				@Override
				public void close() throws IOException {
				}
			};
			contextLoader = new AndroidPluginLoader(new File(pluginsDir), opener);
		}
	}
	public static void init() {
		if (inited.compareAndSet(false, true)) {
			createLoadPluginTask().execute();
			new GameModeFileLoader().execute();
			new CampaignFileLoader().execute();
		}
	}

	private static PluginClassLoader createLoadPluginTask()
	{
		PluginClassLoader loader = contextLoader;
		contextLoader = null;
		if (loader == null) {
			String pluginsDir = ConfigurationSettings.getPluginsDir();
			loader = new PluginClassLoader(new File(pluginsDir));
		}
		loader.addPluginLoader(TokenLibrary.getInstance());
		loader.addPluginLoader(TokenStore.inst());
		try
		{
			loader.addPluginLoader(PreParserFactory.getInstance());
		}
		catch (PersistenceLayerException ex)
		{
			Logging.errorPrint("createLoadPluginTask failed", ex);
		}
		loader.addPluginLoader(PrerequisiteTestFactory.getInstance());
		loader.addPluginLoader(PrerequisiteWriterFactory.getInstance());
		loader.addPluginLoader(PJEP.getJepPluginLoader());
		loader.addPluginLoader(ExportHandler.getPluginLoader());
		loader.addPluginLoader(TokenConverter.getPluginLoader());
		loader.addPluginLoader(PluginManager.getInstance());
		return loader;
	}
	static String toHexString(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length*2);
		for (byte b : bytes) {
			sb.append(Integer.toString((b >> 4) & 0xf, 16));
			sb.append(Integer.toString(b & 0xf, 16));
		}
		return sb.toString();
	}

	interface LazyStreamOpener {
		InputStream open() throws IOException;
		void close() throws IOException;
	}
	static void explodeFiles(final Context context) throws IOException {
		LazyStreamOpener opener = new LazyStreamOpener() {
			@Override
			public InputStream open() throws IOException {
				return context.getAssets().open("datafiles.zip");
			}
			@Override
			public void close() throws IOException {
			}
		};
		File apkFile = new File(context.getPackageCodePath());
		File thisCacheDir = Exploder.explodeFiles(apkFile, context.getCacheDir(), opener);
        ConfigurationSettings settings = ConfigurationSettings.getInstance();
        settings.setProperty(ConfigurationSettings.THEME_PACK_DIR,    new File(thisCacheDir, "lib/lnf/themes").getAbsolutePath());
        settings.setProperty(ConfigurationSettings.SYSTEMS_DIR,       new File(thisCacheDir, "system").getAbsolutePath());
        settings.setProperty(ConfigurationSettings.OUTPUT_SHEETS_DIR, new File(thisCacheDir, "outputsheets").getAbsolutePath());
        settings.setProperty(ConfigurationSettings.PLUGINS_DIR,       new File(thisCacheDir, "plugins").getAbsolutePath());
        settings.setProperty(ConfigurationSettings.PREVIEW_DIR,       new File(thisCacheDir, "preview").getAbsolutePath());
        settings.setProperty(ConfigurationSettings.DOCS_DIR,          new File(thisCacheDir, "docs").getAbsolutePath());
        settings.setProperty(ConfigurationSettings.VENDOR_DATA_DIR,   new File(thisCacheDir, "vendordata").getAbsolutePath());
        settings.setProperty(ConfigurationSettings.PCC_FILES_DIR,     new File(thisCacheDir, "data").getAbsolutePath());
        settings.setProperty(ConfigurationSettings.CUSTOM_DATA_DIR,   new File(thisCacheDir, "data/customsources").getAbsolutePath());
	}

	public static class Exploder {
		public static File explodeFiles(File apkFile, File appCacheDir, LazyStreamOpener opener) throws IOException {
			if (!apkFile.exists())
				throw new IllegalStateException("missing APK! expected at: " + apkFile.getAbsolutePath());
			String digest = makeFileDigest(apkFile);
			File cacheDir = new File(appCacheDir, "expand");
			File thisCacheDir = new File(cacheDir, digest);
			if (!thisCacheDir.exists() || !thisCacheDir.isDirectory()) {
				if (!cacheDir.exists()) {
					if (!cacheDir.mkdir())
						throw new IOException("Failed to make a cache dir");
				} else {
					// clear stale cache
					for (File f : cacheDir.listFiles()) {
						FileUtil.deleteTree(f); // ignore failures
					}
				}
				if (! thisCacheDir.mkdir())
					throw new IOException("Failed to make a cache dir");
				boolean success = false;
				try {
					InputStream is = opener.open();
					try {
						ZipInputStream zis = new ZipInputStream(is);
						try {
							int numFiles = FileUtil.unzip(zis, thisCacheDir);
							success = true;
							logger.info("Exploded " + numFiles + " files from " + apkFile + " to " + appCacheDir);
						} finally {        
							zis.close();
						}
					} finally {
						is.close();
						opener.close();
					}
				} finally {
					if (!success)
						FileUtil.deleteTree(thisCacheDir); // ignore failures
				}
			}
			return thisCacheDir;
		}

		private static String makeFileDigest(File apkFile) throws IOException {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
				DataOutputStream dos = new DataOutputStream(baos);

				dos.writeLong(apkFile.lastModified());
				dos.writeChar(';');
				dos.writeLong(apkFile.length());
				dos.writeChar(';');
				dos.writeUTF(apkFile.getAbsolutePath());
				dos.close();

				MessageDigest md = MessageDigest.getInstance("MD5");
				return toHexString(md.digest(baos.toByteArray()));
			} catch (NoSuchAlgorithmException e) {
				throw new IOException("Failed to make a digester", e);
			} catch (UnsupportedEncodingException e) {
				throw new IOException("No UTF??", e);
			}
		}
	}
	private static void splat(Context context) {
        logger.warning("Context code path " + context.getPackageCodePath());
        logger.warning("Context code path has = " + Arrays.toString(new File(context.getPackageCodePath()).list()));
        logger.warning("Context resource path " + context.getPackageResourcePath());
        logger.warning("Context app info " + context.getApplicationInfo());
        logger.warning("Context cache dir " + context.getCacheDir());
        logger.warning("Context cache dir has = " + Arrays.toString(context.getCacheDir().list()));
        logger.warning("Context external cache dir " + context.getExternalCacheDir());
        logger.warning("Context files dir " + context.getFilesDir());
        logger.warning("Context files dir has = " + Arrays.toString(context.getFilesDir().list()));
        logger.warning("Context app dir " + context.getFilesDir().getParentFile());
        logger.warning("Context app dir has = " + Arrays.toString(context.getFilesDir().getParentFile().list()));
        logger.warning("Context external files dir " + context.getExternalFilesDir(null));
        logger.warning("Context obb dir " + context.getObbDir());
        logger.warning("Context obb dir has = " + Arrays.toString(context.getObbDir().list()));
        try {
            logger.warning("Context assets in . = " + Arrays.toString(context.getAssets().list(".")));
        } catch (IOException e) {
        }
        logger.warning("Context resources " + context.getResources());
        logger.warning("File list! " + Arrays.toString(context.fileList()));
        logger.warning("env ext store -- " + Environment.getExternalStorageDirectory().getPath());
        logger.warning("env Files on ext store -- "
                + Arrays.toString(new File(Environment.getExternalStorageDirectory().getPath()).list()));
        logger.warning("env getDataDirectory -- " + Environment.getDataDirectory().getPath());
        logger.warning("env getDownloadCacheDirectory -- " + Environment.getDownloadCacheDirectory().getPath());
        logger.warning("env getRootDirectory -- " + Environment.getRootDirectory().getPath());
        for (File f : new File[] { new File("."), new File("/") }) {
            logger.warning("test file " + f.getPath() + " = abs " + f.getAbsolutePath() + ", list = " + Arrays.toString(f.list()));
        }
//        recursiveShowDir("Environment.getDataDirectory()", Environment.getDataDirectory());
//        recursiveShowDir("new File(context.getPackageCodePath()).getParentFile()",
//                new File(context.getPackageCodePath()).getParentFile());
//        recursiveShowDir("context.getFilesDir().getParentFile()", context.getFilesDir().getParentFile());
    }

    static void recursiveShowDir(String name, File dir) {
    	recursiveShowDir(name, dir, null, null);
    }
    static void recursiveShowDir(String name, File dir, File relativeTo, PrintStream out) {
    	String relPath = relativeTo == null ? null : relativeTo.getAbsolutePath();
    	if (relPath != null) {
    		if (!dir.getAbsolutePath().startsWith(relPath))
    			throw new IllegalArgumentException("dir does not start with relpath");
    	}
    	if (out == null)
    		logger.warning("recursive show: " + name);
    	else
    		out.println("recursive show: " + name);
        showDir(dir, "  ", relPath, out);
    }

    static void showDir(File dir, String prefix, String relPath, PrintStream out) {
    	String path = relPath == null ? dir.getPath() : dir.getAbsolutePath().substring(relPath.length());
    	if (out == null)
    		logger.warning(prefix + path);
    	else
    		out.println(prefix + path);
        if (dir.isDirectory()) {
            File[] list = dir.listFiles();
            if (list != null)
                for (File f : list)
                    showDir(f, prefix + "  ", relPath, out);
        }
    }
}
