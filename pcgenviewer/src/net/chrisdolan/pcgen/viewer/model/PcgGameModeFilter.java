package net.chrisdolan.pcgen.viewer.model;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pcgen.core.Campaign;
import pcgen.core.GameMode;
import pcgen.persistence.CampaignFileLoader.CampaignFilter;
import pcgen.persistence.GameModeFileLoader.GameModeFilter;


public class PcgGameModeFilter implements GameModeFilter, CampaignFilter {
	private String gameModeName;
	private String campaignName;
	public PcgGameModeFilter(File pcgFile) throws IOException {
		FileReader fileReader = new FileReader(pcgFile);
		try {
			LineNumberReader reader = new LineNumberReader(fileReader);
			try {
				Pattern gmp = Pattern.compile("\\A\\s*GAMEMODE\\s*:\\s*(.+?)\\s*");
				Pattern gmc = Pattern.compile("\\A\\s*CAMPAIGN\\s*:\\s*(.+?)\\s*");
				while (true) {
					String line = reader.readLine();
					if (line == null)
						break;
					Matcher matcher = gmp.matcher(line);
					if (matcher.matches()) {
						this.gameModeName = matcher.group(1);
						if (this.campaignName != null)
							break;
					}
					matcher = gmc.matcher(line);
					if (matcher.matches()) {
						this.campaignName = matcher.group(1);
						if (this.gameModeName != null)
							break;
					}
				}
			} finally {
				reader.close();
			}
		} finally {
			fileReader.close();
		}
		if (gameModeName == null)
			throw new IllegalArgumentException("File does not have a game mode: " + pcgFile);
		if (campaignName == null)
			throw new IllegalArgumentException("File does not have a campaign: " + pcgFile);
	}

	public boolean acceptDir(File gameModeDir) {
		//System.out.println("Compare " + gameModeName + " to dir " + gameModeDir);
		return gameModeName.equals(gameModeDir.getName());
	}
	public boolean acceptMode(GameMode gameMode) {
		//System.out.println("Compare " + gameModeName + " to mode " + gameMode.getName() + "/" + gameMode.getDisplayName());
		return gameModeName.equals(gameMode.getName());
	}
	public boolean acceptPccFile(File campaignFile) {
		return true; // no basis for rejection yet...
	}
	public boolean acceptCampaign(Campaign campaign) {
		//System.out.println("Compare '" + campaignName + "' to campaign '" + campaign.getName() + "'/'" + campaign.getDisplayName() + "'");
		return campaignName.equals(campaign.getName());
	}
}
