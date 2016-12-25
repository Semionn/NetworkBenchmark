package com.au.mit.benchmark.network.protocols.udp;

import com.au.mit.benchmark.network.common.AbstractClientHandler;
import com.au.mit.benchmark.network.common.ProtobufMessage;
import com.au.mit.benchmark.network.common.exceptions.CommunicationException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UDPClientHandler extends AbstractClientHandler<DatagramSocket> {
    private Logger logger = Logger.getLogger(UDPClientHandler.class.getName());
    private final DatagramSocket serverSocket;
    private final UDPServer server;
    private final int X;

    public UDPClientHandler(DatagramSocket serverSocket, UDPServer server, int X) {
        super(serverSocket, server);
        this.serverSocket = serverSocket;
        this.server = server;
        this.X = X;
    }

    @Override
    protected void processClient() {
        try {
            final int packetSize = serverSocket.getReceiveBufferSize();
            for (int k = 0; k < X; k++) {
                DatagramPacket packet = new DatagramPacket(new byte[packetSize], packetSize);
                if (!tryReceive(packet)) {
                    return;
                }
                final byte[] data = packet.getData();
                ByteBuffer packetBuffer = ByteBuffer.wrap(data);
                int messageSize = packetBuffer.getInt();
                int hashcode = packetBuffer.getInt();
                byte[] messageBytes = new byte[messageSize];
                packetBuffer.get(messageBytes);
                if (Arrays.hashCode(messageBytes) != hashcode) {
                    throw new CommunicationException("Packet is corrupted");
                }
                final ProtobufMessage.Message message = ProtobufMessage.Message.parseFrom(messageBytes);
                final ProtobufMessage.Message response = processMessage(message);
                final byte[] responseBytes = response.toByteArray();
                ByteBuffer responseBuffer = ByteBuffer.allocate(Integer.BYTES + responseBytes.length);
                responseBuffer.putInt(responseBytes.length);
                responseBuffer.put(responseBytes);
                DatagramPacket result = new DatagramPacket(responseBuffer.array(), responseBuffer.capacity(),
                        packet.getAddress(), packet.getPort());
                sendMessageUDP(serverSocket,result);
            }
        } catch (IOException e) {
            server.logException(e);
        } finally {
            serverSocket.close();
            server.detachHandler(this);
        }
    }

    @Override
    protected void processClient(boolean arg) {

    }

    private static int CONNECTION_RETRIES = 10;
    private static int RETRY_TIME_MS = 100;
    private boolean tryReceive(DatagramPacket packet) {
        try {
            serverSocket.setSoTimeout(200);
            for (int i = 0; i < CONNECTION_RETRIES; i++) {
                try {
                    serverSocket.receive(packet);
                    return true;
                } catch (SocketTimeoutException e1) {
                } catch (IOException e) {
                    logger.info(String.format("Attempt %d: Connection opening at port %d failed due to error: %s", i,
                            serverSocket.getLocalPort(), e.getMessage()));
                    Thread.sleep(RETRY_TIME_MS);
                }
            }
        } catch (InterruptedException ignored) {

        } catch (SocketException e) {
            logger.log(Level.WARNING, "Something gone wrong", e);
        }
        return false;
    }

    private void sendMessageUDP(DatagramSocket socket, DatagramPacket packet) throws CommunicationException {
        try {
            for (int i = 0; i < CONNECTION_RETRIES; i++) {
                try {
                    socket.send(packet);
                    break;
                } catch (SocketTimeoutException ignored) {
                } catch (IOException e) {
                    logger.info(String.format("Attempt %d: Message sending failed due to error: %s", i, e.getMessage()));
                    Thread.sleep(RETRY_TIME_MS);
                }
            }
        } catch (InterruptedException ignored) { }
    }
}
