package com.Inicializacion;

import com.Entidades.Producto;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;

@Startup
@ApplicationScoped
public class ApplicationInitializer {

    @Inject
    EntityManager entityManager; // Inyección para interactuar con la base de datos

    @PostConstruct
    @Transactional // Indica que el método se ejecuta dentro de una transacción
    public void init() {
        // Lista de productos reales de electrónica con el constructor de Producto
        persistirProducto(new Producto(
                "Smartphone Samsung Galaxy S21",
                "Teléfono móvil de última generación con cámara de alta resolución.",
                BigDecimal.valueOf(799.99),
                50,
                "{\"color\": \"Negro\", \"almacenamiento\": \"128GB\", \"procesador\": \"Exynos 2100\"}"
        ));

        persistirProducto(new Producto(
                "Laptop Dell XPS 13",
                "Ultrabook de alto rendimiento con pantalla InfinityEdge.",
                BigDecimal.valueOf(1199.99),
                30,
                "{\"procesador\": \"Intel Core i7\", \"RAM\": \"16GB\", \"almacenamiento\": \"512GB SSD\"}"
        ));

        persistirProducto(new Producto(
                "Smart TV LG OLED55",
                "Televisor OLED de 55 pulgadas con resolución 4K.",
                BigDecimal.valueOf(1399.99),
                20,
                "{\"pantalla\": \"55 pulgadas\", \"resolucion\": \"4K\", \"sistema\": \"WebOS\"}"
        ));

        persistirProducto(new Producto(
                "Auriculares Sony WH-1000XM4",
                "Auriculares inalámbricos con cancelación de ruido de alta calidad.",
                BigDecimal.valueOf(349.99),
                100,
                "{\"color\": \"Plateado\", \"autonomia\": \"30 horas\", \"conectividad\": \"Bluetooth 5.0\"}"
        ));

        persistirProducto(new Producto(
                "Cámara Canon EOS R5",
                "Cámara profesional de fotograma completo con capacidades de video 8K.",
                BigDecimal.valueOf(3899.99),
                10,
                "{\"resolucion\": \"45MP\", \"video\": \"8K UHD\", \"almacenamiento\": \"CFExpress\"}"
        ));

        persistirProducto(new Producto(
                "Reloj Inteligente Apple Watch Series 7",
                "Último modelo del Apple Watch con características avanzadas de salud y fitness.",
                BigDecimal.valueOf(429.99),
                70,
                "{\"tamaño\": \"45mm\", \"color\": \"Rojo\", \"sistema\": \"watchOS\"}"
        ));

        persistirProducto(new Producto(
                "Disco Duro Externo Seagate 2TB",
                "Almacenamiento externo de alta capacidad para copias de seguridad.",
                BigDecimal.valueOf(89.99),
                150,
                "{\"capacidad\": \"2TB\", \"conexion\": \"USB 3.0\", \"color\": \"Negro\"}"
        ));

        persistirProducto(new Producto(
                "Teclado Mecánico Razer BlackWidow",
                "Teclado mecánico para gaming con iluminación RGB.",
                BigDecimal.valueOf(129.99),
                60,
                "{\"switches\": \"Razer Green\", \"conectividad\": \"Cable USB\", \"retroiluminacion\": \"RGB\"}"
        ));

        persistirProducto(new Producto(
                "Mouse Logitech MX Master 3",
                "Ratón inalámbrico ergonómico de alta precisión.",
                BigDecimal.valueOf(99.99),
                120,
                "{\"dpi\": \"4000\", \"conexiones\": \"Bluetooth, USB\", \"autonomia\": \"70 días\"}"
        ));

        persistirProducto(new Producto(
                "Consola Sony PlayStation 5",
                "Consola de última generación con capacidades avanzadas de gráficos y rendimiento.",
                BigDecimal.valueOf(499.99),
                15,
                "{\"almacenamiento\": \"825GB SSD\", \"resolucion\": \"4K HDR\", \"color\": \"Blanco\"}"
        ));

        System.out.println("Se han insertado productos reales de electrónica en la base de datos utilizando el constructor.");
    }

    // Método auxiliar para persistir un Producto
    private void persistirProducto(Producto producto) {
        entityManager.persist(producto);
    }
}