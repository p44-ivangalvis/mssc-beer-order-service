package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.AllocateBeerOrderResult;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AllocateOrderResponseListener
{
    private final BeerOrderManager beerOrderManager;

    @JmsListener( destination = JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE)
    public void listen( AllocateBeerOrderResult result){

        UUID beerOrderId = result.getBeerOrderDto().getId();
        beerOrderManager.processAllocationResult( result.getBeerOrderDto(), result.getHasAllocationErrors(), result.getHasPendingInventory() );
    }
}
