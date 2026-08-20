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

    /** Uses dotted IPv4 for IPv4-mapped IPv6 addresses while preserving native IPv6 presentation. */
    public String getDisplayText() {
        if (!isIpv4MappedAddress(bytes)) return text;
        return (bytes[12] & 0xff) + "." + (bytes[13] & 0xff) + "." + (bytes[14] & 0xff) + "." + (bytes[15] & 0xff);
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

    private static boolean isIpv4MappedAddress(byte[] value) {
        if (value.length != 16 || value[10] != (byte) 0xff || value[11] != (byte) 0xff) return false;
        for (int i = 0; i < 10; i++) if (value[i] != 0) return false;
        return true;
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
