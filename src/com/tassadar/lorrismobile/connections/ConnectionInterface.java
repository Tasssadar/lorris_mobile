package com.tassadar.lorrismobile.connections;

public interface ConnectionInterface {
    void connected(boolean connected);
    void stateChanged(int state);
    void disconnecting();
    void dataRead(byte[] data);
}