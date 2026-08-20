package com.ntnh.herald.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;

import org.junit.jupiter.api.Test;

class IpAddressDisplayTest {

    @Test
    void ipv4MappedIpv6DisplaysAsDottedIpv4() throws Exception {
        byte[] mapped = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xff, (byte) 192, 0, 2, 10 };

        assertEquals(
            "192.0.2.10",
            IpAddress.from(InetAddress.getByAddress(mapped))
                .getDisplayText());
    }

    @Test
    void nativeIpv6KeepsIpv6Presentation() throws Exception {
        IpAddress address = IpAddress.from(InetAddress.getByName("2001:db8::10"));

        assertEquals(address.getText(), address.getDisplayText());
    }

    @Test
    void ipv4KeepsDottedPresentation() throws Exception {
        IpAddress address = IpAddress.from(InetAddress.getByName("192.0.2.11"));

        assertEquals("192.0.2.11", address.getDisplayText());
    }
}
