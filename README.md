# IP Blocklist Service

This is a simple REST API service built with Quarkus that checks if an IP address is in a blocklist.

## Features
* Developed using [Quarkus](https://quarkus.io/) 
* Checks if an IP address is blocked using a blocklist retrieved from a URL
* Supports only IPv4
* Uses [Caffeine cache](https://github.com/ben-manes/caffeine) to improve performance and reduce the number of requests to the blocklist URL
* Updates the blocklist automatically every 24 hours

## Requirements
* Java 11 or higher
* Docker (optional)

## Getting Start
* Clone the repository
```
git clone https://github.com/charig/IPBlocklist
```

* Build the application: 
```
./mvnw package
```

* Test the application: 
```
./mvnw test
```

* Run the application: 
```
java -jar target/quarkus-app/quarkus-run.jar
```

* Test the API:
```
curl http://localhost:8080/ips/192.168.1.1
```
The API should return true or false depending on whether the IP address is in the blocklist.

* Build a native image (linux executable):
```
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

## Docker
You can also run the application in a Docker container. To build the container, run:

* Build a docker container with the native image:
```
docker build -f src/main/docker/Dockerfile.native -t ip-blocklist .
```

* Run the container locally
```
docker run -it -p{localhostPort}:{containerPort} ip-blocklist
```

## Configurations
Configurations are taken from the `application.properties` file under `src/main/resources`:
* `config.url`: the URL of the blocklist file (required)
* `quarkus.http.port`: the port to listen on (default 8080)
* `quarkus.cache.caffeine.size`: the maximum number of entries in the cache (default 10_000)
* `quarkus.cache.caffeine.expire-after-write`: the duration after which entries in the cache expire (default 24h)

To change a configuration, the project must be rebuilt
