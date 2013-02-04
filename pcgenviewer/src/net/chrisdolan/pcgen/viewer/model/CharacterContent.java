package net.chrisdolan.pcgen.viewer.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.chrisdolan.pcgen.viewer.model.Startup.LazyStreamOpener;
import pcgen.system.ConfigurationSettings;

import android.content.Context;
import android.os.Environment;

/**
 * Mostly a stub right now. This is the model for the list of characters.
 * I expect this will be completely replaced eventually.
 * @author chris
 */
public class CharacterContent {
    private static final Logger logger = Logger.getLogger(CharacterContent.class.getName());

    public static List<CharacterItem> ITEMS = new ArrayList<CharacterItem>();
    public static Map<String, CharacterItem> ITEM_MAP = new HashMap<String, CharacterItem>();
    private static CharacterLoadHelper characterLoadHelper = new CharacterLoadHelper();

    public static void initFromContext(final Context context) throws IOException {
    	Startup.initFromContext(context);

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
		AndroidPluginLoader contextLoader = new AndroidPluginLoader(new File(pluginsDir), opener);
    	characterLoadHelper.setPluginClassLoader(contextLoader);

    	if (ITEMS.isEmpty()) {

        	// HACK: just look for files in /sdcard/pcgen
            File[] files = new File(Environment.getExternalStorageDirectory(), "pcgen").listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().toLowerCase(Locale.US).endsWith(".pcg")) {
                        try {
                            addItem(new CharacterItem(f.getName(), f, characterLoadHelper));
                        } catch (Throwable t) {
                            logger.log(Level.WARNING, "threw while opening character: " + t, t);
                        }
                    }
                }
            }
            if (ITEMS.isEmpty()) {
            	// Fallback: just put some placeholder content in to play with the UI
                addItem(new CharacterItem("1", new File("Hrig-5.17.pcg"), characterLoadHelper));
                addItem(new CharacterItem("2", new File("Item 2"), characterLoadHelper));
                addItem(new CharacterItem("3", new File("Item 3"), characterLoadHelper));
            }
        }
    }

    private static void addItem(CharacterItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }
}
