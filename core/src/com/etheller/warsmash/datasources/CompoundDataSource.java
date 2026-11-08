package com.etheller.warsmash.datasources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CompoundDataSource implements DataSource {
	private final List<DataSource> mpqList = new ArrayList<>();

	public CompoundDataSource(final List<DataSource> dataSources) {
		if (dataSources != null) {
			for (final DataSource dataSource : dataSources) {
				this.mpqList.add(dataSource);
			}
		}
		loadFileAliases();
	}

	Map<String, File> cache = new HashMap<>();
	// Index of the source that has each path, or MISSING. The sources do not change while this instance lives,
	// and the same paths are asked for many times (prefixes, extensions, aliases).
	private static final int MISSING = -1;
	private final Map<String, Integer> sourceIndexCache = new ConcurrentHashMap<>();

	// Data prepared as a folder has many files only under their alias target, and only CASC resolves aliases itself.
	// The path as asked wins, so CASC sources behave as before.
	private Map<String, String> fileAliases = Collections.emptyMap();

	private void loadFileAliases() {
		this.fileAliases = Collections.emptyMap();
		final int index = findSourceIndex("filealiases.json");
		if (index == MISSING) {
			return;
		}
		try (InputStream stream = this.mpqList.get(index).getResourceAsStream("filealiases.json")) {
			if (stream != null) {
				this.fileAliases = CascDataSource.cachedFileAliases(stream);
			}
		}
		catch (final IOException | RuntimeException e) {
			e.printStackTrace();
		}
	}

	private String resolveAlias(final String filepath) {
		if (this.fileAliases.isEmpty()) {
			return null;
		}
		return this.fileAliases.get(filepath.toLowerCase(Locale.US).replace('/', '\\').replace(':', '\\'));
	}

	private int findSourceIndex(final String filepath) {
		final Integer cached = this.sourceIndexCache.get(filepath);
		if (cached != null) {
			return cached;
		}
		int found = MISSING;
		for (int i = this.mpqList.size() - 1; i >= 0; i--) {
			if (this.mpqList.get(i).has(filepath)) {
				found = i;
				break;
			}
		}
		this.sourceIndexCache.put(filepath, found);
		return found;
	}

	@Override
	public File getFile(final String filepath) {
		File file = getFileFromSources(filepath);
		if (file == null) {
			final String alias = resolveAlias(filepath);
			if (alias != null) {
				file = getFileFromSources(alias);
				if (file != null) {
					this.cache.put(filepath, file);
				}
			}
		}
		return file;
	}

	private File getFileFromSources(final String filepath) {
		if (this.cache.containsKey(filepath)) {
			return this.cache.get(filepath);
		}
		try {
			for (int i = this.mpqList.size() - 1; i >= 0; i--) {
				final DataSource mpq = this.mpqList.get(i);
				final File tempProduct = mpq.getFile(filepath);
				if (tempProduct != null) {
					this.cache.put(filepath, tempProduct);
					return tempProduct;
				}
			}
		}
		catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public File getDirectory(String filepath) throws IOException {
		try {
			for (int i = this.mpqList.size() - 1; i >= 0; i--) {
				final DataSource mpq = this.mpqList.get(i);
				final File tempProduct = mpq.getDirectory(filepath);
				if (tempProduct != null) {
					return tempProduct;
				}
			}
		}
		catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public ByteBuffer read(final String path) throws IOException {
		final ByteBuffer buffer = readFromSources(path);
		if (buffer == null) {
			final String alias = resolveAlias(path);
			if (alias != null) {
				return readFromSources(alias);
			}
		}
		return buffer;
	}

	private ByteBuffer readFromSources(final String path) {
		try {
			for (int i = this.mpqList.size() - 1; i >= 0; i--) {
				final DataSource mpq = this.mpqList.get(i);
				final ByteBuffer buffer = mpq.read(path);
				if (buffer != null) {
					return buffer;
				}
			}
		}
		catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public InputStream getResourceAsStream(final String filepath) {
		final InputStream stream = getResourceAsStreamFromSources(filepath);
		if (stream == null) {
			final String alias = resolveAlias(filepath);
			if (alias != null) {
				return getResourceAsStreamFromSources(alias);
			}
		}
		return stream;
	}

	private InputStream getResourceAsStreamFromSources(final String filepath) {
		try {
			final Integer cached = this.sourceIndexCache.get(filepath);
			if ((cached != null) && (cached != MISSING)) {
				final InputStream resourceAsStream = this.mpqList.get(cached).getResourceAsStream(filepath);
				if (resourceAsStream != null) {
					return resourceAsStream;
				}
			}
			for (int i = this.mpqList.size() - 1; i >= 0; i--) {
				final DataSource mpq = this.mpqList.get(i);
				final InputStream resourceAsStream = mpq.getResourceAsStream(filepath);
				if (resourceAsStream != null) {
					return resourceAsStream;
				}
			}
		}
		catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean has(final String filepath) {
		if (this.cache.containsKey(filepath)) {
			return true;
		}
		if (findSourceIndex(filepath) != MISSING) {
			return true;
		}
		final String alias = resolveAlias(filepath);
		return (alias != null) && (findSourceIndex(alias) != MISSING);
	}

	@Override
	public String findFirst(final String... filepaths) {
		// The latest source with any of the paths decides, so a file in an override folder wins over the game data
		// even when it has another of the names (a .blp beside the game's .dds).
		for (int i = this.mpqList.size() - 1; i >= 0; i--) {
			final String found = this.mpqList.get(i).findFirst(filepaths);
			if (found != null) {
				return found;
			}
		}
		for (final String filepath : filepaths) {
			final String alias = resolveAlias(filepath);
			if ((alias != null) && (findSourceIndex(alias) != MISSING)) {
				return filepath;
			}
		}
		return null;
	}

	public void refresh(final List<DataSourceDescriptor> dataSourceDescriptors) {
		for (final DataSource dataSource : this.mpqList) {
			try {
				dataSource.close();
			}
			catch (final NullPointerException e) {
				e.printStackTrace();
			}
			catch (final IOException e) {
				e.printStackTrace();
			}
		}
		this.cache.clear();
		this.sourceIndexCache.clear();
		this.mpqList.clear();
		if (dataSourceDescriptors != null) {
			for (final DataSourceDescriptor descriptor : dataSourceDescriptors) {
				this.mpqList.add(descriptor.createDataSource());
			}
		}
		loadFileAliases();
	}

	public interface LoadedMPQ {
		void unload();

		boolean hasListfile();

		boolean has(String path);
	}

	public Set<String> getMergedListfile() {
		final Set<String> listfile = new HashSet<>();
		for (final DataSource mpqGuy : this.mpqList) {
			final Collection<String> dataSourceListfile = mpqGuy.getListfile();
			if (dataSourceListfile != null) {
				for (final String element : dataSourceListfile) {
					listfile.add(element);
				}
			}
		}
		return listfile;
	}

	@Override
	public Collection<String> getListfile() {
		return getMergedListfile();
	}

	@Override
	public void close() throws IOException {
		for (final DataSource mpqGuy : this.mpqList) {
			mpqGuy.close();
		}
	}
}
