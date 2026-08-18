package com.ntnh.herald.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/** Canonical address value. Equality and persistence identity use address bytes, not presentation text. */
public final class IpAddress {

    private final byte[] bytes;
    private final String text;
    private final String key;

    private IpAddress(byte[] bytes) throws UnknownHostException {
        this.bytes = Arrays.copyOf(bytes, bytes.length);
        this.text = InetAddress.getByAddress(this.bytes)
            .getHostAddress();
        this.key = toHex(this.bytes);
    }

    public static IpAddress from(InetAddress address) {
        if (address == null) throw new IllegalArgumentException("address cannot be null");
        try {
            return new IpAddress(address.getAddress());
        } catch (UnknownHostException impossible) {
            throw new IllegalArgumentException("Invalid IP address", impossible);
        }
    }

    public static IpAddress parse(String address) throws UnknownHostException {
        return from(InetAddress.getByName(address));
    }

    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public String getText() {
        return text;
    }

    String getKey() {
        return key;
    }

    private static String toHex(byte[] value) {
        char[] result = new char[value.length * 2];
        char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < value.length; i++) {
            int current = value[i] & 0xff;
            result[i * 2] = digits[current >>> 4];
            result[i * 2 + 1] = digits[current & 0x0f];
        }
        return new String(result);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof IpAddress && Arrays.equals(bytes, ((IpAddress) other).bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return text;
    }
}
