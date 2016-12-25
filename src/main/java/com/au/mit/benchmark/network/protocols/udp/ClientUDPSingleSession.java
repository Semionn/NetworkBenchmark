package com.au.mit.benchmark.network.protocols.udp;

import com.au.mit.benchmark.network.common.AbstractClient;
import com.au.mit.benchmark.network.common.ClientParams;
import com.au.mit.benchmark.network.common.ProtobufMessage;
import com.au.mit.benchmark.network.common.exceptions.CommunicationException;
import com.au.mit.benchmark.network.common.exceptions.WrongResponseException;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Level;

public class ClientUDPSingleSession extends AbstractClient {
    private static int CONNECTION_RETRIES = 10;
    private static int RETRY_TIME_MS = 100;
    public ClientUDPSingleSession(ClientParams params) {
        super(params);
    }

    @Override
    protected boolean connectImpl(String hostname, int port, int clientNum) {
        for (int k = 0; k < X; k++) {
            try (DatagramSocket socket = new DatagramSocket()) {
                try {
                    final int[] array = generateArray(N);
                    final ProtobufMessage.Message message = generateMessage(array);
                    final byte[] messageBytes = message.toByteArray();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(messageBytes.length + Integer.BYTES * 2);
                    byteBuffer.putInt(messageBytes.length);
                    int hachcode = Arrays.hashCode(messageBytes);
                    byteBuffer.putInt(hachcode);
                    byteBuffer.put(messageBytes);
                    byteBuffer.flip();

                    DatagramPacket packet = new DatagramPacket(byteBuffer.array(), byteBuffer.capacity(),
                            InetAddress.getByName(hostname), port + clientNum);
                    sendMessageUDP(socket, packet);

                    Arrays.sort(array);

                    byte[] responseBytes = new byte[messageBytes.length * 3 / 2];
                    DatagramPacket result = new DatagramPacket(responseBytes, responseBytes.length);
                    if (!tryReceive(socket, result)) {
                        continue;
                    }
                    ByteBuffer responseBuffer = ByteBuffer.wrap(responseBytes);
                    int responseSize = responseBuffer.getInt();

                    final byte[] responseMessageBytes = Arrays.copyOfRange(responseBytes, Integer.BYTES, Integer.BYTES + responseSize);
                    final ProtobufMessage.Message responseMessage = ProtobufMessage.Message.parseFrom(responseMessageBytes);

                    boolean isEquals = true;
                    for (int i = 0; i < array.length; ++i) {
                        if (responseMessage.getBody(i) != array[i]) {
                            isEquals = false;
                        }
                    }
                    if (!isEquals) {
                        throw new WrongResponseException("Arrays is not equals!");
                    }
                    if (Delta > 0) {
                        Thread.sleep(Delta);
                    }
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Client thread was interrupted", e);
                }

            } catch (IOException e) {
                logger.log(Level.WARNING, "Communication exception occurred", e);
                throw new CommunicationException(e);
            }
        }
        return true;
    }

    @Override
    public void disconnect() {

    }

    private void sendMessageUDP(DatagramSocket socket, DatagramPacket packet) throws CommunicationException {
        try {
            for (int i = 0; i < CONNECTION_RETRIES; i++) {
                try {
                    socket.send(packet);
                    break;
                } catch (IOException e) {
                    logger.info(String.format("Attempt %d: Message sending failed due to error: %s", i, e.getMessage()));
                    Thread.sleep(RETRY_TIME_MS);
                }
            }
        } catch (InterruptedException ignored) { }
    }

    private boolean tryReceive(DatagramSocket socket, DatagramPacket packet) {
        try {
            socket.setSoTimeout(200);
            for (int i = 0; i < CONNECTION_RETRIES; i++) {
                try {
                    socket.receive(packet);
                    return true;
                } catch (SocketTimeoutException ignored) {
                } catch (IOException e) {
                    logger.info(String.format("Attempt %d: Connection opening at port %d failed due to error: %s", i,
                            socket.getLocalPort(), e.getMessage()));
                    Thread.sleep(RETRY_TIME_MS);
                }
            }
        } catch (InterruptedException ignored) {

        } catch (SocketException e) {
            logger.log(Level.WARNING, "Something gone wrong", e);
        }
        return false;
    }
}
