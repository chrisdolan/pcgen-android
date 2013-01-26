package net.chrisdolan.pcgen.viewer.uitest;

import java.io.File;

import pcgen.core.facade.CharacterFacade;
import pcgen.core.facade.DataSetFacade;
import pcgen.core.facade.SourceSelectionFacade;
import pcgen.core.facade.UIDelegate;
import pcgen.core.prereq.PrerequisiteTestFactory;
import pcgen.gui2.converter.TokenConverter;
import pcgen.io.ExportHandler;
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
import pcgen.system.PCGenTask;
import pcgen.system.PCGenTaskExecutor;
import pcgen.system.PCGenTaskListener;
import pcgen.system.PluginClassLoader;
import pcgen.util.EventListenerList;
import pcgen.util.Logging;
import pcgen.util.PJEP;

public class CharacterLoadHelper {
	public interface Callback {
		void character(CharacterFacade character);
	}
	public void load(final File pcgFile, final UIDelegate uiDelegate, PCGenTaskListener taskListener, final Callback callback) {
		PCGenTaskExecutor executor = new PCGenTaskExecutor();
		if (taskListener != null)
			executor.addPCGenTaskListener(taskListener);
		executor.addPCGenTask(createLoadPluginTask());
		executor.addPCGenTask(new GameModeFileLoader());
		executor.addPCGenTask(new CampaignFileLoader());
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
				CharacterFacade character = CharacterManager.openCharacter(pcgFile, uiDelegate, data);
				setProgress("Loaded " + pcgFile.getName(), 100);
				callback.character(character);
			}
		});
		executor.execute();
	}

	private static PluginClassLoader createLoadPluginTask()
	{
		String pluginsDir = ConfigurationSettings.getPluginsDir();
		PluginClassLoader loader = new PluginClassLoader(new File(pluginsDir));
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
				throw new IllegalStateException("Task is not set yet!");
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