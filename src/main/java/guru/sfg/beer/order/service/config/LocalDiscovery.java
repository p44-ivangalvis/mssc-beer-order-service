package guru.sfg.beer.order.service.config;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


//This is the class that is created in order to enable local discovery of the services by using eureka
//If we want to use eureka then we have to configure the profile in the run configs to include local-discovery profile
@Profile("local-discovery")
@EnableDiscoveryClient
@Configuration
public class LocalDiscovery
{
}
