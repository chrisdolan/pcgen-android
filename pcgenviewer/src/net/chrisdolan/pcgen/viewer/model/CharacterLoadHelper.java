package net.chrisdolan.pcgen.viewer.model;

import java.io.File;
import java.io.IOException;
import java.util.List;

import pcgen.core.PlayerCharacter;
import pcgen.core.facade.DataSetFacade;
import pcgen.core.facade.SourceSelectionFacade;
import pcgen.core.facade.UIDelegate;
import pcgen.core.facade.util.ListFacades;
import pcgen.core.prereq.PrerequisiteTestFactory;
import pcgen.io.ExportHandler;
import pcgen.io.PCGIOHandler;
import pcgen.persistence.CampaignFileLoader;
import pcgen.persistence.GameModeFileLoader;
import pcgen.persistence.PersistenceLayerException;
import pcgen.persistence.SourceFileLoader;
import pcgen.persistence.lst.TokenStore;
import pcgen.persistence.lst.output.prereq.PrerequisiteWriterFactory;
import pcgen.persistence.lst.prereq.PreParserFactory;
import pcgen.rules.persistence.TokenLibrary;
import pcgen.system.CharacterManager;
import pcgen.system.ConfigurationSettings;
import pcgen.system.LanguageBundle;
import pcgen.system.PCGenTask;
import pcgen.system.PCGenTaskExecutor;
import pcgen.system.PCGenTaskListener;
import pcgen.system.PluginClassLoader;
import pcgen.util.EventListenerList;
import pcgen.util.Logging;
import pcgen.util.PJEP;

public class CharacterLoadHelper {
	private PluginClassLoader pluginClassLoader = null;

	public interface Callback {
		void character(PlayerCharacter character, DataSetFacade dataset);
	}

