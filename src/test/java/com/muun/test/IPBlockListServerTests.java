package com.muun.test;

import com.muun.IPBlocklistService;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import static io.smallrye.common.constraint.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.quarkus.test.junit.QuarkusTest;

import javax.inject.Inject;


@QuarkusTest()
@TestProfile(IPBlockListTestProfile.class)
public class IPBlockListServerTests {
    @Inject
    IPBlocklistService service;

    private static final long TIMEOUT = 10000L;

    @BeforeEach
    public void setUp() {
        long start = System.currentTimeMillis();
        while (true) {
            if (service.isInitialized()) {
                Log.info("Service initialized");
                break;
            }
            if (System.currentTimeMillis() - start > TIMEOUT) {
                throw new RuntimeException("Timed out waiting for scheduled task to finish");
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void testIsBlockedEmptyCache() throws MalformedURLException, URISyntaxException {
        assertFalse(service.isBlocked("127.0.0.1"));
    }

    @Test
    public void testIsBlocked() throws IOException, URISyntaxException {
        String[] ips = {"127.0.0.1", "127.0.0.2"};
        CharArrayReader reader = new CharArrayReader(String.join(System.lineSeparator(), ips).toCharArray());
        service.updateBlockList(new BufferedReader(reader));

        assertTrue(service.isBlocked("127.0.0.1"));
        assertTrue(service.isBlocked("127.0.0.2"));
        assertFalse(service.isBlocked("192.168.1.1"));
    }

    @Test
    public void testUpdateBlocklist() throws IOException, URISyntaxException {
        String[] ips = {"127.0.0.1"};
        CharArrayReader reader = new CharArrayReader(String.join(System.lineSeparator(), ips).toCharArray());
        service.updateBlockList(new BufferedReader(reader));

        assertTrue(service.isBlocked("127.0.0.1"));
        assertFalse(service.isBlocked("192.168.1.1"));
        String[] newIps = {"192.168.1.1"};
        reader = new CharArrayReader(String.join(System.lineSeparator(), newIps).toCharArray());
        service.updateBlockList(new BufferedReader(reader));
        assertFalse(service.isBlocked("127.0.0.1"));
        assertTrue(service.isBlocked("192.168.1.1"));

    }

    @Test
    public void testInvalidIP() throws IOException, URISyntaxException {
        String[] ips = {"256.0.0.0"};
        CharArrayReader reader = new CharArrayReader(String.join(System.lineSeparator(), ips).toCharArray());
        service.updateBlockList(new BufferedReader(reader));

        assertFalse(service.isBlocked("256.0.0.0"));
    }

    @Test
    public void testDuplicatedIPs() throws IOException, URISyntaxException {
        String[] ips = {"127.0.0.1", "127.0.0.1"};
        CharArrayReader reader = new CharArrayReader(String.join(System.lineSeparator(), ips).toCharArray());
        service.updateBlockList(new BufferedReader(reader));
        assertTrue(service.isBlocked("127.0.0.1"));
    }
}
