package com.au.mit.benchmark.network.protocols.tcp;

import com.au.mit.benchmark.network.common.AbstractClient;
import com.au.mit.benchmark.network.common.ClientParams;
import com.au.mit.benchmark.network.common.ProtobufMessage;
import com.au.mit.benchmark.network.common.exceptions.CommunicationException;
import com.au.mit.benchmark.network.common.exceptions.WrongResponseException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;

public class ClientTCPSingleSession extends AbstractClient {
    public ClientTCPSingleSession(ClientParams params) {
        super(params);
    }

    @Override
    public boolean connectImpl(String hostname, int port, int clientNum) {
        return sendRequest(hostname, port, channel -> {
            try {
                DataInputStream inputStream = new DataInputStream(channel.socket().getInputStream());
                DataOutputStream outputStream = new DataOutputStream(channel.socket().getOutputStream());

                for (int k = 0; k < X; k++) {
                    final int[] array = generateArray(N);
                    final ProtobufMessage.Message message = generateMessage(array);

                    final byte[] messageBytes = message.toByteArray();
                    outputStream.writeInt(messageBytes.length);
                    outputStream.write(messageBytes);

                    Arrays.sort(array);
                    int responseSize = inputStream.readInt();
                    byte[] responseBytes = new byte[responseSize];

                    for (int i = 0; i < responseSize; ++i) {
                        responseBytes[i] = inputStream.readByte();
                    }
                    final ProtobufMessage.Message response = ProtobufMessage.Message.parseFrom(responseBytes);
                    boolean isEquals = true;
                    for (int i = 0; i < N; i++) {
                        if (response.getBody(i) != array[i]) {
                            isEquals = false;
                        }
                    }
                    if (!isEquals) {
                        throw new WrongResponseException("Arrays is not equals!");
                    }
                    if (Delta > 0) {
                        Thread.sleep(Delta);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "Communication exception occurred", e);
                throw new CommunicationException(e);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Client thread was interrupted", e);
            }
            return true;
        });
    }

    @Override
    public void disconnect() {

    }
}
