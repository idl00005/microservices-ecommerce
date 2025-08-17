package com.Inicializacion;

import com.Entidades.Producto;
import com.Repositorios.RepositorioProducto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;

@Startup
@ApplicationScoped
@IfBuildProfile("init")
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
                    10000,
                    "Teléfono",
                    "https://www.backmarket.es/cdn-cgi/image/format%3Dauto%2Cquality%3D75%2Cwidth%3D640/https://d2e6ccujb3mkqf.cloudfront.net/2b2998f7-a23f-43b8-8421-55a77fe4fadd-1_23c37a99-4d57-402a-b3e3-615928c9827f.jpg",
                    createJsonNode("color:Negro", "almacenamiento:128GB", "procesador:Exynos 2100")));
            agregarProducto(new Producto("iPhone 13",
                    "El último iPhone, con un rendimiento increíble y sistema operativo iOS.",
                    new BigDecimal("999.99"),
                    10000,
                    "Teléfono",
                    "https://www.backmarket.es/cdn-cgi/image/format%3Dauto%2Cquality%3D75%2Cwidth%3D640/https://d2e6ccujb3mkqf.cloudfront.net/943afa80-1922-43de-a7d9-8c1b4294a727-1_57539676-9a3c-465d-85d8-5a8f811d04dc.jpg",
                    createJsonNode("color:Blanco", "almacenamiento:256GB", "procesador:A15 Bionic")));

            agregarProducto(new Producto("Televisor LG 65OLED",
                    "Televisor OLED 4K con colores vibrantes y negros profundos.",
                    new BigDecimal("1499.99"),
                    10000,
                    "Televisor",
                    "https://www.lg.com/content/dam/channel/wcms/es/productos/he/2024/tv/oled/oled65g45lw/lgcom/gallery/01_lg_OLED65G45LW_gallery_main_2010x1334.jpg/_jcr_content/renditions/thum-1600x1062.jpeg",
                    createJsonNode("pantalla:65 pulgadas", "resolución:4K", "tecnología:OLED")));

            agregarProducto(new Producto("MacBook Pro M2",
                    "Potente laptop con procesador Apple M2 y diseño ultradelgado.",
                    new BigDecimal("1999.99"),
                    10000,
                    "Portátil",
                    "https://www.backmarket.es/cdn-cgi/image/format%3Dauto%2Cquality%3D75%2Cwidth%3D1920/https://d2e6ccujb3mkqf.cloudfront.net/fb894509-64b5-4a69-8618-7fced1288813-1_740e7163-ef0a-4924-90ae-7891264edd61.jpg",
                    createJsonNode("almacenamiento:512GB", "procesador:M2", "ram:16GB")));

            agregarProducto(new Producto("Cámara Web Logitech C920",
                    "Una de las cámaras web más populares para streaming y videoconferencia.",
                    new BigDecimal("99.99"),
                    10000,
                    "Cámara",
                    "https://www.bhphotovideo.com/images/images2500x2500/Logitech_960_000764_C920_832460.jpg",
                    createJsonNode("resolución:1080p", "fps:60", "conexión:HDMI")));

            agregarProducto(new Producto("Disco Duro Seagate 1TB",
                    "Almacenamiento confiable para tus documentos y multimedia.",
                    new BigDecimal("59.99"),
                    10000,
                    "Disco",
                    "https://img.pccomponentes.com/articles/32/327718/1464-seagate-expansion-card-para-xbox-series-x-s-1tb-ssd.jpg",
                    createJsonNode("almacenamiento:1TB", "tecnología:HDD", "velocidad:7200RPM")));

            agregarProducto(new Producto("Auriculares Sony WH-1000XM4",
                    "Auriculares inalámbricos con cancelación activa de ruido.",
                    new BigDecimal("349.99"),
                    10000,
                    "Auricular",
                    "https://img.pccomponentes.com/articles/58/587483/1799-sony-wh-1000xm4-auriculares-bluetooth-azules.jpg",
                    createJsonNode("conectividad:Bluetooth", "duración batería:40 horas")));

            agregarProducto(new Producto("Monitor Dell UltraSharp 27",
                    "Monitor profesional con colores precisos y tecnología IPS.",
                    new BigDecimal("499.99"),
                    10000,
                    "Monitor",
                    "https://img.pccomponentes.com/articles/1084/10843895/1435-dell-ultrasharp-u2724de-27-led-ips-qhd-120hz-usb-c.jpg",
                    createJsonNode("tecnología:IPS", "resolución:1440p", "tamaño:27 pulgadas")));

            agregarProducto(new Producto("Power Bank Anker 30000mAh",
                    "Batería portátil de gran capacidad para cargar varios dispositivos.",
                    new BigDecimal("79.99"),
                    10000,
                    "Batería",
                    "https://img.pccomponentes.com/articles/1085/10859414/161-anker-powerbank-20000-mah-30w-negra.jpg",
                    createJsonNode("capacidad:30000mAh", "puertos:USB-C y USB-A", "peso:600g")));

            agregarProducto(new Producto("Laptop MSI Gaming",
                    "Laptop diseñada para gamers con alto rendimiento.",
                    new BigDecimal("1499.99"),
                    10000,
                    "Portátil",
                    "https://cdn.idealo.com/folder/Product/204253/4/204253456/s11_produktbild_max/msi-thin-gf63-12uc-689xes.jpg",
                    createJsonNode("procesador:Ryzen 7", "almacenamiento:1TB SSD", "ram:16GB")));

            agregarProducto(new Producto("Router TP-Link Archer AX50",
                    "Router Wi-Fi 6 para una conexión más rápida y estable.",
                    new BigDecimal("129.99"),
                    10000,
                    "Router",
                    "https://img.pccomponentes.com/articles/1084/10846853/1462-tp-link-archer-ax55-router-doble-banda-wifi-6.jpg",
                    createJsonNode("velocidad:600 Mbps", "tipo:Wi-Fi 6", "puertos:RJ45")));

            agregarProducto(new Producto("Impresora Láser HP LaserJet Pro",
                    "Impresora láser rápida con alta calidad de impresión.",
                    new BigDecimal("299.99"),
                    10000,
                    "Impresora",
                    "https://img.pccomponentes.com/articles/22/221182/69574298-2262671459.jpg",
                    createJsonNode("velocidad:35 ppm", "resolución:1200 DPI", "tecnología:Láser")));

            agregarProducto(new Producto("Tablet Samsung Galaxy Tab A7",
                    "Tablet económica con excelentes prestaciones multimedia.",
                    new BigDecimal("179.99"),
                    10000,
                    "Tablet",
                    "https://www.backmarket.es/cdn-cgi/image/format%3Dauto%2Cquality%3D75%2Cwidth%3D1920/https://d2e6ccujb3mkqf.cloudfront.net/906a614c-33db-466b-9c24-4f5d2ab067f2-1_0b01e6b9-7b34-483a-bc7f-ed411435f60e.jpg",
                    createJsonNode("almacenamiento:64GB", "ram:4GB", "procesador:MediaTek Helio P22")));

            agregarProducto(new Producto("Teclado Bluetooth Logitech K380",
                    "Teclado inalámbrico compacto compatible con múltiples dispositivos.",
                    new BigDecimal("49.99"),
                    10000,
                    "Teclado",
                    "https://http2.mlstatic.com/D_NQ_NP_825139-MLB46956315830_082021-F.jpg",
                    createJsonNode("tamaño:10 pulgadas", "conectividad:Bluetooth", "batería:Recargable")));

            agregarProducto(new Producto("Reloj Inteligente Fitbit Versa 3",
                    "Reloj deportivo con funciones avanzadas de salud y fitness.",
                    new BigDecimal("229.99"),
                    10000,
                    "Smartwatch",
                    "https://img.pccomponentes.com/articles/1060/10603725/1604-fitbit-versa-4-smartwatch-azul-oceano-platino.jpg",
                    createJsonNode("sensores:Corazón y Oxígeno", "batería:7 días", "resistencia:5ATM")));

            agregarProducto(new Producto("SSD Samsung 980 Pro 2TB",
                    "Unidad SSD de alto rendimiento para PC y consolas.",
                    new BigDecimal("349.99"),
                    10000,
                    "Disco",
                    "https://img.pccomponentes.com/articles/1064/10648534/1952-samsung-990-pro-2tb-ssd-pcie-40-nvme-m2.jpg",
                    createJsonNode("almacenamiento:2TB", "tipo:SSD", "interfaz:NVMe")));

            agregarProducto(new Producto("Smartphone Xiaomi Redmi Note 11",
                    "Smartphone económico con excelentes especificaciones.",
                    new BigDecimal("249.99"),
                    10000,
                    "Teléfono",
                    "https://img.pccomponentes.com/articles/1002/10026499/1742-xiaomi-redmi-note-11-4-128gb-azul-ocaso-libre.jpg",
                    createJsonNode("tamaño pantalla:6.5 pulgadas", "batería:5000mAh", "ram:6GB")));

            agregarProducto(new Producto("Cargador Rápido Anker PowerPort",
                    "Cargador rápido compatible con múltiples dispositivos.",
                    new BigDecimal("59.99"),
                    10000,
                    "Cargador",
                    "https://img.pccomponentes.com/articles/1085/10859427/1327-wall-charger-45w-1c-black-eu-plug.jpg",
                    createJsonNode("conectividad:USB-C", "potencia:100W", "peso:200g")));

            agregarProducto(new Producto("Proyector Epson Home Cinema",
                    "Proyector LED Full HD ideal para cine en casa.",
                    new BigDecimal("799.99"),
                    10000,
                    "Proyector",
                    "https://images.visunextgroup.com/images/D/original/1/1000025026/es/epson/Epson-CO-FH02-Proyector-Full-HD-con-tecnologia-3LCD-3-000-lumenes-y-Android-TV-para-cine-en-casa-y-la-oficina.webp",
                    createJsonNode("resolución:1080p", "fps:120", "tecnología:LED")));

            agregarProducto(new Producto("Monitor Ultrawide LG 34WN80C-B",
                    "Monitor ultrapanorámico de 34 pulgadas ideal para productividad.",
                    new BigDecimal("699.99"),
                    10000,
                    "Monitor",
                    "https://images.visunextgroup.com/images/D/original/2/1000030294/es/lg/LG-34WQ500-B-UltraWide-34-IPS-Monitor.webp",
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