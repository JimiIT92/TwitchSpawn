package net.programmer.igoodie.twitchspawn.tracer;

import io.socket.client.IO;
import io.socket.client.Socket;
import net.programmer.igoodie.twitchspawn.configuration.CredentialsConfig;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

public abstract class SocketIOTracer {

    protected TraceManager manager;
    protected Platform api;
    protected List<Socket> sockets;

    public SocketIOTracer(Platform api, TraceManager manager) {
        this.manager = manager;
        this.api = api;
        this.sockets = new LinkedList<>();
    }

    protected String liveEventChannelName() {
        return "event";
    }

    public abstract void start();

    public abstract void stop();

    protected Socket createSocket(CredentialsConfig.Streamer streamer) {
        checkCredentials(streamer);

        try {
            IO.Options options = generateOptions(streamer);
            Socket socket = IO.socket(api.url, options);

            socket.on(Socket.EVENT_CONNECT, args -> onConnect(socket, streamer, args));
            socket.on(Socket.EVENT_DISCONNECT, args -> onDisconnect(socket, streamer, args));
            socket.on(liveEventChannelName(), args -> onLiveEvent(socket, streamer, args));

            this.sockets.add(socket);

            return socket;

        } catch (URISyntaxException e) {
            throw new InternalError("Invalid URI for " + api.name + " = " + api.url);
        }
    }

    protected void checkCredentials(CredentialsConfig.Streamer streamer) {}

    protected IO.Options generateOptions(CredentialsConfig.Streamer streamer) {
        IO.Options options = new IO.Options();
        options.forceNew = true;
        options.reconnection = false;
        options.transports = new String[]{"websocket"};

        return options;
    }

    protected abstract void onConnect(Socket socket, CredentialsConfig.Streamer streamer, Object... args);

    protected abstract void onDisconnect(Socket socket, CredentialsConfig.Streamer streamer, Object... args);

    protected abstract void onLiveEvent(Socket socket, CredentialsConfig.Streamer streamer, Object... args);

}
