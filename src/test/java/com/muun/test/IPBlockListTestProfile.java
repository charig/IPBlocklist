package com.muun.test;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class IPBlockListTestProfile implements QuarkusTestProfile {

    public Map<String, String> getConfigOverrides() {
        return Map.of("config.url", "file:src/test/resources/emptyIPs.txt");
    }

}