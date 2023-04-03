package com.muun;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;

import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import io.quarkus.runtime.annotations.CommandLineArguments;
import io.quarkus.scheduler.Scheduled;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.quarkus.logging.Log;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/ips")
@ApplicationScoped
public class IPBlocklistService {

    private final Cache<String, Boolean> cache;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    URL url;
    Boolean init = false;

    public Boolean isInitialized() {
        return init;
    }

    public IPBlocklistService(@ConfigProperty(name = "config.url") String arg) throws URISyntaxException, MalformedURLException {
        Log.info("Starting IP blocklist service...");
        Log.info(arg);
        url = new URI(arg).toURL();
        cache = Caffeine.newBuilder()
                .build();

    }

    @GET
    @Path("/{ip}")
    public boolean isBlocked(@PathParam("ip") String ip) {
        return isInBlocklist(ip);
    }

    public boolean isInBlocklist(String ip) {
        return cache.getIfPresent(ip) != null;
    }

    public void updateBlockList(BufferedReader input) throws IOException {
        HashMap<String, Boolean> newValues = new HashMap<>();
        Set<String> oldBlockedIps = new HashSet<>(cache.asMap().keySet());

        String line;
        while ((line = input.readLine()) != null) {
            String[] lineFields = line.split("\\s+");
            // Expecting lines with this format: 103.233.155.93 1 (ip, #blocklist)
            if (!lineFields[0].startsWith("#")) {
                try {
                    InetAddress address = InetAddress.getByName(lineFields[0]);
                    if (!oldBlockedIps.remove(address.getHostAddress())) {
                        newValues.put(address.getHostAddress(), true);
                    }
                } catch (java.net.UnknownHostException e) {
                    Log.info("Invalid ip: " + lineFields[0]);
                }
            }
        }

        // Invalidate IPs that are no longer blocked
        for (String ip : oldBlockedIps) {
            cache.invalidate(ip);
        }

        // Add new blocked IPs
        cache.putAll(newValues);
        Log.info(newValues.size());

        Log.info("Blocklist updated with " + newValues.size() + " entries");
        init = true;
    }

    @Scheduled(every = "24h")
    public void updateBlocklistTask() {
        Log.info("Updating blocklist...");
        long retryDelayMiliSeconds = 1000;
        final Runnable[] updateTask = {null};
        updateTask[0] = () -> {
            boolean success = false;
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream()));
                this.updateBlockList(reader);
                success = true;
            } catch (IOException e) {
                Log.info(e.getMessage());
                Log.info("Updating blocklist failed, retrying in " + retryDelayMiliSeconds + " miliseconds...");
            } catch (Exception e) {
                Log.info(e.getMessage());
            } finally {
                if (!success) {
                    Log.info("Fallo, que cosa??");
                    executorService.schedule(updateTask[0], retryDelayMiliSeconds, TimeUnit.MILLISECONDS);
                }
            }
        };
        executorService.schedule(updateTask[0], 0, TimeUnit.MILLISECONDS);
    }
}
