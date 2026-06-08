package com.facketmaster.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "facketmaster.pagamentos";
    public static final String QUEUE_PEDIDOS = "pagamentos.pedidos";
    public static final String QUEUE_RESULTADO = "pagamentos.resultado";
    public static final String QUEUE_DLQ = "pagamentos.pedidos.dlq";
    public static final String EXCHANGE_DLQ = "facketmaster.pagamentos.dlx";

    public static final String RK_PEDIDO = "pagamento.pedido.novo";
    public static final String RK_RESULTADO = "pagamento.resultado";

    @Bean
    public TopicExchange exchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_DLQ).durable(true).build();
    }

    @Bean
    public Queue queuePedidos() {
        return QueueBuilder.durable(QUEUE_PEDIDOS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLQ)
                .withArgument("x-dead-letter-routing-key", QUEUE_DLQ)
                .withArgument("x-message-ttl", 86_400_000)
                .build();
    }

    @Bean
    public Queue queueResultado() {
        return QueueBuilder.durable(QUEUE_RESULTADO).build();
    }

    @Bean
    public Queue queueDlq() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    @Bean
    public Binding bindingPedidos(Queue queuePedidos, TopicExchange exchange) {
        return BindingBuilder.bind(queuePedidos).to(exchange).with(RK_PEDIDO);
    }

    @Bean
    public Binding bindingResultado(Queue queueResultado, TopicExchange exchange) {
        return BindingBuilder.bind(queueResultado).to(exchange).with(RK_RESULTADO);
    }

    @Bean
    public Binding bindingDlq(Queue queueDlq, DirectExchange dlxExchange) {
        return BindingBuilder.bind(queueDlq).to(dlxExchange).with(QUEUE_DLQ);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(@NonNull ConnectionFactory cf) {
        RabbitTemplate tpl = new RabbitTemplate(cf);
        tpl.setMessageConverter(jacksonMessageConverter());
        tpl.setMandatory(true);
        return tpl;
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