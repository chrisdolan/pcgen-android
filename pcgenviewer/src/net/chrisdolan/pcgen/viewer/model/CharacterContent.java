package net.chrisdolan.pcgen.viewer.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import pcgen.system.ConfigurationSettings;

import android.content.Context;
import android.os.Environment;

public class CharacterContent {
    private static final Logger logger = Logger.getLogger(CharacterContent.class.getName());

    public static List<CharacterItem> ITEMS = new ArrayList<CharacterItem>();
    public static Map<String, CharacterItem> ITEM_MAP = new HashMap<String, CharacterItem>();

    public static void initFromContext(Context context) throws IOException {
    	Startup.initFromContext(context);
        if (ITEMS.isEmpty()) {

            File[] files = new File(Environment.getExternalStorageDirectory(), "pcgen").listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().toLowerCase().endsWith(".pcg")) {
                        try {
                            addItem(new CharacterItem(f.getName(), f));
                        } catch (Throwable t) {
                            logger.log(Level.WARNING, "threw while opening character: " + t, t);
                        }
                    }
                }
            }
            if (ITEMS.isEmpty()) {
                addItem(new CharacterItem("1", new File("Hrig-5.17.pcg")));
                addItem(new CharacterItem("2", new File("Item 2")));
                addItem(new CharacterItem("3", new File("Item 3")));
            }
        }
    }

    private static void addItem(CharacterItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
    }
}
