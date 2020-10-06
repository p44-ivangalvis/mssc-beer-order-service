package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.sm.BeerOrderStateChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@RequiredArgsConstructor
@Service
public class BeerOrderManagerImpl
      implements BeerOrderManager
{

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;

    public static final String BEER_ORDER_ID_HEADER = "beer_order_id";

    private final BeerOrderRepository beerOrderRepository;

    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;

    //private final EntityManager entityManager;



    @Transactional
    @Override
    public BeerOrder newBeerOrder( final BeerOrder beerOrder )
    {

        beerOrder.setId( null );
        beerOrder.setOrderStatus( BeerOrderStatusEnum.NEW );

        BeerOrder savedBeerOrder = beerOrderRepository.save( beerOrder );
        BeerOrder requestedBeerOrder = beerOrderRepository.findById( beerOrder.getId() ).get();
        sendBeerOrderEvent( savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER );
        return savedBeerOrder;
    }

    @Transactional
    @Override
    public void processValidationResult( UUID beerOrderId,
                                         Boolean isValid )
    {
        log.debug("Process Validation Result for beerOrderId: " + beerOrderId + " Valid? " + isValid);
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById( beerOrderId );

        /**The following line was performing a forced flush so that DB takes the most updated status,
         * due that all this logic changes very quick, and is using different threads**/
        //entityManager.flush();

        beerOrderOptional.ifPresentOrElse( beerOrder ->{

            //beerOrder = (BeerOrder) beerOrder;
            if ( isValid )
            {
                sendBeerOrderEvent( beerOrder, BeerOrderEventEnum.VALIDATION_PASSED );

                //wait for status change
                /**This is done due that all the evenst happen so quick, then DB does not save it with the correct ststus
                 *  so as we were receiving an error here,
                 * we need to create a custom method that helps us await until the status in the order gets as the SM has**/
                awaitForStatus(beerOrderId, BeerOrderStatusEnum.VALIDATED);
                /**Once the interceptor intercepts that the order changes from state Validation_pending to Validation_passed it persists to the database
                 so this new query to the repository is practically, a new one, a very new different object than what was there before,
                 so we can allocate that one as well**/
                BeerOrder validatedOrder = beerOrderRepository.findById(beerOrderId).get();

                sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATE_ORDER);

            }
            else
            {
                sendBeerOrderEvent( beerOrder, BeerOrderEventEnum.VALIDATION_FAILED );
            }
        }, ()-> log.error( "Order not found in validation. ID: " + beerOrderId ) );

    }


    @Override
    public void processAllocationResult( final BeerOrderDto beerOrderDto,
                                         final Boolean hasAllocationErrors,
                                         final Boolean hasPendingInventory )
    {

        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById( beerOrderDto.getId() );

        beerOrderOptional.ifPresentOrElse( beerOrder -> {

            if(hasAllocationErrors){

                sendBeerOrderEvent( beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED );

            }else if(!hasAllocationErrors && hasPendingInventory){

                sendBeerOrderEvent( beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY );

                awaitForStatus(beerOrder.getId(), BeerOrderStatusEnum.PENDING_INVENTORY);

                updateAllocatedQty( beerOrderDto );

            }else{

                sendBeerOrderEvent( beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS );

                awaitForStatus(beerOrder.getId(), BeerOrderStatusEnum.ALLOCATED);

                updateAllocatedQty( beerOrderDto );

            }

        },() -> log.error( "Order not found in allocation. ID: " + beerOrderDto.getId() )  );
    }


    @Override
    public void processPickUpOrder( final UUID beerOrderId )
    {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById( beerOrderId );

        beerOrderOptional.ifPresentOrElse( beerOrder -> {

            sendBeerOrderEvent( beerOrder, BeerOrderEventEnum.BEERORDER_PICKED_UP );

        }, () -> log.error( "Order not found in pickup. ID: " + beerOrderId ));
    }


    @Override
    public void processCancelOrder( final UUID beerOrderId )
    {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById( beerOrderId );

        beerOrderOptional.ifPresentOrElse( beerOrder -> {

            sendBeerOrderEvent( beerOrder, BeerOrderEventEnum.CANCEL_ORDER );

        }, () -> log.error( "Order not found to cancel. ID: " + beerOrderId ));

    }


    @Transactional
    public void updateAllocatedQty( BeerOrderDto beerOrderDto )
    {
        Optional<BeerOrder> beerOrderOptional = beerOrderRepository.findById( beerOrderDto.getId() );
        beerOrderOptional.ifPresentOrElse( allocatedOrder ->{

            allocatedOrder.getBeerOrderLines().forEach( beerOrderLine -> {
                beerOrderDto.getBeerOrderLines().forEach( beerOrderLineDto -> {
                    if ( beerOrderLine.getId().equals( beerOrderLineDto.getId() ) )
                    {
                        beerOrderLine.setQuantityAllocated( beerOrderLineDto.getQuantityAllocated() );
                    }
                } );
            } );
        beerOrderRepository.saveAndFlush( allocatedOrder );
        }, () -> log.error( "Order not found. ID: " + beerOrderDto.getId() ));

    }

    private void sendBeerOrderEvent( BeerOrder beerOrder,
                                     BeerOrderEventEnum eventEnum )
    {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = build( beerOrder );

        Message msg = MessageBuilder.withPayload( eventEnum )
                                    .setHeader( BEER_ORDER_ID_HEADER, beerOrder.getId() )
                                    .build();

        sm.sendEvent( msg );
    }

    private void awaitForStatus(UUID beerOrderId, BeerOrderStatusEnum statusEnum) {

        AtomicBoolean found = new AtomicBoolean( false);
        AtomicInteger loopCount = new AtomicInteger( 0);

        while (!found.get()) {
            if (loopCount.incrementAndGet() > 10) {
                found.set(true);
                log.debug("Loop Retries exceeded");
            }

            beerOrderRepository.findById(beerOrderId).ifPresentOrElse(beerOrder -> {
                if (beerOrder.getOrderStatus().equals(statusEnum)) {
                    found.set(true);
                    log.debug("Order Found");
                } else {
                    log.debug("Order Status Not Equal. Expected: " + statusEnum.name() + " Found: " + beerOrder.getOrderStatus().name());
                }
            }, () -> {
                log.debug("Order Id Not Found");
            });

            if (!found.get()) {
                try {
                    log.debug("Sleeping for retry");
                    Thread.sleep(100);
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }


    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build( BeerOrder beerOrder )
    {
        StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> sm = stateMachineFactory.getStateMachine( beerOrder.getId() );

        sm.stop();

        sm.getStateMachineAccessor()
          .doWithAllRegions( sma -> {
              var stateChangeInterceptor = beerOrderStateChangeInterceptor;
              sma.addStateMachineInterceptor( stateChangeInterceptor );
              sma.resetStateMachine( new DefaultStateMachineContext<>( beerOrder.getOrderStatus(), null, null, null ) );
          } );

        sm.start();

        return sm;
    }
}
