package com.Inicializacion;

import com.Entidades.Producto;
import com.Repositorios.RepositorioProducto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;

@Startup
@ApplicationScoped
public class ApplicationInitializer {

    @Inject
    RepositorioProducto repositorioProducto;

    @Inject
    ObjectMapper objectMapper; // Jackson para construir detalles JSON

    @PostConstruct
    @Transactional // Indica que el método se ejecuta dentro de una transacción
    public void init() {
        if(repositorioProducto.count() == 0) {
            agregarProducto(new Producto("Smartphone Samsung Galaxy S21",
                    "Teléfono móvil de última generación con cámara de alta resolución.",
                    new BigDecimal("799.99"),
                    100,
                    "Teléfono",
                    createJsonNode("color:Negro", "almacenamiento:128GB", "procesador:Exynos 2100")));
            agregarProducto(new Producto("iPhone 13",
                    "El último iPhone, con un rendimiento increíble y sistema operativo iOS.",
                    new BigDecimal("999.99"),
                    50,
                    "Teléfono",
                    createJsonNode("color:Blanco", "almacenamiento:256GB", "procesador:A15 Bionic")));

            agregarProducto(new Producto("Televisor LG 65OLED",
                    "Televisor OLED 4K con colores vibrantes y negros profundos.",
                    new BigDecimal("1499.99"),
                    30,
                    "Televisor",
                    createJsonNode("pantalla:65 pulgadas", "resolución:4K", "tecnología:OLED")));

            agregarProducto(new Producto("MacBook Pro M2",
                    "Potente laptop con procesador Apple M2 y diseño ultradelgado.",
                    new BigDecimal("1999.99"),
                    20,
                    "Portátil",
                    createJsonNode("almacenamiento:512GB", "procesador:M2", "ram:16GB")));

            agregarProducto(new Producto("Cámara Web Logitech C920",
                    "Una de las cámaras web más populares para streaming y videoconferencia.",
                    new BigDecimal("99.99"),
                    200,
                    "Cámara",
                    createJsonNode("resolución:1080p", "fps:60", "conexión:HDMI")));

            agregarProducto(new Producto("Disco Duro Seagate 1TB",
                    "Almacenamiento confiable para tus documentos y multimedia.",
                    new BigDecimal("59.99"),
                    500,
                    "Disco",
                    createJsonNode("almacenamiento:1TB", "tecnología:HDD", "velocidad:7200RPM")));

            agregarProducto(new Producto("Auriculares Sony WH-1000XM4",
                    "Auriculares inalámbricos con cancelación activa de ruido.",
                    new BigDecimal("349.99"),
                    70,
                    "Auricular",
                    createJsonNode("conectividad:Bluetooth", "duración batería:40 horas")));

            agregarProducto(new Producto("Monitor Dell UltraSharp 27",
                    "Monitor profesional con colores precisos y tecnología IPS.",
                    new BigDecimal("499.99"),
                    40,
                    "Monitor",
                    createJsonNode("tecnología:IPS", "resolución:1440p", "tamaño:27 pulgadas")));

            agregarProducto(new Producto("Power Bank Anker 30000mAh",
                    "Batería portátil de gran capacidad para cargar varios dispositivos.",
                    new BigDecimal("79.99"),
                    150,
                    "Batería",
                    createJsonNode("capacidad:30000mAh", "puertos:USB-C y USB-A", "peso:600g")));

            agregarProducto(new Producto("Laptop MSI Gaming",
                    "Laptop diseñada para gamers con alto rendimiento.",
                    new BigDecimal("1499.99"),
                    25,
                    "Portátil",
                    createJsonNode("procesador:Ryzen 7", "almacenamiento:1TB SSD", "ram:16GB")));

            agregarProducto(new Producto("Router TP-Link Archer AX50",
                    "Router Wi-Fi 6 para una conexión más rápida y estable.",
                    new BigDecimal("129.99"),
                    100,
                    "Router",
                    createJsonNode("velocidad:600 Mbps", "tipo:Wi-Fi 6", "puertos:RJ45")));

            agregarProducto(new Producto("Impresora Láser HP LaserJet Pro",
                    "Impresora láser rápida con alta calidad de impresión.",
                    new BigDecimal("299.99"),
                    40,
                    "Impresora",
                    createJsonNode("velocidad:35 ppm", "resolución:1200 DPI", "tecnología:Láser")));

            agregarProducto(new Producto("Tablet Samsung Galaxy Tab A7",
                    "Tablet económica con excelentes prestaciones multimedia.",
                    new BigDecimal("179.99"),
                    120,
                    "Tablet",
                    createJsonNode("almacenamiento:64GB", "ram:4GB", "procesador:MediaTek Helio P22")));

            agregarProducto(new Producto("Teclado Bluetooth Logitech K380",
                    "Teclado inalámbrico compacto compatible con múltiples dispositivos.",
                    new BigDecimal("49.99"),
                    200,
                    "Teclado",
                    createJsonNode("tamaño:10 pulgadas", "conectividad:Bluetooth", "batería:Recargable")));

            agregarProducto(new Producto("Reloj Inteligente Fitbit Versa 3",
                    "Reloj deportivo con funciones avanzadas de salud y fitness.",
                    new BigDecimal("229.99"),
                    80,
                    "Smartwatch",
                    createJsonNode("sensores:Corazón y Oxígeno", "batería:7 días", "resistencia:5ATM")));

            agregarProducto(new Producto("SSD Samsung 980 Pro 2TB",
                    "Unidad SSD de alto rendimiento para PC y consolas.",
                    new BigDecimal("349.99"),
                    60,
                    "Disco",
                    createJsonNode("almacenamiento:2TB", "tipo:SSD", "interfaz:NVMe")));

            agregarProducto(new Producto("Smartphone Xiaomi Redmi Note 11",
                    "Smartphone económico con excelentes especificaciones.",
                    new BigDecimal("249.99"),
                    150,
                    "Teléfono",
                    createJsonNode("tamaño pantalla:6.5 pulgadas", "batería:5000mAh", "ram:6GB")));

            agregarProducto(new Producto("Cargador Rápido Anker PowerPort",
                    "Cargador rápido compatible con múltiples dispositivos.",
                    new BigDecimal("59.99"),
                    250,
                    "Cargador",
                    createJsonNode("conectividad:USB-C", "potencia:100W", "peso:200g")));

            agregarProducto(new Producto("Proyector Epson Home Cinema",
                    "Proyector LED Full HD ideal para cine en casa.",
                    new BigDecimal("799.99"),
                    30,
                    "Proyector",
                    createJsonNode("resolución:1080p", "fps:120", "tecnología:LED")));

            agregarProducto(new Producto("Monitor Ultrawide LG 34WN80C-B",
                    "Monitor ultrapanorámico de 34 pulgadas ideal para productividad.",
                    new BigDecimal("699.99"),
                    45,
                    "Monitor",
                    createJsonNode("resolución:2K", "tamaño:34 pulgadas", "relación aspecto:21:9")));
            System.out.println("Se han insertado los productos en la base de datos.");
        }
    }

    @Transactional
    protected void agregarProducto(Producto producto) {
        // Persistimos el producto a través del repositorio
        repositorioProducto.persist(producto);
    }

    // Método para construir un ObjectNode con atributos personalizados
    private ObjectNode createJsonNode(String... atributos) {
        ObjectNode detalles = objectMapper.createObjectNode();
        for (String atributo : atributos) {
            // Dividimos el atributo en clave y valor
            String[] partes = atributo.split(":");
            if (partes.length == 2) {
                detalles.put(partes[0].trim(), partes[1].trim());
            }
        }
        return detalles;
    }
}