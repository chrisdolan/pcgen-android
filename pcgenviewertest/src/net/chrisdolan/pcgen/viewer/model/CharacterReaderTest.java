package net.chrisdolan.pcgen.viewer.model;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.Test;

import pcgen.core.facade.CharacterFacade;
import pcgen.core.facade.UIDelegate;

public class CharacterReaderTest {
	static final Logger logger = Logger.getLogger(CharacterReaderTest.class.getPackage().getName());
	static {
		Object[] keys = System.getProperties().keySet().toArray();
		Arrays.sort(keys);
		for (Object key : keys)
			System.out.println("" + key + " = "
					+ System.getProperty(key.toString()));
	}

    @Test
    public void test() throws IOException {
		//Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).setLevel(Level.FINEST);
        UIDelegate delegate = new MockUIDelegate();
        URL url = CharacterReader.class.getClassLoader().getResource("Hrig-5.17.7.pcg");
        Assert.assertNotNull(url);

//		configFactory = new PropertyContextFactory(SystemUtils.USER_DIR);
//		configFactory.registerAndLoadPropertyContext(ConfigurationSettings.getInstance());
        //ConfigurationSettings settings = ConfigurationSettings.getInstance();
        //settings.se
		Startup.init();
        CharacterReader characterReader = new CharacterReader();
		CharacterFacade character = characterReader.open(url, delegate);
		Assert.assertNotNull(character);
		//character.
    }
}
