package com.etheller.warsmash.datasources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;

/** Game data another owner opened, read as it is; closing it here leaves it open for that owner to close. */
public final class BorrowedDataSource implements DataSource {
	private final DataSource data;

	public BorrowedDataSource(final DataSource data) {
		this.data = data;
	}

	@Override
	public InputStream getResourceAsStream(final String filepath) throws IOException {
		return this.data.getResourceAsStream(filepath);
	}

	@Override
	public File getFile(final String filepath) throws IOException {
		return this.data.getFile(filepath);
	}

	@Override
	public File getDirectory(final String filepath) throws IOException {
		return this.data.getDirectory(filepath);
	}

	@Override
	public ByteBuffer read(final String path) throws IOException {
		return this.data.read(path);
	}

	@Override
	public boolean has(final String filepath) {
		return this.data.has(filepath);
	}

	@Override
	public String findFirst(final String... filepaths) {
		return this.data.findFirst(filepaths);
	}

	@Override
	public Collection<String> getListfile() {
		return this.data.getListfile();
	}

	@Override
	public void close() {
	}
}
