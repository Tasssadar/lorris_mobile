package com.tassadar.lorrismobile.connections;


public interface ShupitoConnInterface extends ConnectionInterface {
    public void descRead(ShupitoDesc desc);

    // Standard read(byte[] data) is used instead
    //public void packetRead(byte[] packet);
}