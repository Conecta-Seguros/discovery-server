package conectaseguros.co.discovery_server;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.Objects;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

	static void main(String[] args) {

        Dotenv dotenv = Dotenv.configure()
                .directory("./discovery-server")
                .ignoreIfMissing()
                .load();

        System.setProperty("EUREKA_PORT", Objects.requireNonNull(dotenv.get("EUREKA_PORT")));

        System.setProperty("EUREKA_PREFER_IP_ADDRESS", Objects.requireNonNull(dotenv.get("EUREKA_PREFER_IP_ADDRESS")));

        System.setProperty("EUREKA_HOSTNAME_PEER1", Objects.requireNonNull(dotenv.get("EUREKA_HOSTNAME_PEER1")));
        System.setProperty("EUREKA_HOSTNAME_PEER2", Objects.requireNonNull(dotenv.get("EUREKA_HOSTNAME_PEER2")));

        System.setProperty("EUREKA_SELF_PRESERVATION", Objects.requireNonNull(dotenv.get("EUREKA_SELF_PRESERVATION")));
        System.setProperty("EUREKA_RENEWAL_THRESHOLD", Objects.requireNonNull(dotenv.get("EUREKA_RENEWAL_THRESHOLD")));
        System.setProperty("EUREKA_EVICTION_INTERVAL", Objects.requireNonNull(dotenv.get("EUREKA_EVICTION_INTERVAL")));

		SpringApplication.run(DiscoveryServerApplication.class, args);
	}

}
