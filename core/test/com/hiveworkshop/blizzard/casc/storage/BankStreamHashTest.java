package com.hiveworkshop.blizzard.casc.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.hiveworkshop.blizzard.casc.Key;
import com.hiveworkshop.blizzard.casc.nio.DataNotLocalException;
import com.hiveworkshop.blizzard.casc.nio.HashMismatchException;
import com.hiveworkshop.blizzard.casc.nio.MalformedCASCStructureException;

/**
 * Stored data is checked before it is used: the container's key, the key of BLTE data stored without a container,
 * and the MD5 of every bank.
 */
class BankStreamHashTest {
	private static final Key OTHER_KEY = new Key(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 });

	private static byte[] md5(final byte[] data) throws Exception {
		return MessageDigest.getInstance("MD5").digest(data);
	}

	private static byte[] blte(final byte[][] banks, final byte[][] hashes, final int[] decodedSizes) {
		final ByteArrayOutputStream blte = new ByteArrayOutputStream();
		final int headerSize = 8 + 4 + (banks.length * 24);
		final ByteBuffer header = ByteBuffer.allocate(headerSize).order(ByteOrder.BIG_ENDIAN);
		header.put("BLTE".getBytes(StandardCharsets.US_ASCII)).putInt(headerSize);
		header.put((byte) 0x0F).put((byte) 0).put((byte) 0).put((byte) banks.length);
		for (int i = 0; i < banks.length; i++) {
			header.putInt(banks[i].length).putInt(decodedSizes[i]).put(hashes[i]);
		}
		blte.write(header.array(), 0, headerSize);
		for (final byte[] bank : banks) {
			blte.write(bank, 0, bank.length);
		}
		return blte.toByteArray();
	}

	private static ByteBuffer container(final byte[] headerKey, final byte[] body) {
		final ByteBuffer buffer = ByteBuffer.allocate(30 + body.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(headerKey).putInt(30 + body.length).putShort((short) 0).putInt(0).putInt(0).put(body);
		buffer.flip();
		return buffer;
	}

	private static byte[] plain(final String text) {
		final byte[] data = text.getBytes(StandardCharsets.UTF_8);
		final byte[] bank = new byte[data.length + 1];
		bank[0] = 'N';
		System.arraycopy(data, 0, bank, 1, data.length);
		return bank;
	}

	private static byte[] read(final BankStream stream, final int size) throws Exception {
		final ByteBuffer out = ByteBuffer.allocate(size);
		while (stream.hasNextBank()) {
			stream.getBank(out);
		}
		return out.array();
	}

	@Test
	void goodHashesPassAndDataDecodes() throws Exception {
		final byte[] a = plain("hello ");
		final byte[] b = plain("world");
		final BankStream stream = new BankStream(
				container(new byte[16], blte(new byte[][] { a, b }, new byte[][] { md5(a), md5(b) }, new int[] { 6, 5 })),
				null);
		assertTrue(stream.verifyBankHashes());
		assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8), read(stream, 11));
	}

	@Test
	void changedByteIsCaught() throws Exception {
		final byte[] a = plain("hello ");
		final byte[] b = plain("world");
		final byte[] hashB = md5(b);
		b[3] = 'X';
		final BankStream stream = new BankStream(
				container(new byte[16], blte(new byte[][] { a, b }, new byte[][] { md5(a), hashB }, new int[] { 6, 5 })),
				null);
		assertThrows(HashMismatchException.class, stream::verifyBankHashes);
	}

	@Test
	void zeroHeaderMeansNotDownloaded() throws Exception {
		final byte[] a = plain("abc");
		final ByteBuffer data = container(new byte[16], blte(new byte[][] { a }, new byte[][] { md5(a) }, new int[] { 3 }));
		assertThrows(DataNotLocalException.class, () -> new BankStream(data, OTHER_KEY));
	}

	@Test
	void otherKeyMismatchNamesBothKeys() throws Exception {
		final byte[] a = plain("abc");
		final byte[] header = new byte[16];
		header[15] = 0x77;
		final ByteBuffer data = container(header, blte(new byte[][] { a }, new byte[][] { md5(a) }, new int[] { 3 }));
		final MalformedCASCStructureException error = assertThrows(MalformedCASCStructureException.class,
				() -> new BankStream(data, OTHER_KEY));
		assertFalse(error instanceof DataNotLocalException);
		assertTrue(error.getMessage().contains("index 010203040506070809"), error.getMessage());
	}

	@Test
	void blteWithoutContainerIsMatchedByItsHeader() throws Exception {
		final byte[] a = plain("hello ");
		final byte[] b = plain("world");
		final byte[] body = blte(new byte[][] { a, b }, new byte[][] { md5(a), md5(b) }, new int[] { 6, 5 });
		final int headerSize = ByteBuffer.wrap(body).getInt(4);
		final Key key = new Key(Arrays.copyOf(md5(Arrays.copyOf(body, headerSize)), 9));
		assertThrows(MalformedCASCStructureException.class, () -> new BankStream(ByteBuffer.wrap(body), OTHER_KEY));
		final BankStream stream = new BankStream(ByteBuffer.wrap(body), key);
		assertTrue(stream.verifyBankHashes());
		assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8), read(stream, 11));
	}

	@Test
	void zeroHashesAreSkipped() throws Exception {
		final byte[] a = plain("abc");
		final BankStream stream = new BankStream(
				container(new byte[16], blte(new byte[][] { a }, new byte[][] { new byte[16] }, new int[] { 3 })), null);
		assertFalse(stream.verifyBankHashes());
	}
}
