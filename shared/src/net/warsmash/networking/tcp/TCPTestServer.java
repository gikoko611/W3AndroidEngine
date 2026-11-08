package net.warsmash.networking.tcp;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import net.warsmash.nio.channels.SelectableChannelOpener;
import net.warsmash.nio.channels.SocketChannelCallback;
import net.warsmash.nio.channels.WritableOutput;
import net.warsmash.nio.channels.WritableSocketOutput;
import net.warsmash.nio.channels.tcp.TCPClientParser;
import net.warsmash.nio.util.ExceptionListener;

import com.etheller.warsmash.util.Log;

public class TCPTestServer {
	private static final int PORT = 8989;

	public static void main(final String[] args) {
		final SelectableChannelOpener selectableChannelOpener = new SelectableChannelOpener();
		selectableChannelOpener.openTCPServerChannel(PORT, new SocketChannelCallback() {
			@Override
			public TCPClientParser onConnect(final WritableSocketOutput writableOpenedChannel,
					final SocketAddress remoteAddress) {
				Log.info("Received connection from " + remoteAddress);
				return new TCPClientParser() {
					@Override
					public void parse(final ByteBuffer data) {
						Log.info("Got " + data.remaining() + " bytes from " + remoteAddress);
						if (data.hasRemaining()) {
							Log.infoPart("[");
							Log.infoPart(data.get());
							while (data.hasRemaining()) {
								Log.infoPart(", ");
								Log.infoPart(data.get());
							}
							Log.info("]");
						}
						writableOpenedChannel.write(ByteBuffer.wrap("reply".getBytes()));
					}

					@Override
					public void disconnected() {
						Log.info("Disconnected from " + remoteAddress);
					}
				};
			}
		}, ExceptionListener.THROW_RUNTIME, 8 * 1024 * 1024, ByteOrder.BIG_ENDIAN);

		while (true) {
			selectableChannelOpener.select(0);
		}
	}
}
