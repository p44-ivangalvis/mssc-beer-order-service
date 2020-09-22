package guru.sfg.beer.order.service.services.testComponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateBeerOrderResult;
import guru.sfg.brewery.model.events.ValidateBeerOrderRequest;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;


@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationListener
{
    private final JmsTemplate jmsTemplate;

    @JmsListener( destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void list( AllocateBeerOrderResult request ){
        boolean isValid = true;
        boolean sendResponse = true;


        System.out.println("################# JMS ALLOCATION LISTENER RUNNING ##############");
        jmsTemplate.convertAndSend( JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE, AllocateBeerOrderResult.builder()
                                                                                                    .hasAllocationErrors( false )
                                                                                                    .hasPendingInventory( false )
                                                                                                    .beerOrderDto( request.getBeerOrderDto() )
                                                                                                    .build() );

    }
}
