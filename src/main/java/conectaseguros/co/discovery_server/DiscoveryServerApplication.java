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
                .load();

        System.setProperty("SERVER_PORT", Objects.requireNonNull(dotenv.get("SERVER_PORT")));
        System.setProperty("EUREKA_INSTANCE_HOSTNAME", Objects.requireNonNull(dotenv.get("EUREKA_INSTANCE_HOSTNAME")));
        System.setProperty("SPRING_PROFILES_ACTIVE", Objects.requireNonNull(dotenv.get("SPRING_PROFILES_ACTIVE")));
        System.setProperty("EUREKA_CLIENT_REGISTER_WITH_EUREKA", Objects.requireNonNull(dotenv.get("EUREKA_CLIENT_REGISTER_WITH_EUREKA")));
        System.setProperty("EUREKA_CLIENT_FETCH_REGISTRY", Objects.requireNonNull(dotenv.get("EUREKA_CLIENT_FETCH_REGISTRY")));
        System.setProperty("EUREKA_SERVER_ENABLE_SELF_PRESERVATION", Objects.requireNonNull(dotenv.get("EUREKA_SERVER_ENABLE_SELF_PRESERVATION")));
        System.setProperty("LOGGING_LEVEL_ROOT", Objects.requireNonNull(dotenv.get("LOGGING_LEVEL_ROOT")));
        System.setProperty("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD", Objects.requireNonNull(dotenv.get("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_CLOUD")));

		SpringApplication.run(DiscoveryServerApplication.class, args);
	}

}
