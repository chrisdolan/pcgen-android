package net.chrisdolan.pcgen.viewer.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import pcgen.core.facade.CharacterFacade;
import pcgen.core.facade.ChooserFacade;
import pcgen.core.facade.UIDelegate;
import pcgen.system.PropertyContext;

/**
 * Callback functions from the engine to the UI. This needs a lot of work to fill in the "TODO" methods.
 * But this initial implementation suffices to get the warning/error messages out.
 * 
 * @author chris
 */
public final class AndroidUIDelegate implements UIDelegate {
	Logger logger = Logger.getLogger(AndroidUIDelegate.class.getName());
	@Override
	public boolean showWarningPrompt(String title, String message) {
		// TODO
		return false;
	}
	@Override
	public void showWarningMessage(String title, String message) {
		logger.log(Level.WARNING, title + ": " + message, new Exception("stack trace..."));
	}
	@Override
	public boolean showWarningConfirm(String title, String message) {
		// TODO
		return false;
	}
	@Override
	public void showLevelUpInfo(CharacterFacade character, int oldLevel) {
		// TODO
	}
	@Override
	public String showInputDialog(String title, String message, String initialValue) {
		// TODO
		return null;
	}
	@Override
	public void showInfoMessage(String title, String message) {
		logger.info(title + ": " + message);
	}
	@Override
	public boolean showGeneralChooser(ChooserFacade chooserFacade) {
		// TODO
		return false;
	}
	@Override
	public void showErrorMessage(String title, String message) {
		logger.log(Level.SEVERE, title + ": " + message, new Exception("stack trace..."));
	}
	@Override
	public Boolean maybeShowWarningConfirm(String title, String message,
			String checkBoxText, PropertyContext context, String contextProp) {
		// TODO
		return false;
	}
}