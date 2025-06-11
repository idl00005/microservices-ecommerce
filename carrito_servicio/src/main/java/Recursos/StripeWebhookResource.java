package Recursos;

import Entidades.OrdenPago;
import Repositorios.OrdenPagoRepository;
import Servicios.CarritoService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/webhook/stripe")
public class StripeWebhookResource {

    @Inject
    OrdenPagoRepository ordenPagoRepository;

    @ConfigProperty(name = "stripe.webhook.secret")
    String stripeWebhookSecret;

    @Inject
    CarritoService carritoService;

    @POST
    @Transactional
    public Response handleStripeWebhook(String payload, @jakarta.ws.rs.core.Context jakarta.ws.rs.core.HttpHeaders headers) {
        String sigHeader = headers.getHeaderString("Stripe-Signature");
        try {
            // Verifica la firma del webhook
            Event event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
            System.out.println("Payload recibido: " + payload);
            System.out.println("Cabecera de firma: " + sigHeader);
            System.out.println("Tipo de evento recibido: " + event.getType());

            if ("payment_intent.succeeded".equals(event.getType()) ||
                    "payment_intent.payment_failed".equals(event.getType()) ||
                    "payment_intent.canceled".equals(event.getType())) {
                JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
                JsonObject paymentIntentObj = json
                        .getAsJsonObject("data")
                        .getAsJsonObject("object");

                String paymentIntentId = paymentIntentObj.get("id").getAsString();

                OrdenPago orden = ordenPagoRepository.findByReferenciaExterna(paymentIntentId);
                if (orden != null) {
                    switch (event.getType()) {
                        case "payment_intent.succeeded":
                            orden.estado = "PAGADO";
                            carritoService.procesarPagoCompletado(orden.id);
                            break;
                        case "payment_intent.payment_failed":
                            orden.estado = "FALLIDO";
                            break;
                        case "payment_intent.canceled":
                            orden.estado = "CANCELADO";
                            carritoService.procesarPagoCancelado(orden.id);
                            break;
                    }

                    ordenPagoRepository.persist(orden);
                } else {
                    System.err.println("Orden no encontrada para PaymentIntent ID: " + paymentIntentId);
                }
            }

            return Response.ok().build();
        } catch (Exception e) {
            System.err.println("Error al procesar el webhook: " + e.getMessage());
            return Response.status(400).entity("Webhook error: " + e.getMessage()).build();
        }
    }
}
