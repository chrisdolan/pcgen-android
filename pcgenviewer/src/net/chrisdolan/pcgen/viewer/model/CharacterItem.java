package net.chrisdolan.pcgen.viewer.model;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.chrisdolan.pcgen.viewer.model.CharacterLoadHelper.Callback;
import pcgen.core.PlayerCharacter;
import pcgen.core.facade.DataSetFacade;
import pcgen.core.facade.UIDelegate;

/**
 * Model for a single character. This holds the loaded .pcg file.
 * @author chris
 */
public class CharacterItem {
    private static final Logger logger = Logger.getLogger(CharacterContent.class.getName());
    private static final UIDelegate uiDelegate = new AndroidUIDelegate();
    private static final Executor executor = Executors.newSingleThreadExecutor();

    public String id;
    public File file;
	private PlayerCharacter character = null;
	private final CharacterLoadHelper loader;

    public CharacterItem(String id, final File file, CharacterLoadHelper characterLoadHelper) {
        this.id = id;
        this.file = file;
		this.loader = characterLoadHelper;
		executor.execute(new Runnable() {
			public void run() {
				try {
					loader.load(file, uiDelegate, null, new Callback() {
						public void character(PlayerCharacter character, DataSetFacade dataset) {
							setCharacter(character);
						}
					});
//					Startup.init();
//					CharacterReader reader = new CharacterReader();
//		            character = reader.open(file, uiDelegate);
		        } catch (Exception e) {
		            logger.log(Level.WARNING, file + ": exception: " + e, e);
				}
			}
		});
    }


	private void setCharacter(PlayerCharacter character) {
		// Todo update on UI thread and update UI.
		CharacterItem.this.character = character;
	}

	@Override
    public String toString() {
		if (character == null) {
            return file.getName();
		} else {
            return file.getName() + ": " + character.getName();
		}
    }
}