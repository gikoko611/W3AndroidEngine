package com.etheller.warsmash.parsers.jass;

import java.util.Scanner;

import com.etheller.warsmash.util.Log;

public class Tmpgen2 {

	public static void main(final String[] args) {
		// final HandleJassType eventType = globals.registerHandleType("event");

		final Scanner scanner = new Scanner(System.in);
		while (scanner.hasNextLine()) {
			final String line = scanner.nextLine();
			final String[] splitLine = line.trim().split("\\s+");
			if (line.trim().startsWith("//")) {
				Log.info(line);
			}
			else {
				if (splitLine.length > 3) {
					Log.info(splitLine[2] + ",");
				}
				else {
					Log.info("");
				}
			}
		}

	}

}
