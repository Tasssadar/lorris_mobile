package com.tassadar.lorrismobile.connections;

public class TCPConnProto implements Comparable<TCPConnProto> {
    public String name;
    public String address;
    public int port;

    @Override
    public int compareTo(TCPConnProto other) {
        if(other == null)
            throw new NullPointerException();
        return name.compareTo(other.name);
    }

    public boolean sameAs(TCPConnProto o) {
        return name.equals(o.name) &&
               address.equals(o.address) &&
               port == o.port;
    }
}
