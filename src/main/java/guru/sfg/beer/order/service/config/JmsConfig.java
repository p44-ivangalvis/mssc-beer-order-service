package guru.sfg.beer.order.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
//import org.springframework.jms.support.converter.


@Configuration
public class JmsConfig
{
    @Bean // Serialize message content to json using TextMessage
    //if we dont specify an object mapper to work with, this jackson converter will never use it, so it will fail to convert objects or deserialize them
    //this was the error Cannot construct instance of `java.time.OffsetDateTime` (no Creators, like default construct, exist): cannot deserialize from Object value (no delegate- or property-based Creator)
    public MessageConverter jacksonJmsMessageConverter( ObjectMapper objectMapper) {
    //public MessageConverter jacksonJmsMessageConverter( ) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType( MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setObjectMapper(objectMapper);
        return converter;
    }
}
