package net.chrisdolan.pcgen.viewer.model;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import pcgen.core.facade.CharacterFacade;
import pcgen.core.facade.UIDelegate;

public class CharacterItem {
    private static final Logger logger = Logger.getLogger(CharacterContent.class.getName());
    private static final UIDelegate uiDelegate = new AndroidUIDelegate();

    public String id;
    public File file;
	private CharacterFacade character = null;

    public CharacterItem(String id, File file) {
        this.id = id;
        this.file = file;
		try {
            // ClassLoader classLoader = CharacterReader.class.getClassLoader();
            // ClassLoader classLoader = new Bundle().getClassLoader();
            // URL url = classLoader.getResource(content);
            // if (url == null) {
            // logger.warning(content + ": null URL");
            // } else {
				Startup.init();
				CharacterReader reader = new CharacterReader();
            character = reader.open(file, uiDelegate);
            // }
        } catch (Exception e) {
            logger.log(Level.WARNING, file + ": exception: " + e, e);
		}
    }

    @Override
    public String toString() {
		if (character == null) {
            return file.getName();
		} else {
            return file.getName() + ": " + character.getNameRef().getReference();
		}
    }
}