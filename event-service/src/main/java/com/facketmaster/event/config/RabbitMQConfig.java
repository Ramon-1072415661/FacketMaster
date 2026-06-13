package com.facketmaster.event.config;

import com.facketmaster.event.messaging.ResultadoPagamentoMessage;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuração RabbitMQ do event-service.
 * <p>
 * Declara a mesma topologia de exchange/queue usada pelo payment-service
 * para a fila de resultados de pagamento (declaração idempotente — RabbitMQ
 * não duplica recursos já existentes, desde que os argumentos sejam iguais).
 * <p>
 * Também configura o conversor Jackson para mapear o tipo
 * {@code com.facketmaster.payment.messaging.ResultadoPagamentoMessage}
 * (enviado pelo payment-service no header {@code __TypeId__}) para a classe
 * local equivalente {@link ResultadoPagamentoMessage}.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "facketmaster.pagamentos";
    public static final String EXCHANGE_DLQ = "facketmaster.pagamentos.dlx";

    public static final String QUEUE_RESULTADO = "pagamentos.resultado";
    public static final String QUEUE_RESULTADO_DLQ = "pagamentos.resultado.dlq";

    public static final String RK_RESULTADO = "pagamento.resultado";

    private static final String REMOTE_RESULTADO_TYPE =
            "com.facketmaster.payment.messaging.ResultadoPagamentoMessage";

    @Bean
    public TopicExchange exchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_DLQ).durable(true).build();
    }

    @Bean
    public Queue queueResultado() {
        return QueueBuilder.durable(QUEUE_RESULTADO)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLQ)
                .withArgument("x-dead-letter-routing-key", QUEUE_RESULTADO_DLQ)
                .build();
    }

    @Bean
    public Queue queueResultadoDlq() {
        return QueueBuilder.durable(QUEUE_RESULTADO_DLQ).build();
    }

    @Bean
    public Binding bindingResultado(Queue queueResultado, TopicExchange exchange) {
        return BindingBuilder.bind(queueResultado).to(exchange).with(RK_RESULTADO);
    }

    @Bean
    public Binding bindingResultadoDlq(Queue queueResultadoDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(queueResultadoDlq).to(dlxExchange).with(QUEUE_RESULTADO_DLQ);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");
        typeMapper.setIdClassMapping(Map.of(
                REMOTE_RESULTADO_TYPE, ResultadoPagamentoMessage.class
        ));
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory cf) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(jacksonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(5);
        return factory;
    }
}
