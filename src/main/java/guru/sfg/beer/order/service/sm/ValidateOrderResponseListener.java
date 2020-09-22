package guru.sfg.beer.order.service.sm;


import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.services.BeerOrderManager;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class ValidateOrderResponseListener
{
    private final BeerOrderManager beerOrderManager;

    @JmsListener( destination = JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE)
    public void listen( ValidateOrderResult result){

        UUID beerOrderId = result.getOrderId();
        beerOrderManager.processValidationResult( beerOrderId, result.getIsValid() );
    }

}
