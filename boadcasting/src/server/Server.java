package server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.Scanner;

public class Server implements AutoCloseable {
	
	private volatile boolean running;

	private Server(int port) throws SocketException {
		final DatagramSocket socket = new DatagramSocket(null);
		socket.setReuseAddress(true);
		socket.bind(new InetSocketAddress(port));
		new Thread(() -> {
			running = true;
			final String message = "pong";
			while (!socket.isClosed() && running) {
				try {
					byte[] buffer = new byte[256];
					DatagramPacket packet = new DatagramPacket(buffer,
							buffer.length);
					socket.receive(packet);
					System.out.println(MessageFormat.format(
							"received {0} from {1}:{2}",
							new String(packet.getData()),
							packet.getAddress(),
							Integer.toString(packet.getPort())));
					buffer = message.getBytes();
					packet = new DatagramPacket(buffer, buffer.length,
							packet.getAddress(), packet.getPort());
					socket.send(packet);
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}
			if (!socket.isClosed()) {
				socket.close();
			}
		}).start();
	}

	@Override
	public void close() throws Exception {
		running = false;
	}

	public static void main(String[] args) {
		try (Server server = new Server(Integer.parseInt(args[0]));
				Scanner scanner = new Scanner(System.in)) {
			while (true) {
				if (scanner.hasNextLine() && "exit".equals(scanner.nextLine())) {
					break;
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			System.exit(0);
		}
	}

}
