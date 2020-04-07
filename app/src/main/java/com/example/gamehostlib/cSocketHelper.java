package com.example.gamehostlib;
import android.util.Log;

import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class cSocketHelper {

    public static void connectSocket(String url)
    {
        try {
            SocketClient chatclient = new SocketClient(new URI(url));
            chatclient.connect();
            BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) );
            while ( true ) {
                String line = reader.readLine();
                if( line.equals( "close" ) ) {
                    chatclient.closeBlocking();
                } else if ( line.equals( "open" ) ) {
                    chatclient.reconnect();
                } else {
                    chatclient.send( line );
                }
            }
        }catch (Exception e){
            Log.i("socket","socket exception");
        }
    }

    static class SocketClient extends WebSocketClient {
        public SocketClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            Log.i("socket","socket open");
        }

        @Override
        public void onMessage(String message) {
            Log.i("socket","socket on msg");
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.i("socket","socket on close");
        }

        @Override
        public void onError(Exception ex) {
            Log.i("socket","socket on error");
        }
    }
}
