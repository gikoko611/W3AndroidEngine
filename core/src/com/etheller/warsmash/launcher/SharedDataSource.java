package com.etheller.warsmash.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;

import com.etheller.warsmash.datasources.DataSource;

/**
 * Game data opened once and read from two threads, the window's (the launcher and the games it starts) and the
 * catalog's: one read at a time.
 */
public final class SharedDataSource implements DataSource {
	private final DataSource data;

	public SharedDataSource(final DataSource data) {
		this.data = data;
	}

	@Override
	public synchronized InputStream getResourceAsStream(final String filepath) throws IOException {
		return this.data.getResourceAsStream(filepath);
	}

	@Override
	public synchronized File getFile(final String filepath) throws IOException {
		return this.data.getFile(filepath);
	}

	@Override
	public synchronized File getDirectory(final String filepath) throws IOException {
		return this.data.getDirectory(filepath);
	}

	@Override
	public synchronized ByteBuffer read(final String path) throws IOException {
		return this.data.read(path);
	}

	@Override
	public synchronized boolean has(final String filepath) {
		return this.data.has(filepath);
	}

	@Override
	public synchronized String findFirst(final String... filepaths) {
		return this.data.findFirst(filepaths);
	}

	@Override
	public synchronized Collection<String> getListfile() {
		return this.data.getListfile();
	}

	@Override
	public synchronized void close() throws IOException {
		this.data.close();
	}
}
