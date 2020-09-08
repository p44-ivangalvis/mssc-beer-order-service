package guru.sfg.beer.order.service.services.beer;

import guru.sfg.beer.order.service.web.model.BeerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@ConfigurationProperties( prefix = "sfg.brewery", ignoreUnknownFields = false)
@Service
public class BeerServiceImpl implements BeerService {
    public final String BEER_PATH_V1 = "/api/v1/beer/";
    public final String BEER_UPC_PATH_V1 = "/api/v1/beer/beerUpc/";
    private final RestTemplate restTemplate;

    private String beerServiceHost;

    public BeerServiceImpl( RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public Optional<BeerDto> getBeerById( UUID uuid){
        log.debug( "getting by id: " + uuid );
        return Optional.of(restTemplate.getForObject(beerServiceHost + BEER_PATH_V1 + uuid.toString(), BeerDto.class));
    }

    @Override
    public Optional<BeerDto> getBeerByUpc(String upc) {
        log.debug( "getting by upc: " + upc );
//        error of the beer grasp fixed by changing the BEER_UPC_PATH_V1 from /api/v1/beerUpc/ to /api/v1/beer/beerUpc/
        return Optional.of(restTemplate.getForObject(beerServiceHost + BEER_UPC_PATH_V1 + upc, BeerDto.class));
    }

    public void setBeerServiceHost(String beerServiceHost) {
        this.beerServiceHost = beerServiceHost;
    }
}
