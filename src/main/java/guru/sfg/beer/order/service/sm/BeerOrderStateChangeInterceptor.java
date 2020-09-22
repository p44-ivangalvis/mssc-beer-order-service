package guru.sfg.beer.order.service.sm;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.services.BeerOrderManagerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;


@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderStateChangeInterceptor extends StateMachineInterceptorAdapter<BeerOrderStatusEnum, BeerOrderEventEnum>
{

    private final BeerOrderRepository beerOrderRepository;

    @Override
    /**We annotate this with the transactional because this uses the repo, so that we don't get the lazyinitialization exception**/
    @Transactional
    /**This interceptor is built in order to get the change of state and persist it into the DB**/
    public void preStateChange( State<BeerOrderStatusEnum, BeerOrderEventEnum> state, Message<BeerOrderEventEnum> message,
                                Transition<BeerOrderStatusEnum, BeerOrderEventEnum> transition, StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachine) {

        Optional.ofNullable( message ).
              flatMap( msg -> Optional.ofNullable( msg.getHeaders().getOrDefault( BeerOrderManagerImpl.BEER_ORDER_ID_HEADER, " " ).toString() ) )
              .ifPresent( beerOrderId -> {
            /** Here we can have serious problems if the guard is null, hence we need to ensure that it wont be null, maybe using a guard, look at the config class **/
            //Optional.ofNullable( UUID.class.cast( msg.getHeaders().getOrDefault( BeerOrderManagerImpl.BEER_ORDER_ID_HEADER, -1L)))

                    //.ifPresent(beerOrderId -> {
                        //BeerOrder beerOrder = beerOrderRepository.getOne(beerOrderId);
                        BeerOrder beerOrder = beerOrderRepository.getOne(UUID.fromString( beerOrderId ));
                        BeerOrderStatusEnum beerOrderStatus = state.getId();
                        beerOrder.setOrderStatus(beerOrderStatus);
                        log.debug( "saving the state for order id: " + beerOrderId );
                        beerOrderRepository.save(beerOrder);
              });
        //});
    }
}

