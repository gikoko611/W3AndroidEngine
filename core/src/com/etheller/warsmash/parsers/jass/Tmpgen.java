package com.etheller.warsmash.parsers.jass;

import java.util.Scanner;

import com.etheller.warsmash.util.Log;

public class Tmpgen {

	public static void main(final String[] args) {
		// final HandleJassType eventType = globals.registerHandleType("event");

		final Scanner scanner = new Scanner(System.in);
		while (scanner.hasNextLine()) {
			final String line = scanner.nextLine();
			if (line.startsWith("type ")) {
				final String[] splitLine = line.split("\\s+");
				Log.info("final HandleJassType " + splitLine[1] + "Type = globals.registerHandleType(\""
						+ splitLine[1] + "\");");
			}
		}

	}

}
