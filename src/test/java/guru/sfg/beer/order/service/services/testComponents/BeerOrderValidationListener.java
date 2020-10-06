package guru.sfg.beer.order.service.services.testComponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.ValidateBeerOrderRequest;
import guru.sfg.brewery.model.events.ValidateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;


@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderValidationListener {


    private final JmsTemplate jmsTemplate;

    @JmsListener( destination = JmsConfig.VALIDATE_ORDER_QUEUE)
    public void list( ValidateBeerOrderRequest request){
        boolean isValid = true;
        boolean sendResponse = true;

        //ValidateBeerOrderRequest request = (ValidateBeerOrderRequest) msg.getPayload();
        //ValidateBeerOrderRequest request = (ValidateBeerOrderRequest) msg.getPayload();

        //condition to fail validation
        if (request.getBeerOrderDto().getCustomerRef() != null) {
            if (request.getBeerOrderDto().getCustomerRef().equals("fail-validation")){
                isValid = false;
            } else if (request.getBeerOrderDto().getCustomerRef().equals("dont-validate")){
                sendResponse = false;
            }
        }

        if (sendResponse) {
        System.out.println("################# JMS VALIDATION LISTENER RUNNING ##############");
            jmsTemplate.convertAndSend( JmsConfig.VALIDATE_ORDER_RESPONSE_QUEUE,
                                        ValidateOrderResult.builder()
                                                           .isValid(isValid)
                                                           .orderId(request.getBeerOrderDto().getId())
                                                           .build());
        }
    }
}
