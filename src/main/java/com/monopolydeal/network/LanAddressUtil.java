package com.monopolydeal.network;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Enumerates local IPv4 addresses that other players can use to join.
 * Skips loopback and virtual adapters — only real physical interfaces.
 * Modelled after ENG-19-main's LanAddressUtil.
 */
public final class LanAddressUtil {

    private LanAddressUtil() {}

    /**
     * Returns all active, non-loopback IPv4 addresses on this machine.
     * Sorted alphabetically for consistent display order.
     */
    public static List<String> localIpv4Addresses() {
        List<String> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;

                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        addresses.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {}

        Collections.sort(addresses);
        return addresses;
    }

    /**
     * Returns a single best-guess local IP (first from the list, or "localhost" fallback).
     */
    public static String primaryAddress() {
        List<String> all = localIpv4Addresses();
        return all.isEmpty() ? "localhost" : all.get(0);
    }
}
