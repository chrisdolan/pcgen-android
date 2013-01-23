package net.chrisdolan.pcgen.viewer.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Properties;
import java.util.logging.Logger;

import net.chrisdolan.pcgen.viewer.model.Startup.LazyStreamOpener;
import pcgen.base.util.HashMapToList;
import pcgen.base.util.MapToList;
import pcgen.system.PluginClassLoader;
import pcgen.system.PluginLoader;
import pcgen.util.Logging;

/**
 * This is a heavy workaround for the PluginClassLoader. That class doesn't work at
 * all in Android because it wants to search through .jar files to find .class files.
 * Instead, I load a pre-generated list of plugin classes from a flat file (see
 * ant-preparefiles.xml) and then send those classes to the plugin loaders.
 * 
 * @author chris
 */
public class AndroidPluginLoader extends PluginClassLoader {
    private static final Logger logger = Logger.getLogger(AndroidPluginLoader.class.getName());
	private final MapToList<Class<?>, PluginLoader> loaderMap = new HashMapToList<Class<?>, PluginLoader>();
	private final LazyStreamOpener opener;

	public AndroidPluginLoader(File pluginDir, LazyStreamOpener opener) {
		super(pluginDir);
		this.opener = opener;
	}

	@Override
	public void addPluginLoader(PluginLoader loader) {
		for (Class<?> clazz : loader.getPluginClasses()) {
			loaderMap.addToListFor(clazz, loader);
		}
	}

	@Override
	public void loadPlugins() {
		MapToList<String, String> pluginClassNames;
		try {
			pluginClassNames = readPluginClasses();
		} catch (IOException e) {
			Logging.errorPrint("Error occurred while loading plugin classes file", e);
			return;
		}
		loadPluginClasses(pluginClassNames);
	}

	/**
	 * Reads a flat file of classes, which were early collected from plugin jar files at compile time.
	 * 
	 * @return a list of class names from each plugin. Right now, the plugin name is always empty
	 *    (just an implementation limitation).
	 * @throws IOException
	 */
	MapToList<String, String>readPluginClasses() throws IOException {
		MapToList<String, String> pluginClassNames = new HashMapToList<String, String>();
		InputStream is = opener.open();
		try {
			Properties p = new Properties();
			p.load(is);
			for (String className : p.stringPropertyNames()) {
				String pluginName = p.getProperty(className);
				className = className.trim();
				pluginName = pluginName.trim();
				if (!className.isEmpty())
					pluginClassNames.addToListFor(pluginName, className);
			}
		} finally {
			is.close();
			opener.close();
		}
		return pluginClassNames;
	}

	void loadPluginClasses(MapToList<String, String> pluginClassNames) {
		int numLoadedClasses = 0;
		int numSkippedGuiClasses = 0;

		for (String pluginName : pluginClassNames.getKeySet()) {
			for (String className : pluginClassNames.getListFor(pluginName)) {
				if (className.contains(".gui.")) {
					numSkippedGuiClasses++;
					continue; // GUI plugin classes are guaranteed to fail on Android...
				}

				try {
					Class<?> clazz = Class.forName(className);
					processClass(clazz);
					numLoadedClasses++;
				}
				catch (ClassNotFoundException ex)
				{
					Logging.errorPrint("Error occurred while loading file: " +
							className, ex);
				}
				catch (NoClassDefFoundError e)
				{
					Logging.errorPrint("Error occurred while loading file: " +
							className, e);
				}
				catch (ExceptionInInitializerError e)
				{
					Logging.errorPrint("Error occurred while loading file: " +
							className, e);
				}
			}
		}
		logger.warning("Loaded some plugin classes: " + numLoadedClasses);
		if (numSkippedGuiClasses > 0) {
			logger.warning("Skipped some GUI plugin classes: " + numSkippedGuiClasses);
		}
	}

	private void processClass(Class<?> clazz) {
		// HACK: this is a copy of the private method of the same name in the superclass... 

		int modifiers = clazz.getModifiers();
		if (Modifier.isInterface(modifiers) || Modifier.isAbstract(modifiers))
		{
			return;
		}
		for (Class<?> key : loaderMap.getKeySet())
		{
			if (key != null && !key.isAssignableFrom(clazz))
			{
				continue;
			}
			for (PluginLoader loader : loaderMap.getListFor(key))
			{
				try
				{
					loader.loadPlugin(clazz);
				} catch (Exception ex)
				{
					Logging.errorPrint("Error occurred while loading plugin class: " +
							clazz.getName(), ex);
				}
			}
		}
	}
}
