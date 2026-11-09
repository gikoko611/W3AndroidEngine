package com.etheller.warsmash.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etheller.warsmash.datasources.FolderDataSource;
import com.etheller.warsmash.viewer5.handlers.w3x.simulation.campaign.CCampaignProgress;
import com.etheller.warsmash.viewer5.handlers.w3x.ui.menu.CampaignCatalog;

/** The Forsaken Kingdom section of the campaign list comes only with data that has its campaigns. */
class CampaignChoiceTest {
	@TempDir
	File temp;

	private static final String COMBINED_LIST = "[Index]\r\nCampaignList=\"\",\"Tutorial\",\"Human\",\"\",\"NightElfEx\"\r\n"
			+ "[Tutorial]\r\nHeader=\"Prologue\"\r\nName=\"Exodus of the Horde\"\r\nDefaultOpen=1\r\n"
			+ "[Human]\r\nHeader=\"Human\"\r\nName=\"The Scourge of Lordaeron\"\r\nDefaultOpen=1\r\n"
			+ "[NightElfEx]\r\nHeader=\"Sentinels\"\r\nName=\"Terror of the Tides\"\r\nDefaultOpen=1\r\n";

	private CampaignCatalog catalog(final String name, final String... extraFiles) throws IOException {
		final File root = new File(this.temp, name);
		write(new File(root, "UI/CampaignInfoClassic.txt"), COMBINED_LIST);
		for (final String extra : extraFiles) {
			write(new File(root, extra), "map");
		}
		return CampaignCatalog.load(new FolderDataSource(root.toPath()), "UI\\CampaignInfo.txt");
	}

	private static void write(final File file, final String text) throws IOException {
		file.getParentFile().mkdirs();
		Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
	}

	private static long count(final List<CampaignChoice.ListLine> lines, final int kind) {
		return lines.stream().filter(line -> line.kind == kind).count();
	}

	@Test
	void combinedListWithoutForsakenKingdomEndsWithTheExpansion() throws IOException {
		final CampaignCatalog catalog = catalog("reforged2");
		assertTrue(catalog.isUnified());
		assertFalse(catalog.hasForsakenKingdom());
		final List<CampaignChoice.ListLine> lines = CampaignChoice.campaignList(catalog,
				CCampaignProgress.emptySnapshot());
		assertEquals(0, count(lines, CampaignChoice.ListLine.COMING));
		assertEquals(2, count(lines, CampaignChoice.ListLine.SECTION));
		assertEquals(CampaignChoice.ListLine.CAMPAIGN, lines.get(lines.size() - 1).kind);
	}

	@Test
	void forsakenKingdomMapsAddTheComingSection() throws IOException {
		final CampaignCatalog catalog = catalog("reforged3", "Campaign/ForsakenKingdom/HumanRe01.w3xd",
				"Campaign/ForsakenKingdom/UndeadRe01.w3xd");
		assertTrue(catalog.hasForsakenKingdom());
		final List<CampaignChoice.ListLine> lines = CampaignChoice.campaignList(catalog,
				CCampaignProgress.emptySnapshot());
		assertEquals(CampaignChoice.FORSAKEN_CAMPAIGNS, count(lines, CampaignChoice.ListLine.COMING));
		final CampaignChoice.ListLine header = lines.get(lines.size() - 1 - CampaignChoice.FORSAKEN_CAMPAIGNS);
		assertEquals(CampaignChoice.ListLine.SECTION, header.kind);
		assertEquals(CampaignChoice.SECTION_FORSAKEN, header.index);
	}
}
