package com.hiveworkshop.blizzard.casc.nio;

public class DataNotLocalException extends MalformedCASCStructureException {
	private static final long serialVersionUID = 1L;

	public DataNotLocalException(final String message) {
		super(message);
	}
}
