package com.tassadar.lorrismobile.connections;


public abstract class ShupitoConnection extends Connection {

    protected ShupitoConnection() {
        super(CONN_SHUPITO);
    }

    public abstract void sendPacket(byte[] packet);
    public abstract void requestDesc();
}