package Servicios;

import DTO.CarritoItemDTO;
import Entidades.OrdenPago;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class StripeService {

    @Inject
    @ConfigProperty(name = "stripe.secret-key")
    String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public PaymentIntent crearPago(OrdenPago ordenPago) throws StripeException {

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(ordenPago.getMontoTotal().multiply(BigDecimal.valueOf(100)).longValue())  // en céntimos
                .setCurrency("eur")
                .build();

        return PaymentIntent.create(params);
    }
}