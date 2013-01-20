package net.chrisdolan.pcgen.viewer.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import pcgen.core.facade.CharacterFacade;
import pcgen.core.facade.DataSetFacade;
import pcgen.core.facade.SourceSelectionFacade;
import pcgen.core.facade.UIDelegate;
import pcgen.io.PCGFile;
import pcgen.persistence.SourceFileLoader;
import pcgen.system.CharacterManager;

/**
 * Wraps a bunch of the .pcg reading details, like pre-req sources.
 * @author chris
 */
public class CharacterReader {
	public CharacterFacade open(URL url, UIDelegate delegate) throws IOException {
		// TODO: change PCGen API to accept an URL or cache the remote URL as a local file
    	if (!"file".equals(url.getProtocol()))
    		throw new IOException("URL is not a file URL -- " + url);
    	File file = new File(url.getFile());
        return open(file, delegate);
    }

    public CharacterFacade open(File file, UIDelegate delegate) throws IOException {
    	if (!PCGFile.isPCGenCharacterFile(file))
            throw new IOException("File is not a PCGen file -- " + file);
    	if (!file.exists())
            throw new FileNotFoundException("File does not exist -- " + file);
    	
		SourceSelectionFacade sources = CharacterManager.getRequiredSourcesForCharacter(file, delegate);
		if (sources == null)
			throw new IOException("Could not find sources -- " + file);
		SourceFileLoader sourceLoader = new SourceFileLoader(sources, delegate);
		sourceLoader.execute();
		DataSetFacade data = sourceLoader.getDataSetFacade();
		if (data == null)
			throw new NullPointerException("data set facade is null");
		return CharacterManager.openCharacter(file, delegate, data);
    }
}
