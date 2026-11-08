package com.etheller.warsmash.viewer5.handlers.w3x;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.etheller.warsmash.datasources.FolderDataSource;
import com.etheller.warsmash.units.DataTable;
import com.etheller.warsmash.util.StringBundle;

class DialogueLabelsTest {
	private static final String DIALOGUE = String.join("\n", "ID;PWXL;N;E", "B;X5;Y3;D0",
			"C;X1;Y1;K\"DialogueLabel\"", "C;X2;K\"Filepath\"", "C;X5;K\"FacialAnimationLabel\"",
			"C;X1;Y2;K\"HPitLordPitLordYesYes1\"", "C;X2;K\"Units/Demon/HeroPitLord/HPitLordYes1.flac\"",
			"C;X5;K\"HPitLordYes1\"", "C;X1;Y3;K\"HPitLordYes2\"",
			"C;X2;K\"Units/Demon/HeroPitLord/HPitLordYes2.flac\"", "C;X5;K\"HPitLordYes2\"", "E", "");
	private static final String ACKNOWLEDGEMENTS = String.join("\n", "ID;PWXL;N;E", "B;X2;Y2;D0",
			"C;X1;Y1;K\"SoundName\"", "C;X2;K\"FileNames\"", "C;X1;Y2;K\"HeroPitLordYes\"",
			"C;X2;K\"HPitLordYes1,HPitLordYes2,HPitLordYes9\"", "E", "");

	@Test
	void misspeltLabelIsFoundByItsFacialAnimationLabel(@TempDir final Path folder) throws IOException {
		final Path soundInfo = Files.createDirectories(folder.resolve("UI").resolve("SoundInfo"));
		Files.write(soundInfo.resolve("DialogueDemonBase.slk"), DIALOGUE.getBytes(StandardCharsets.UTF_8));
		final DataTable acknowledgements = new DataTable(StringBundle.EMPTY);
		acknowledgements.readSLK(new ByteArrayInputStream(ACKNOWLEDGEMENTS.getBytes(StandardCharsets.UTF_8)));

		DialogueLabels.resolve(new FolderDataSource(folder), acknowledgements, StringBundle.EMPTY);

		assertEquals("Units\\Demon\\HeroPitLord\\HPitLordYes1,Units\\Demon\\HeroPitLord\\HPitLordYes2,HPitLordYes9",
				acknowledgements.get("HeroPitLordYes").getField("FileNames"));
	}
}
