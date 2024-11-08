package org.example.heartbeat;

import io.quarkus.logging.Log;
import jakarta.jws.WebService;

import java.util.List;

@WebService(endpointInterface = "org.example.heartbeat.UICHBMessage")
public class UICHBMessageImpl implements UICHBMessage {

    @Override
    public List<Object> uichbMessage(Object message, Object properties) {
        Log.info("Received heartbeat");
        return List.of("HEART_BEAT_WS_RECEIVED");
    }
}
