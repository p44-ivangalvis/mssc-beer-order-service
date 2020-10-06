package guru.sfg.beer.order.service.services.testComponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateBeerOrderRequest;
import guru.sfg.brewery.model.events.AllocateBeerOrderResult;
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
    public void listen( AllocateBeerOrderRequest request ){
        boolean sendResponse = true;
        boolean hasAllocationErrors = false;
        boolean hasPendingInventory = false;

        //condition to fail validation
        if (request.getBeerOrderDto().getCustomerRef() != null) {
            if (request.getBeerOrderDto().getCustomerRef().equals("has-errors")){
                hasAllocationErrors = true;
            } else if (request.getBeerOrderDto().getCustomerRef().equals("pending-inventory")){
                hasPendingInventory = true;
            } else if (request.getBeerOrderDto().getCustomerRef().equals("dont-allocate")){
                sendResponse = false;
            }
        }

        boolean finalPendingInventory = hasPendingInventory;

        request.getBeerOrderDto().getBeerOrderLines().forEach(beerOrderLineDto -> {
            if (finalPendingInventory) {
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity() - 1);
            } else {
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
            }
        });

        if (sendResponse)
        {
            System.out.println( "################# JMS ALLOCATION LISTENER RUNNING ##############" );
            jmsTemplate.convertAndSend( JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE, AllocateBeerOrderResult.builder()
                                                                                                        .hasAllocationErrors( hasAllocationErrors )
                                                                                                        .hasPendingInventory( hasPendingInventory )
                                                                                                        .beerOrderDto( request.getBeerOrderDto() )
                                                                                                        .build() );
        }
    }
}
