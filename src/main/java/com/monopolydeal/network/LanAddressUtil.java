package com.monopolydeal.network;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Enumerates local IPv4 addresses that other players can use to join.
 */
public final class LanAddressUtil {

    private LanAddressUtil() {
    }

    /**
     * Returns active, non-loopback IPv4 addresses on this machine (deduplicated).
     */
    public static List<String> localIpv4Addresses() {
        Set<String> found = new LinkedHashSet<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }

                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        found.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
        }

        try {
            InetAddress local = InetAddress.getLocalHost();
            if (local instanceof Inet4Address && !local.isLoopbackAddress()) {
                found.add(local.getHostAddress());
            }
        } catch (Exception ignored) {
        }

        List<String> addresses = new ArrayList<>(found);
        Collections.sort(addresses);
        return addresses;
    }

    /**
     * Addresses to show on the host waiting screen (LAN IPs + localhost hint).
     */
    public static List<String> joinAddressesForDisplay() {
        List<String> lines = new ArrayList<>();
        List<String> lan = localIpv4Addresses();
        if (!lan.isEmpty()) {
            lines.addAll(lan);
        }
        lines.add("localhost");
        return lines;
    }

    public static String primaryAddress() {
        List<String> all = localIpv4Addresses();
        return all.isEmpty() ? "localhost" : all.get(0);
    }
}
