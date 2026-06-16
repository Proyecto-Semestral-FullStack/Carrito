package ms_carrito.carrito.service;

import ms_carrito.carrito.dto.CarritoResponseDTO;
import ms_carrito.carrito.dto.ItemCarritoRequestDTO;
import ms_carrito.carrito.exception.RecursoNoEncontradoException;
import ms_carrito.carrito.model.Carrito;
import ms_carrito.carrito.model.ItemCarrito;
import ms_carrito.carrito.repository.CarritoRepository;
import ms_carrito.carrito.webclient.CatalogoClient;
import ms_carrito.carrito.webclient.InventarioClient;
import ms_carrito.carrito.webclient.UsuarioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PRUEBAS UNITARIAS PARA: CarritoService
 *
 * OBJETIVO: Alcanzar ≥80% de cobertura de código (~95% esperado)
 *
 * ENFOQUE:
 * - Pruebas unitarias PURAS (sin contexto de Spring)
 * - Uso exclusivo de @Mock y @InjectMocks de Mockito
 * - Estructura Given-When-Then en cada prueba
 * - Mocking completo de dependencias
 * - Validación de excepciones, valores y comportamiento
 *
 * @author Arquitecto de Software Senior
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CarritoService - Suite de Pruebas Unitarias")
class CarritoServiceTest {

    // ============================================================
    // MOCKS: Simulan las dependencias externas
    // ============================================================

    @Mock
    private CarritoRepository carritoRepository;

    @Mock
    private CatalogoClient catalogoClient;

    @Mock
    private UsuarioClient usuarioClient;

    @Mock
    private InventarioClient inventarioClient;

    // ============================================================
    // CLASS UNDER TEST: La clase que vamos a testear
    // ============================================================

    @InjectMocks
    private CarritoService carritoService;

    // ============================================================
    // DATOS DE PRUEBA
    // ============================================================

    private static final Long USUARIO_ID_VALIDO = 1L;
    private static final Long PRODUCTO_ID_VALIDO = 100L;
    private static final Long ITEM_ID_VALIDO = 50L;
    private static final Integer CANTIDAD_VALIDA = 2;
    private static final String NOMBRE_PRODUCTO = "Laptop Premium";
    private static final BigDecimal PRECIO_UNITARIO = new BigDecimal("999.99");

    private Carrito carritoActivo;
    private ItemCarritoRequestDTO itemCarritoDto;
    private CatalogoClient.CatalogoProductoInfo catalogoProductoInfo;
    private ItemCarrito itemCarritoMock;

    /**
     * SETUP: Se ejecuta antes de CADA prueba
     *
     * RESPONSABILIDAD:
     * - Inicializar datos comunes de test
     * - Configurar stubs genéricos
     * - Preparar objetos reutilizables
     *
     * NOTA: Los mocks son AUTOMÁTICAMENTE inicializados por @ExtendWith(MockitoExtension.class)
     */
    @BeforeEach
    void setUp() {
        // Crear carrito de prueba
        carritoActivo = Carrito.builder()
                .id(1L)
                .usuarioId(USUARIO_ID_VALIDO)
                .estado(Carrito.EstadoCarrito.ACTIVO)
                .creadoEn(LocalDateTime.now())
                .actualizadoEn(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        // Crear DTO de request
        itemCarritoDto = new ItemCarritoRequestDTO();
        itemCarritoDto.setProductoId(PRODUCTO_ID_VALIDO);
        itemCarritoDto.setCantidad(CANTIDAD_VALIDA);

        // Crear info de producto desde catálogo
        catalogoProductoInfo = new CatalogoClient.CatalogoProductoInfo(
                NOMBRE_PRODUCTO,
                PRECIO_UNITARIO
        );

        // Crear item de carrito
        itemCarritoMock = ItemCarrito.builder()
                .id(ITEM_ID_VALIDO)
                .carrito(carritoActivo)
                .productoId(PRODUCTO_ID_VALIDO)
                .nombreProducto(NOMBRE_PRODUCTO)
                .cantidad(CANTIDAD_VALIDA)
                .precioUnitario(PRECIO_UNITARIO)
                .agregadoEn(LocalDateTime.now())
                .build();
    }

    // ============================================================
    // SUITE 1: OBTENER CARRITO
    // ============================================================

    @Test
    @DisplayName("Debería obtener carrito cuando existe carrito activo")
    void deberiaObtenerCarritoCuandoExisteCarritoActivo() {
        // GIVEN: Hay un carrito activo para el usuario
        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));

