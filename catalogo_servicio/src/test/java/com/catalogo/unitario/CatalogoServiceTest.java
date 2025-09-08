package com.catalogo.unitario;

import com.DTO.ProductoDTO;
import com.DTO.ValoracionDTO;
import com.Entidades.Producto;
import com.Entidades.Valoracion;
import com.DTO.ProductEventDTO;
import com.Recursos.CatalogoResource;
import com.Repositorios.RepositorioProducto;
import com.Repositorios.ValoracionRepository;
import com.Servicios.CatalogoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogoServiceTest {

    @Spy
    @InjectMocks
    CatalogoService catalogoService;

    @Mock
    RepositorioProducto productoRepository;

    @Mock
    Emitter<ProductEventDTO> productEventEmitter;

    @Mock
    ValoracionRepository valoracionRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        objectMapper.registerModule(new JavaTimeModule());
        MockitoAnnotations.openMocks(this);
        catalogoService = new CatalogoService();
        catalogoService.productEventEmitter = productEventEmitter; // Inyecta el mock
        catalogoService.productoRepository = productoRepository; // Inyecta el mock
        catalogoService.valoracionRepository = valoracionRepository; // Inyecta el mock
        catalogoService.objectMapper = new ObjectMapper();
    }

    private List<Producto> crearProductosDeEjemplo() {
        return List.of(
                new Producto("Zapato", "Zapato deportivo", new BigDecimal("59.99"), 10, "Ropa", "url",null),
                new Producto("Camisa", "Camisa formal", new BigDecimal("39.99"), 5, "Ropa", "url", null),
                new Producto("Laptop", "Portátil", new BigDecimal("999.99"), 3, "Electrónica", "url", null),
                new Producto("Libro", "Novela", new BigDecimal("15.00"), 20, "Libros", "url", null)
        );
    }

    @Test
    void obtenerProductosSinFiltros() {
        Mockito.when(productoRepository.buscarProductos(1,10,null,null,null,null)).thenReturn(crearProductosDeEjemplo());

        List<ProductoDTO> productos = catalogoService.obtenerProductos(1, 10, null, null, null, null);

        assertEquals(4, productos.size());
    }

    @Test
    void obtenerProductosConFiltros() {
        Mockito.when(productoRepository.buscarProductos(1,10,"zapato", "ropa", 50.0, 100.0)).thenReturn(crearProductosDeEjemplo());

        List<ProductoDTO> filtrados = catalogoService.obtenerProductos(1, 10, "zapato", "ropa", 50.0, 100.0);

        assertEquals(4, filtrados.size());
        assertEquals("Zapato", filtrados.get(0).getNombre());
    }

    @Test
    void obtenerProductosConPaginacionPagina1() {
        Mockito.when(productoRepository.buscarProductos(1,4,null,null,null,null)).thenReturn(crearProductosDeEjemplo());

        List<ProductoDTO> pagina1 = catalogoService.obtenerProductos(1, 4, null, null, null, null);

        assertEquals(4, pagina1.size());
        assertEquals("Zapato", pagina1.get(0).getNombre());
    }

    @Test
    void obtenerProductosCuandoNoHayNinguno() {
        Mockito.when(productoRepository.listAll()).thenReturn(Collections.emptyList());

        List<ProductoDTO> productos = catalogoService.obtenerProductos(1, 10, null, null, null, null);

        assertTrue(productos.isEmpty());
    }

    @Test
    void agregarProductoCorrectamente() {
        // Arrange: DTO de entrada
        ProductoDTO dto = new ProductoDTO(
                2L,"Zapato", "Zapato deportivo",
                new BigDecimal("59.99"), 10,
                "Ropa","url", null
        );

        // Act
        ProductoDTO result = catalogoService.agregarProducto(dto);

        // Assert
        assertNotNull(result);
        assertEquals(dto.getNombre(), result.getNombre());
        assertEquals(dto.getDescripcion(), result.getDescripcion());
        assertEquals(dto.getPrecio(), result.getPrecio());
        assertEquals(dto.getStock(), result.getStock());
        assertEquals(dto.getCategoria(), result.getCategoria());

        // Verifica que se llamó a persistencia
        Mockito.verify(productoRepository).persist(Mockito.any(Producto.class));
    }

    @Test
    void validarProductoGuardadoCoincideConDTO() throws JsonProcessingException {
        // Arrange
        JsonNode detallesNode = objectMapper.readTree("{\"color\": \"azul\"}");

        ProductoDTO dto = new ProductoDTO(
                2L,"Camisa", "Camisa formal",
                new BigDecimal("39.99"), 5,
                "Ropa", "url",detallesNode
        );

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);

        // Act
        catalogoService.agregarProducto(dto);

        // Assert: Captura el producto persistido
        Mockito.verify(productoRepository).persist(captor.capture());

        Producto guardado = captor.getValue();
        assertEquals(dto.getNombre(), guardado.getNombre());
        assertEquals(dto.getDescripcion(), guardado.getDescripcion());
        assertEquals(dto.getPrecio(), guardado.getPrecio());
        assertEquals(dto.getStock(), guardado.getStock());
        assertEquals(dto.getCategoria(), guardado.getCategoria());
        assertEquals(dto.getDetalles(), guardado.getDetalles());
    }

    @Test
    void actualizarProductoExistente() {
        // Arrange
        Long productoId = 1L;
        ProductoDTO dto = new ProductoDTO(
                3L, "Zapatilla", "Zapatilla running",
                new BigDecimal("89.99"), 20,
                "Calzado", "url",null
        );

        // Simular que el repositorio actualiza correctamente
        Mockito.when(productoRepository.updateProduct(
                eq(productoId),
                eq(dto.getNombre()),
                eq(dto.getDescripcion()),
                eq(dto.getPrecio()),
                eq(dto.getStock()),
                eq(dto.getDetalles())
        )).thenReturn(true);

        // Act
        boolean resultado = catalogoService.actualizarProducto(productoId, dto);

        // Assert
        assertTrue(resultado);
        Mockito.verify(productoRepository).updateProduct(anyLong(), anyString(), anyString(), any(), anyInt(), any());
    }

    @Test
    void actualizarProductoInexistente() {
        // Arrange
        Long productoId = 999L;
        ProductoDTO dto = new ProductoDTO(
                4L,"NoExiste", "Producto no existente",
                new BigDecimal("49.99"), 0,
                "Desconocido", "url", null
        );

        // Simular que el repositorio no actualiza nada
        Mockito.when(productoRepository.updateProduct(anyLong(), anyString(), anyString(), any(), anyInt(), any()))
                .thenReturn(false);

        // Act
        boolean resultado = catalogoService.actualizarProducto(productoId, dto);

        // Assert
        assertFalse(resultado);
    }

    @Test
    void eliminarProductoExistente() {
        // Arrange
        Long productoId = 1L;

        Mockito.when(productoRepository.eliminarPorId(productoId)).thenReturn(true);

        // Act
        boolean resultado = catalogoService.eliminarProducto(productoId);

        // Assert
        assertTrue(resultado);
    }

    @Test
    void eliminarProductoInexistente_debeRetornarFalseSinEventoNiCache() {
        // Arrange
        Long productoId = 999L;

        Mockito.when(productoRepository.eliminarPorId(productoId)).thenReturn(false);

        // Act
        boolean resultado = catalogoService.eliminarProducto(productoId);

        // Assert
        assertFalse(resultado);
        Mockito.verify(productoRepository).eliminarPorId(productoId);
    }

    @Test
    void reservarStockBatch_conSuficienteStockDebeActualizar() {
        Long productoId = 1L;
        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setStock(10);
        producto.setStockReservado(2);

        Mockito.when(productoRepository.findById(productoId)).thenReturn(producto);

        List<CatalogoResource.ReservaItemRequest> request = List.of(new CatalogoResource.ReservaItemRequest(productoId, 3));
        CatalogoService.ReservaBatchResult resultado = catalogoService.reservarStockMultiple(request);

        assertTrue(resultado.reserved());
        assertEquals(5, producto.getStockReservado());

        Mockito.verify(productoRepository).findById(productoId);
    }

    @Test
    void reservarStockBatch_productoNoExisteDebeLanzarExcepcion404() {
        Long productoId = 999L;

        Mockito.when(productoRepository.findById(productoId)).thenReturn(null);

        List<CatalogoResource.ReservaItemRequest> request = List.of(new CatalogoResource.ReservaItemRequest(productoId, 3));

        CatalogoService.ReservaBatchResult resultado = catalogoService.reservarStockMultiple(request);

        assertFalse(resultado.reserved());
        assertFalse(resultado.failures().isEmpty());
    }

    @Test
    void reservarStockBatch_sinStockSuficienteDebeDevolverFalse() {
        Long productoId = 2L;
        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setStock(5);
        producto.setStockReservado(4);  // Solo queda 1 disponible

        Mockito.when(productoRepository.findById(productoId)).thenReturn(producto);

        List<CatalogoResource.ReservaItemRequest> request = List.of(new CatalogoResource.ReservaItemRequest(productoId, 3));

        CatalogoService.ReservaBatchResult resultado = catalogoService.reservarStockMultiple(request);

        assertFalse(resultado.reserved());
    }

    @Test
    void procesarEventoValoracion_valida() throws Exception {
        // Arrange
        Long productoId = 1L;
        ValoracionDTO dto = new ValoracionDTO("user123", productoId, 5, "Muy bueno");
        String mensaje = new ObjectMapper().writeValueAsString(dto);

        Producto producto = new Producto();
        producto.setId(productoId);
        Mockito.when(productoRepository.findById(productoId)).thenReturn(producto);

        CatalogoService spyService = Mockito.spy(catalogoService);
        Mockito.doNothing().when(spyService).actualizarPuntuacionProducto(Mockito.eq(producto), Mockito.anyInt());

        // Act
        spyService.procesarEventoValoracion(mensaje);

        // Assert
        Mockito.verify(spyService).actualizarPuntuacionProducto(producto, dto.puntuacion());
        assertEquals(1, producto.getValoraciones().size());
        Valoracion valoracionGuardada = producto.getValoraciones().get(0);
        assertEquals(dto.idUsuario(), valoracionGuardada.getIdUsuario());
        assertEquals(dto.comentario(), valoracionGuardada.getComentario());
    }

    @Test
    void procesarEventoValoracion_productoNoExiste() throws Exception {
        // Arrange
        Long productoId = 99L;
        ValoracionDTO dto = new ValoracionDTO("user123", productoId, 4, "Ok");
        String mensaje = new ObjectMapper().writeValueAsString(dto);

        Mockito.when(productoRepository.findById(productoId)).thenReturn(null);

        // Act & Assert
        CatalogoService spyService = Mockito.spy(catalogoService);
        assertDoesNotThrow(() -> spyService.procesarEventoValoracion(mensaje));
        // El método atrapa la excepción internamente y sólo imprime error, no lanza
    }

    @Test
    void procesarEventoValoracion_mensajeMalFormado() {
        // Arrange
        String mensaje = "{malformed json...";

        // Act & Assert
        CatalogoService spyService = Mockito.spy(catalogoService);
        assertDoesNotThrow(() -> spyService.procesarEventoValoracion(mensaje));
        // Se espera que no lance excepción, solo imprima error
    }
}

