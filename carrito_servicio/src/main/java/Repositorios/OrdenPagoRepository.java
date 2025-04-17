package Repositorios;

import Entidades.OrdenPago;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrdenPagoRepository implements PanacheRepository<OrdenPago> {

    public OrdenPago findByReferenciaExterna(String referenciaExterna) {
        return find("referenciaExterna", referenciaExterna).firstResult();
    }
}