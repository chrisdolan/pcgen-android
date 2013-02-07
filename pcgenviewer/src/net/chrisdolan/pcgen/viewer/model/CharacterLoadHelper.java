package net.chrisdolan.pcgen.viewer.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

	public static final class Options {
		private boolean htmlWanted;
		private File htmlTemplate;

		public boolean isHtmlWanted() {
			return htmlWanted;
		}
		public Options setHtmlWanted(boolean htmlWanted) {
			this.htmlWanted = htmlWanted;
			return this;
		}
		public File getHtmlTemplate() {
			return htmlTemplate;
		}
		public Options setHtmlTemplate(File htmlTemplate) {
			this.htmlTemplate = htmlTemplate;
			return this;
		}
	}

	public interface Result {
		PlayerCharacter getPlayerCharacter();
		DataSetFacade getDataSetFacade();
		String getHtml();
	}

	public void setPluginClassLoader(PluginClassLoader pluginClassLoader) {
		this.pluginClassLoader = pluginClassLoader;
		
	}
	public Result load(final File pcgFile, final UIDelegate uiDelegate, PCGenTaskListener taskListener, final Options options) {
		final AtomicReference<Result> pcRef = new AtomicReference<Result>();
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
				final DataSetFacade data = ((SourceFileLoader)sourceLoaderTask.getTask()).getDataSetFacade();
				if (data == null)
					throw new NullPointerException("data set facade is null for sources of " + pcgFile);
				setProgress("Open character", 10);
				final PlayerCharacter pc = generatePC(pcgFile, data, uiDelegate);
				
				//CharacterFacade character = CharacterManager.openCharacter(pcgFile, uiDelegate, data);
				setProgress("Loaded " + pcgFile.getName(), 100);
				pcRef.set(new Result() {
					public PlayerCharacter getPlayerCharacter() {
						return pc;
					}
					public DataSetFacade getDataSetFacade() {
						return data;
					}
					public String getHtml() {
						return null;
					}
				});
			}
		});
		if (options.isHtmlWanted()) {
			if (null == options.htmlTemplate)
				throw new IllegalArgumentException("Missing options.htmlTemplate");
			executor.addPCGenTask(new PCGenTask() {
				{
					setMaximum(100);
				}
				public void execute() {
					setProgress("Generate HTML", 0);
					Result result = pcRef.get();
					final PlayerCharacter pc = result.getPlayerCharacter();
					final DataSetFacade data = result.getDataSetFacade();
					final String html = generateHtml(pc, options.htmlTemplate);
					setProgress("Loaded " + pcgFile.getName(), 100);
					pcRef.set(new Result() {
						public PlayerCharacter getPlayerCharacter() {
							return pc;
						}
						public DataSetFacade getDataSetFacade() {
							return data;
						}
						public String getHtml() {
							return html;
						}
					});
				}
			});
		}
		executor.execute();
		return pcRef.get();
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

	private String generateHtml(PlayerCharacter pc, File htmlTemplate) {
		StringWriter out = new StringWriter();
		BufferedWriter buf = new BufferedWriter(out);
		ExportHandler handler = new ExportHandler(htmlTemplate);
		handler.write(pc, buf);

		String genText = out.toString();
//		genText = genText.replace(COLOR_TAG, cssColor.getCssText());
		return genText;
//		ByteArrayInputStream instream = new ByteArrayInputStream(genText.getBytes());
//		Document doc = null;
//
//		URI root = new URI("file", ConfigurationSettings.getPreviewDir().replaceAll("\\\\", "/"), null);
//		doc = theDocBuilder.parse(new InputSourceImpl(instream,
//													  root.toString(),
//													  "UTF-8"));
//		return doc;
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