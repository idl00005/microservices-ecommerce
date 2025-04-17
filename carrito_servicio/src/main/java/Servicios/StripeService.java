package Servicios;

import Entidades.OrdenPago;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class StripeService {

    @Inject
    @ConfigProperty(name = "stripe.secret-key")
    String stripeSecretKey;

    @PostConstruct
    public void init() {
        // Configura la clave secreta de Stripe después de la inyección
        Stripe.apiKey = stripeSecretKey;
    }

    public PaymentIntent crearPago(OrdenPago ordenPago) throws StripeException {
        // Configura los parámetros del PaymentIntent
        Map<String, Object> params = new HashMap<>();
        params.put("amount", ordenPago.montoTotal.multiply(BigDecimal.valueOf(100)).longValue()); // Convertir a centavos
        params.put("currency", "usd");
        params.put("description", "Pago de carrito para el usuario: " + ordenPago.userId);

        // Crear el PaymentIntent
        return PaymentIntent.create(params);
    }
}