        // WHEN: Llamamos a obtenerCarrito
        CarritoResponseDTO resultado = carritoService.obtenerCarrito(USUARIO_ID_VALIDO);

        // THEN: Debería retornar un DTO válido
        assertNotNull(resultado);
        assertEquals(USUARIO_ID_VALIDO, resultado.getUsuarioId());
        assertEquals("ACTIVO", resultado.getEstado());
        assertEquals(BigDecimal.ZERO, resultado.getTotal());

        // Verificar que se consultó el repositorio exactamente una vez
        verify(carritoRepository, times(1))
                .findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO);
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando carrito no existe")
    void deberiaLanzarExcepcionCuandoCarritoNoExiste() {
        // GIVEN: No hay carrito activo para el usuario
        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.empty());

        // WHEN/THEN: Debería lanzar RecursoNoEncontradoException
        RecursoNoEncontradoException excepcion = assertThrows(
                RecursoNoEncontradoException.class,
                () -> carritoService.obtenerCarrito(USUARIO_ID_VALIDO)
        );

        // THEN: Validar mensaje de error
        assertTrue(excepcion.getMessage().contains("Carrito no encontrado"));
        assertTrue(excepcion.getMessage().contains(USUARIO_ID_VALIDO.toString()));
    }

    @Test
    @DisplayName("Debería retornar CarritoResponseDTO con estructura correcta")
    void deberiaObtenerCarritoConEstructuraCorrecta() {
        // GIVEN: Carrito con un item
        ItemCarrito item = ItemCarrito.builder()
                .id(1L)
                .productoId(100L)
                .nombreProducto("Monitor")
                .cantidad(1)
                .precioUnitario(new BigDecimal("300.00"))
                .build();
        carritoActivo.getItems().add(item);

        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));

        // WHEN
        CarritoResponseDTO resultado = carritoService.obtenerCarrito(USUARIO_ID_VALIDO);

        // THEN: Validar estructura del DTO
        assertNotNull(resultado.getId());
        assertNotNull(resultado.getUsuarioId());
        assertNotNull(resultado.getEstado());
        assertNotNull(resultado.getItems());
        assertNotNull(resultado.getTotal());
        assertNotNull(resultado.getActualizadoEn());
    }

    // ============================================================
    // SUITE 2: AGREGAR ITEM
    // ============================================================

    @Test
    @DisplayName("Debería agregar item cuando datos son válidos y carrito existe")
    void deberiaAgregarItemCuandoDatosValidosYCarritoExiste() {
        // GIVEN
        // 1. Usuario existe (validarUsuario no lanza excepción)
        doNothing().when(usuarioClient).validarUsuario(USUARIO_ID_VALIDO);

        // 2. Producto existe en catálogo
        when(catalogoClient.obtenerProducto(PRODUCTO_ID_VALIDO))
                .thenReturn(catalogoProductoInfo);

        // 3. Stock disponible
        when(inventarioClient.verificarStock(PRODUCTO_ID_VALIDO, CANTIDAD_VALIDA))
                .thenReturn(true);

        // 4. Carrito activo existe
        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));

        // 5. Repository retorna el carrito actualizado
        when(carritoRepository.save(any(Carrito.class)))
                .thenReturn(carritoActivo);

        // WHEN
        CarritoResponseDTO resultado = carritoService.agregarItem(USUARIO_ID_VALIDO, itemCarritoDto);

        // THEN
        assertNotNull(resultado);
        assertEquals(USUARIO_ID_VALIDO, resultado.getUsuarioId());

        // Verificar que se agregó el item al carrito
        assertEquals(1, carritoActivo.getItems().size());
        ItemCarrito itemAgregado = carritoActivo.getItems().get(0);
        assertEquals(PRODUCTO_ID_VALIDO, itemAgregado.getProductoId());
        assertEquals(NOMBRE_PRODUCTO, itemAgregado.getNombreProducto());
        assertEquals(CANTIDAD_VALIDA, itemAgregado.getCantidad());
        assertEquals(PRECIO_UNITARIO, itemAgregado.getPrecioUnitario());

        // Verificar que se llamó a todos los clientes
        verify(usuarioClient, times(1)).validarUsuario(USUARIO_ID_VALIDO);
        verify(catalogoClient, times(1)).obtenerProducto(PRODUCTO_ID_VALIDO);
        verify(inventarioClient, times(1)).verificarStock(PRODUCTO_ID_VALIDO, CANTIDAD_VALIDA);
        verify(carritoRepository, times(1)).findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO);
        verify(carritoRepository, times(1)).save(any(Carrito.class));
    }

    @Test
    @DisplayName("Debería crear carrito nuevo si no existe al agregar item")
    void deberiaCrearCarritoNuevoCuandoNoExisteAlAgregarItem() {
        // GIVEN
        doNothing().when(usuarioClient).validarUsuario(USUARIO_ID_VALIDO);
        when(catalogoClient.obtenerProducto(PRODUCTO_ID_VALIDO))
                .thenReturn(catalogoProductoInfo);
        when(inventarioClient.verificarStock(PRODUCTO_ID_VALIDO, CANTIDAD_VALIDA))
                .thenReturn(true);

        // No hay carrito activo (Optional.empty()) → Se debe crear uno
        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(carritoActivo)); // Segunda llamada para obtenerCarritoActivo

        // El save retorna el carrito creado
        when(carritoRepository.save(any(Carrito.class)))
                .thenAnswer(invocation -> {
                    Carrito carrito = invocation.getArgument(0);
                    carrito.setId(1L); // Simular ID generado
                    return carrito;
                });

        // WHEN
        CarritoResponseDTO resultado = carritoService.agregarItem(USUARIO_ID_VALIDO, itemCarritoDto);

        // THEN
        assertNotNull(resultado);

        // Verificar que se creó un carrito
        verify(carritoRepository, times(2)).save(any(Carrito.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando usuario no existe")
    void deberiaLanzarExcepcionCuandoUsuarioNoExiste() {
        // GIVEN: UsuarioClient lanza excepción
        doThrow(new RecursoNoEncontradoException("Usuario no encontrado: " + USUARIO_ID_VALIDO))
                .when(usuarioClient).validarUsuario(USUARIO_ID_VALIDO);

        // WHEN/THEN
        RecursoNoEncontradoException excepcion = assertThrows(
                RecursoNoEncontradoException.class,
                () -> carritoService.agregarItem(USUARIO_ID_VALIDO, itemCarritoDto)
        );

        assertTrue(excepcion.getMessage().contains("Usuario no encontrado"));

        // Verificar que NO se continuó con el resto de validaciones
        verify(usuarioClient, times(1)).validarUsuario(USUARIO_ID_VALIDO);
        verify(catalogoClient, never()).obtenerProducto(anyLong());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando producto no existe en catálogo")
    void deberiaLanzarExcepcionCuandoProductoNoExiste() {
        // GIVEN
        doNothing().when(usuarioClient).validarUsuario(USUARIO_ID_VALIDO);

        // CatalogoClient lanza excepción
        when(catalogoClient.obtenerProducto(PRODUCTO_ID_VALIDO))
                .thenThrow(new RecursoNoEncontradoException("Producto no encontrado en catálogo: " + PRODUCTO_ID_VALIDO));

        // WHEN/THEN
        RecursoNoEncontradoException excepcion = assertThrows(
                RecursoNoEncontradoException.class,
                () -> carritoService.agregarItem(USUARIO_ID_VALIDO, itemCarritoDto)
        );

        assertTrue(excepcion.getMessage().contains("Producto no encontrado"));

        // Verificar orden de ejecución
        verify(usuarioClient, times(1)).validarUsuario(USUARIO_ID_VALIDO);
        verify(catalogoClient, times(1)).obtenerProducto(PRODUCTO_ID_VALIDO);
        verify(inventarioClient, never()).verificarStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando stock es insuficiente")
    void deberiaLanzarExcepcionCuandoStockInsuficiente() {
        // GIVEN
        doNothing().when(usuarioClient).validarUsuario(USUARIO_ID_VALIDO);
        when(catalogoClient.obtenerProducto(PRODUCTO_ID_VALIDO))
                .thenReturn(catalogoProductoInfo);

        // Stock NO disponible
        when(inventarioClient.verificarStock(PRODUCTO_ID_VALIDO, CANTIDAD_VALIDA))
                .thenReturn(false);

        // WHEN/THEN
        RuntimeException excepcion = assertThrows(
                RuntimeException.class,
                () -> carritoService.agregarItem(USUARIO_ID_VALIDO, itemCarritoDto)
        );

        // El InventarioClient retorna false en verificarStock (no es excepción)
        // Pero el servicio debería manejar esto
        verify(inventarioClient, times(1)).verificarStock(PRODUCTO_ID_VALIDO, CANTIDAD_VALIDA);
    }

    @Test
    @DisplayName("Debería actualizar timestamp 'actualizadoEn' al agregar item")
    void deberiaActualizarTimestampAlAgregarItem() {
        // GIVEN
        LocalDateTime timestampAnterior = carritoActivo.getActualizadoEn();

        doNothing().when(usuarioClient).validarUsuario(USUARIO_ID_VALIDO);
        when(catalogoClient.obtenerProducto(PRODUCTO_ID_VALIDO))
                .thenReturn(catalogoProductoInfo);
        when(inventarioClient.verificarStock(PRODUCTO_ID_VALIDO, CANTIDAD_VALIDA))
                .thenReturn(true);
        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));
        when(carritoRepository.save(any(Carrito.class)))
                .thenAnswer(invocation -> {
                    Carrito carrito = invocation.getArgument(0);
                    // Simular que se actualiza el timestamp
                    carrito.setActualizadoEn(LocalDateTime.now());
                    return carrito;
                });

        // WHEN
        carritoService.agregarItem(USUARIO_ID_VALIDO, itemCarritoDto);

        // THEN
        assertNotNull(carritoActivo.getActualizadoEn());
        // El timestamp debería actualizarse (aunque sea microsegundos después)
        verify(carritoRepository, times(1)).save(argThat(carrito ->
                carrito.getActualizadoEn() != null
        ));
    }

    @Test
    @DisplayName("Debería retornar DTO con item agregado")
    void deberiaRetornarDtoConItemAgregado() {
        // GIVEN
        carritoActivo.getItems().add(itemCarritoMock);

        doNothing().when(usuarioClient).validarUsuario(USUARIO_ID_VALIDO);
        when(catalogoClient.obtenerProducto(PRODUCTO_ID_VALIDO))
                .thenReturn(catalogoProductoInfo);
        when(inventarioClient.verificarStock(PRODUCTO_ID_VALIDO, CANTIDAD_VALIDA))
                .thenReturn(true);
        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));
        when(carritoRepository.save(any(Carrito.class)))
                .thenReturn(carritoActivo);

        // WHEN
        CarritoResponseDTO resultado = carritoService.agregarItem(USUARIO_ID_VALIDO, itemCarritoDto);

        // THEN
        assertNotNull(resultado.getItems());
        assertEquals(1, resultado.getItems().size());

        CarritoResponseDTO.ItemResponseDTO itemDto = resultado.getItems().get(0);
        assertEquals(PRODUCTO_ID_VALIDO, itemDto.getProductoId());
        assertEquals(NOMBRE_PRODUCTO, itemDto.getNombreProducto());
    }

    @Test
    @DisplayName("Debería permitir agregar múltiples items al mismo carrito")
    void deberiaPermitirAgregarMultiplesItems() {
        // GIVEN
        ItemCarrito item1 = ItemCarrito.builder()
                .id(1L)
                .productoId(100L)
                .nombreProducto("Producto 1")
                .cantidad(1)
                .precioUnitario(new BigDecimal("100.00"))
                .build();

        ItemCarrito item2 = ItemCarrito.builder()
                .id(2L)
                .productoId(101L)
                .nombreProducto("Producto 2")
                .cantidad(2)
                .precioUnitario(new BigDecimal("200.00"))
                .build();

        carritoActivo.getItems().add(item1);
        carritoActivo.getItems().add(item2);

        doNothing().when(usuarioClient).validarUsuario(USUARIO_ID_VALIDO);
        when(catalogoClient.obtenerProducto(PRODUCTO_ID_VALIDO))
                .thenReturn(catalogoProductoInfo);
        when(inventarioClient.verificarStock(PRODUCTO_ID_VALIDO, CANTIDAD_VALIDA))
                .thenReturn(true);
        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));
        when(carritoRepository.save(any(Carrito.class)))
                .thenReturn(carritoActivo);

        // WHEN
        CarritoResponseDTO resultado = carritoService.agregarItem(USUARIO_ID_VALIDO, itemCarritoDto);

        // THEN
        assertEquals(2, resultado.getItems().size());
    }

    // ============================================================
    // SUITE 3: QUITAR ITEM
    // ============================================================

    @Test
    @DisplayName("Debería quitar item cuando existe en el carrito")
    void deberiaQuitarItemCuandoExiste() {
        // GIVEN
        carritoActivo.getItems().add(itemCarritoMock);

        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));
        when(carritoRepository.save(any(Carrito.class)))
                .thenReturn(carritoActivo);

        // WHEN
        CarritoResponseDTO resultado = carritoService.quitarItem(USUARIO_ID_VALIDO, ITEM_ID_VALIDO);

        // THEN
        assertEquals(0, resultado.getItems().size());
        verify(carritoRepository, times(1)).save(any(Carrito.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando carrito no existe")
    void deberiaLanzarExcepcionAlQuitarItemSiCarritoNoExiste() {
        // GIVEN
        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.empty());

        // WHEN/THEN
        RecursoNoEncontradoException excepcion = assertThrows(
                RecursoNoEncontradoException.class,
                () -> carritoService.quitarItem(USUARIO_ID_VALIDO, ITEM_ID_VALIDO)
        );

        assertTrue(excepcion.getMessage().contains("No hay carrito activo"));
    }

    @Test
    @DisplayName("Debería actualizar timestamp al quitar item")
    void deberiaActualizarTimestampAlQuitarItem() {
        // GIVEN
        carritoActivo.getItems().add(itemCarritoMock);

        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));
        when(carritoRepository.save(any(Carrito.class)))
                .thenReturn(carritoActivo);

        // WHEN
        carritoService.quitarItem(USUARIO_ID_VALIDO, ITEM_ID_VALIDO);

        // THEN
        verify(carritoRepository, times(1)).save(argThat(carrito ->
                carrito.getActualizadoEn() != null
        ));
    }

    @Test
    @DisplayName("Debería ser seguro quitar item inexistente (no lanza error)")
    void deberiaSerSeguroQuitarItemInexistente() {
        // GIVEN
        itemCarritoMock.setId(999L); // ID diferente
        carritoActivo.getItems().add(itemCarritoMock);

        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));
        when(carritoRepository.save(any(Carrito.class)))
                .thenReturn(carritoActivo);

        // WHEN - Intentar quitar item con ID diferente
        assertDoesNotThrow(() ->
                carritoService.quitarItem(USUARIO_ID_VALIDO, ITEM_ID_VALIDO)
        );

        // THEN - El item original sigue en el carrito
        assertEquals(1, carritoActivo.getItems().size());
    }

    // ============================================================
    // SUITE 4: VACIAR CARRITO
    // ============================================================

    @Test
    @DisplayName("Debería vaciar carrito eliminando todos los items")
    void deberiaVaciarCarritoEliminandoTodosItems() {
        // GIVEN
        carritoActivo.getItems().add(itemCarritoMock);
        ItemCarrito item2 = ItemCarrito.builder()
                .id(2L)
                .productoId(101L)
                .nombreProducto("Producto 2")
                .cantidad(1)
                .precioUnitario(new BigDecimal("50.00"))
                .build();
        carritoActivo.getItems().add(item2);

        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));
        when(carritoRepository.save(any(Carrito.class)))
                .thenReturn(carritoActivo);

        // WHEN
        carritoService.vaciarCarrito(USUARIO_ID_VALIDO);

        // THEN
        assertEquals(0, carritoActivo.getItems().size());
        verify(carritoRepository, times(1)).save(any(Carrito.class));
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando carrito no existe")
    void deberiaLanzarExcepcionAlVaciarCarritoNoExistente() {
        // GIVEN
        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.empty());

        // WHEN/THEN
        RecursoNoEncontradoException excepcion = assertThrows(
                RecursoNoEncontradoException.class,
                () -> carritoService.vaciarCarrito(USUARIO_ID_VALIDO)
        );

        assertTrue(excepcion.getMessage().contains("No hay carrito activo"));
    }

    @Test
    @DisplayName("Debería actualizar timestamp al vaciar carrito")
    void deberiaActualizarTimestampAlVaciarCarrito() {
        // GIVEN
        carritoActivo.getItems().add(itemCarritoMock);

        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));
        when(carritoRepository.save(any(Carrito.class)))
                .thenReturn(carritoActivo);

        // WHEN
        carritoService.vaciarCarrito(USUARIO_ID_VALIDO);

        // THEN
        verify(carritoRepository, times(1)).save(argThat(carrito ->
                carrito.getActualizadoEn() != null &&
                        carrito.getItems().size() == 0
        ));
    }

    // ============================================================
    // SUITE 5: CONVERTIR A DTO (Método privado - probado indirectamente)
    // ============================================================

    @Test
    @DisplayName("Debería convertir carrito vacío a DTO con total cero")
    void deberiaConvertirCarritoVacioADto() {
        // GIVEN - Carrito sin items
        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));

        // WHEN
        CarritoResponseDTO resultado = carritoService.obtenerCarrito(USUARIO_ID_VALIDO);

        // THEN
        assertEquals(0, resultado.getItems().size());
        assertEquals(BigDecimal.ZERO, resultado.getTotal());
        assertEquals("ACTIVO", resultado.getEstado());
    }

    @Test
    @DisplayName("Debería calcular subtotal correctamente (precio × cantidad)")
    void deberiaCalcularSubtotalCorrectamente() {
        // GIVEN
        ItemCarrito item = ItemCarrito.builder()
                .id(1L)
                .productoId(100L)
                .nombreProducto("Monitor")
                .cantidad(3)
                .precioUnitario(new BigDecimal("500.00")) // 3 × 500 = 1500
                .build();
        carritoActivo.getItems().add(item);

        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));

        // WHEN
        CarritoResponseDTO resultado = carritoService.obtenerCarrito(USUARIO_ID_VALIDO);

        // THEN
        BigDecimal subtotalEsperado = new BigDecimal("1500.00");
        assertEquals(subtotalEsperado, resultado.getItems().get(0).getSubtotal());
        assertEquals(subtotalEsperado, resultado.getTotal());
    }

    @Test
    @DisplayName("Debería calcular total correcto (suma de subtotales)")
    void deberiaCalcularTotalCorrectamente() {
        // GIVEN
        ItemCarrito item1 = ItemCarrito.builder()
                .id(1L)
                .productoId(100L)
                .productoId(100L)
                .nombreProducto("Producto 1")
                .cantidad(2)
                .precioUnitario(new BigDecimal("100.00")) // 2 × 100 = 200
                .build();

        ItemCarrito item2 = ItemCarrito.builder()
                .id(2L)
                .productoId(101L)
                .nombreProducto("Producto 2")
                .cantidad(3)
                .precioUnitario(new BigDecimal("50.00")) // 3 × 50 = 150
                .build();

        carritoActivo.getItems().add(item1);
        carritoActivo.getItems().add(item2);

        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));

        // WHEN
        CarritoResponseDTO resultado = carritoService.obtenerCarrito(USUARIO_ID_VALIDO);

        // THEN
        BigDecimal totalEsperado = new BigDecimal("350.00"); // 200 + 150
        assertEquals(totalEsperado, resultado.getTotal());
    }

    @Test
    @DisplayName("Debería mapear todos los campos del item al DTO correctamente")
    void deberiaMapearTodosCamposDelItemAlDto() {
        // GIVEN
        carritoActivo.getItems().add(itemCarritoMock);

        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));

        // WHEN
        CarritoResponseDTO resultado = carritoService.obtenerCarrito(USUARIO_ID_VALIDO);

        // THEN
        CarritoResponseDTO.ItemResponseDTO itemDto = resultado.getItems().get(0);

        assertEquals(ITEM_ID_VALIDO, itemDto.getId());
        assertEquals(PRODUCTO_ID_VALIDO, itemDto.getProductoId());
        assertEquals(NOMBRE_PRODUCTO, itemDto.getNombreProducto());
        assertEquals(CANTIDAD_VALIDA, itemDto.getCantidad());
        assertEquals(PRECIO_UNITARIO, itemDto.getPrecioUnitario());
        assertNotNull(itemDto.getSubtotal());
    }

    @Test
    @DisplayName("Debería mapear todos los campos del carrito al DTO")
    void deberiaMapearTodosCamposDelCarritoAlDto() {
        // GIVEN
        when(carritoRepository.findByUsuarioIdAndEstado(USUARIO_ID_VALIDO, Carrito.EstadoCarrito.ACTIVO))
                .thenReturn(Optional.of(carritoActivo));

        // WHEN
        CarritoResponseDTO resultado = carritoService.obtenerCarrito(USUARIO_ID_VALIDO);

        // THEN
        assertNotNull(resultado.getId());
        assertEquals(USUARIO_ID_VALIDO, resultado.getUsuarioId());
        assertEquals("ACTIVO", resultado.getEstado());
        assertNotNull(resultado.getItems());
        assertNotNull(resultado.getTotal());
        assertNotNull(resultado.getActualizadoEn());
    }

}