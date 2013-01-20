package net.chrisdolan.pcgen.viewer.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Properties;

import net.chrisdolan.pcgen.viewer.model.Startup.LazyStreamOpener;
import pcgen.base.util.HashMapToList;
import pcgen.base.util.MapToList;
import pcgen.system.PluginClassLoader;
import pcgen.system.PluginLoader;
import pcgen.util.Logging;

public class AndroidPluginLoader extends PluginClassLoader {
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
	public MapToList<String, String>readPluginClasses() throws IOException {
		MapToList<String, String> pluginClassNames = new HashMapToList<String, String>();
		InputStream is = opener.open();
		try {
			Properties p = new Properties();
			p.load(is);
//			for (String pluginName : p.stringPropertyNames()) {
//				String[] classNames = p.getProperty(pluginName).split("\\s*,\\s*");
//				for (String className : classNames) {
//					className = className.trim();
//					if (!className.isEmpty())
//						pluginClassNames.addToListFor(pluginName, className);
//				}
//			}
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
	public void loadPluginClasses(MapToList<String, String> pluginClassNames) {
		for (String pluginName : pluginClassNames.getKeySet()) {
			for (String className : pluginClassNames.getListFor(pluginName)) {
				try {
					Class<?> clazz = Class.forName(className);
					processClass(clazz);
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
	}

	private void processClass(Class<?> clazz) {
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
