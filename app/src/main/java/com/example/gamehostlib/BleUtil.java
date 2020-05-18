package com.example.gamehostlib;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final public class BleUtil {
    private final static String TAG=BleUtil.class.getSimpleName();
    public static BleAdvertisedData parseAdertisedData(byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<UUID>();
        String name = null;
        if( advertisedData == null ){
            cSocketHelper.printLogForUnity("data null");
            return new BleAdvertisedData(uuids, name);
        }else{
            cSocketHelper.printLogForUnity("data not null");
        }

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    cSocketHelper.printLogForUnity("type : 16" );
                    break;
                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    cSocketHelper.printLogForUnity("type : 128" );
                    break;
                case 0x09:
                    byte[] nameBytes = new byte[length-1];
                    buffer.get(nameBytes);
                    try {
                        name = new String(nameBytes, "utf-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    cSocketHelper.printLogForUnity("type : urf8" );
                    break;
                default:
                    buffer.position(buffer.position() + length - 1);
                    cSocketHelper.printLogForUnity("type : default " +buffer.remaining());
                    break;
            }
        }
        return new BleAdvertisedData(uuids, name);
    }
}


