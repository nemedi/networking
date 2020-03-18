package client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Client implements AutoCloseable {
	
	private volatile boolean running;

	public Client(int port) throws SocketException {
		final DatagramSocket socket = new DatagramSocket();
		socket.setBroadcast(true);
		final InetAddress address = getBroadcastAddresses().get(0);
		new Thread(() -> {
			running = true;
			final String message = "ping";
			while (!socket.isClosed() && running) {
				try {
					byte[] buffer = message.getBytes();
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
							address, port);
					socket.send(packet);
					Thread.sleep(8000);
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}
			if (!socket.isClosed()) {
				socket.close();
			}
		}).start();
		new Thread(() -> {
			while (!socket.isClosed()) {
				try {
					byte[] buffer = new byte[256];
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);
					System.out.println(MessageFormat.format(
							"received {0} from {1}:{2}",
							new String(packet.getData()),
							packet.getAddress(),
							Integer.toString(packet.getPort())));
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}
		}).start();
	}

	private List<InetAddress> getBroadcastAddresses()
			throws SocketException {
		List<InetAddress> addresses = new ArrayList<InetAddress>();
		for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
				e.hasMoreElements();) {
			NetworkInterface networkInterface = e.nextElement();
			if (networkInterface.isUp() && !networkInterface.isLoopback()) {
				addresses.addAll(networkInterface.getInterfaceAddresses().stream()
						.filter(address -> address.getBroadcast() != null)
						.map(address -> address.getBroadcast())
						.collect(Collectors.toList()));
			}
		}
		return Collections.unmodifiableList(addresses);
	}

	@Override
	public void close() throws Exception {
		running = false;
	}

	public static void main(String[] args) {
		try (Client client = new Client(Integer.parseInt(args[0]));
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