	public void setPluginClassLoader(PluginClassLoader pluginClassLoader) {
		this.pluginClassLoader = pluginClassLoader;
		
	}
	public void load(final File pcgFile, final UIDelegate uiDelegate, PCGenTaskListener taskListener, final Callback callback) {
		PCGenTaskExecutor executor = new PCGenTaskExecutor();
		if (taskListener != null)
			executor.addPCGenTaskListener(taskListener);
		executor.addPCGenTask(createLoadPluginTask());
		final NotYetDefinedTask gameModeLoaderTask = new NotYetDefinedTask("Load game modes");
		final NotYetDefinedTask campaignLoaderTask = new NotYetDefinedTask("Load campaigns");
		executor.addPCGenTask(new PCGenTask() {
			{
				setMaximum(100);
			}
			public void execute() {
				setProgress("Create game mode filter", 0);
				PcgGameModeFilter modeFilter;
				try {
					modeFilter = new PcgGameModeFilter(pcgFile);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				setProgress("Create game mode loader", 95);
				gameModeLoaderTask.setTask(new GameModeFileLoader(modeFilter));
				setProgress("Create campaign loader", 95);
				campaignLoaderTask.setTask(new CampaignFileLoader(modeFilter));
				setProgress("Create filtered loaders", 100);
			}
		});
		executor.addPCGenTask(gameModeLoaderTask);
		executor.addPCGenTask(campaignLoaderTask);
		final NotYetDefinedTask sourceLoaderTask = new NotYetDefinedTask("Load sources");
		executor.addPCGenTask(new PCGenTask() {
			{
				setMaximum(100);
			}
			public void execute() {
				setProgress("Find sources", 0);
				SourceSelectionFacade sources = CharacterManager.getRequiredSourcesForCharacter(pcgFile, uiDelegate);
				if (sources == null)
					throw new NullPointerException("Could not find sources -- " + pcgFile);
				setProgress("Create source loader", 95);
				sourceLoaderTask.setTask(new SourceFileLoader(sources, uiDelegate));
				setProgress("Load sources", 100);
			}
		});
		executor.addPCGenTask(sourceLoaderTask);
		executor.addPCGenTask(new PCGenTask() {
			{
				setMaximum(100);
			}
			public void execute() {
				setProgress("Get data set", 0);
				DataSetFacade data = ((SourceFileLoader)sourceLoaderTask.getTask()).getDataSetFacade();
				if (data == null)
					throw new NullPointerException("data set facade is null for sources of " + pcgFile);
				setProgress("Open character", 10);
				PlayerCharacter pc = generatePC(pcgFile, data, uiDelegate);
				
				//CharacterFacade character = CharacterManager.openCharacter(pcgFile, uiDelegate, data);
				setProgress("Loaded " + pcgFile.getName(), 100);
				callback.character(pc, data);
			}
		});
		executor.execute();
	}
	private PlayerCharacter generatePC(File file, DataSetFacade dataset, UIDelegate delegate) {
		// This is cheating by breaking the facade
		List campaigns = ListFacades.wrap(dataset.getCampaigns());
		final PCGIOHandler ioHandler = new PCGIOHandler();
		final PlayerCharacter newPC;
		try
		{
			newPC = new PlayerCharacter(false, campaigns);
			newPC.setFileName(file.getAbsolutePath());
			ioHandler.read(newPC, file.getAbsolutePath());
			newPC.insertBonusLanguageAbility();
			// Ensure any custom equipment held by the character is added to the dataset's list
			dataset.refreshEquipment();


			for (String error : ioHandler.getErrors()) {
				delegate.showErrorMessage("generatePC", error);
			}
			for (String warning : ioHandler.getWarnings()) {
				delegate.showWarningMessage("generatePC", warning);
			}
			if (!ioHandler.getErrors().isEmpty()) {
				return null;
			}
			Logging.log(Logging.INFO, "Loaded character " + newPC.getName() //$NON-NLS-1$
				+ " - " + file.getAbsolutePath()); //$NON-NLS-1$
	
			// if it's not broken, then only warnings should have been generated, and we won't count those
			// Register the character so that future checks to see if file already loaded will work
//			Globals.getPCList().add(newPC);
//			GMBus.send(new PCLoadedMessage(null, newPC));
	
			return newPC;
		}
		catch (Exception e)
		{
			Logging.errorPrint("Unable to load character " + file, e); //$NON-NLS-1$
			delegate.showErrorMessage(
				LanguageBundle.getString("in_cmLoadErrorTitle"), //$NON-NLS-1$
				LanguageBundle.getFormattedString("in_cmLoadErrorMessage", //$NON-NLS-1$
					file, e.getMessage()));
			return null;
		}
	}

	private PluginClassLoader createLoadPluginTask()
	{
		String pluginsDir = ConfigurationSettings.getPluginsDir();
		PluginClassLoader loader = pluginClassLoader;
		if (loader == null)
			loader = new PluginClassLoader(new File(pluginsDir));
		loader.addPluginLoader(TokenLibrary.getInstance());
		loader.addPluginLoader(TokenStore.inst());
		try
		{
			loader.addPluginLoader(PreParserFactory.getInstance());
		}
		catch (PersistenceLayerException ex)
		{
			Logging.errorPrint("createLoadPluginTask failed", ex);
		}
		loader.addPluginLoader(PrerequisiteTestFactory.getInstance()); // not needed for render but it causes a ton of errors
		loader.addPluginLoader(PrerequisiteWriterFactory.getInstance()); // not needed for render
		loader.addPluginLoader(PJEP.getJepPluginLoader()); // not needed for render
		loader.addPluginLoader(ExportHandler.getPluginLoader()); // yes, needed
		//loader.addPluginLoader(TokenConverter.getPluginLoader()); // I think not needed
		//loader.addPluginLoader(PluginManager.getInstance()); // not needed
		return loader;
	}

	/**
	 * Holder for a PCGenTask that we are not ready to instantiate yet.
	 */
	private static final class NotYetDefinedTask extends PCGenTask {
		private EventListenerList listenerList = new EventListenerList();
		private PCGenTask task = null;
		
		public NotYetDefinedTask(String message) {
			setProgress(message, 0);
		}
		public void setTask(PCGenTask task) {
			synchronized (this) {
				this.task = task;
			}
			// move cached listeners to the task
			PCGenTaskListener[] listeners = listenerList.getListeners(PCGenTaskListener.class);
			for (PCGenTaskListener l : listeners) {
				listenerList.remove(PCGenTaskListener.class, l);
				task.addPCGenTaskListener(l);
			}
		}
		public PCGenTask getTask() {
			synchronized (this) {
				return task;
			}
		}

		public void addPCGenTaskListener(PCGenTaskListener listener)
		{
			PCGenTask t = getTask();
			if (t == null)
				listenerList.add(PCGenTaskListener.class, listener);
			else
				t.addPCGenTaskListener(listener);
		}

		public void removePCGenTaskListener(PCGenTaskListener listener)
		{
			PCGenTask t = getTask();
			if (t == null)
				listenerList.remove(PCGenTaskListener.class, listener);
			else
				t.removePCGenTaskListener(listener);
		}

		public void execute() {
			PCGenTask t = getTask();
			if (t == null)
				throw new IllegalStateException("Task is not set yet! (msg:"+getMessage()+")");
			t.execute();
		}

		public int getMaximum()
		{
			PCGenTask t = getTask();
			return t == null ? super.getMaximum() : t.getMaximum();
		}

		public int getProgress()
		{
			PCGenTask t = getTask();
			return t == null ? super.getProgress() : t.getProgress();
		}

		public String getMessage()
		{
			PCGenTask t = getTask();
			String message = super.getMessage();
			if (t != null) {
				String taskMsg = task.getMessage();
				if (taskMsg != null) {
					message = message == null ? taskMsg : message + " - " + taskMsg;
				}
			}
			return message;
		}
	}
}