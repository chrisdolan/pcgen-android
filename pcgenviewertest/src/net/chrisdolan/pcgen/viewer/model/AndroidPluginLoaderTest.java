package net.chrisdolan.pcgen.viewer.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import pcgen.base.util.MapToList;
import pcgen.persistence.lst.LstToken;
import pcgen.system.PluginLoader;

public class AndroidPluginLoaderTest {

	@Test
	public void test() throws IOException {
		AndroidPluginLoader loader = new AndroidPluginLoader(new File("."), new Startup.LazyStreamOpener() {
			
			@Override
			public InputStream open() throws IOException {
				return new FileInputStream(new File("../pcgenviewer/assets/pluginclasses.properties"));
			}
			
			@Override
			public void close() throws IOException {
			}
		});
		MockPluginLoader mockPluginLoader = new MockPluginLoader();
		loader.addPluginLoader(mockPluginLoader);
		MapToList<String, String> pluginClasses = loader.readPluginClasses();
		System.out.println(pluginClasses);
		loader.loadPluginClasses(pluginClasses);
		System.out.println(mockPluginLoader.loadedClasses);
		Assert.assertTrue(mockPluginLoader.loadedClasses.size() > 1);
	}

	private final class MockPluginLoader implements PluginLoader {
		public List<Class<?>> loadedClasses = new ArrayList<Class<?>>();
		@Override
		public void loadPlugin(Class<?> clazz) throws Exception {
			loadedClasses.add(clazz);
		}

		@Override
		public Class<?>[] getPluginClasses() {
			return new Class[] {LstToken.class};
		}
	}
}
