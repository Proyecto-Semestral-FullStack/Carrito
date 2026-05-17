package ms_carrito.carrito.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ms_carrito.carrito.dto.CarritoResponseDTO;
import ms_carrito.carrito.dto.ItemCarritoRequestDTO;
import ms_carrito.carrito.exception.RecursoNoEncontradoException;
import ms_carrito.carrito.model.Carrito;
import ms_carrito.carrito.model.ItemCarrito;
import ms_carrito.carrito.repository.CarritoRepository;
import ms_carrito.carrito.webclient.CatalogoClient;
import ms_carrito.carrito.webclient.InventarioClient;
import ms_carrito.carrito.webclient.UsuarioClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;



@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CarritoService {
    private final CarritoRepository carritoRepository;
    private final CatalogoClient catalogoClient;
    private final UsuarioClient usuarioClient;
    private final InventarioClient inventarioClient;

    @Transactional(readOnly = true)
    public CarritoResponseDTO obtenerCarrito(Long usuarioId) {
        Carrito carrito = carritoRepository.findByUsuarioIdAndEstado(usuarioId, Carrito.EstadoCarrito.ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Carrito no encontrado para el usuario " + usuarioId));
        return convertirADto(carrito);
    }

    public CarritoResponseDTO agregarItem(Long usuarioId, ItemCarritoRequestDTO dto) {
        // 1. Validar que el usuario existe
        usuarioClient.validarUsuario(usuarioId);

        // 2. Obtener nombre y precio del producto desde Catalogo
        CatalogoClient.CatalogoProductoInfo info = catalogoClient.obtenerProducto(dto.getProductoId());

        // 3. Verificar stock disponible desde Inventario
        inventarioClient.verificarStock(dto.getProductoId(), dto.getCantidad());

        // 4. Obtener o crear carrito activo
        Carrito carrito = carritoRepository.findByUsuarioIdAndEstado(usuarioId, Carrito.EstadoCarrito.ACTIVO)
                .orElseGet(() -> {
                    Carrito nuevo = Carrito.builder()
                            .usuarioId(usuarioId)
                            .estado(Carrito.EstadoCarrito.ACTIVO)
                            .build();
                    return carritoRepository.save(nuevo);
                });


        ItemCarrito item = ItemCarrito.builder()
                .carrito(carrito)
                .productoId(dto.getProductoId())
                .nombreProducto(info.nombre())
                .cantidad(dto.getCantidad())
                .precioUnitario(info.precio())
                .build();

        carrito.getItems().add(item);
        carrito.setActualizadoEn(LocalDateTime.now());
        carritoRepository.save(carrito);

        log.info("Producto agregado al carrito: usuario={}, producto={}", usuarioId, dto.getProductoId());
        return convertirADto(carrito);
    }

    public CarritoResponseDTO quitarItem(Long usuarioId, Long itemId) {
        Carrito carrito = obtenerCarritoActivo(usuarioId);
        carrito.getItems().removeIf(item -> item.getId().equals(itemId));
        carrito.setActualizadoEn(LocalDateTime.now());
        carritoRepository.save(carrito);
        return convertirADto(carrito);
    }

    public void vaciarCarrito(Long usuarioId) {
        Carrito carrito = obtenerCarritoActivo(usuarioId);
        carrito.getItems().clear();
        carrito.setActualizadoEn(LocalDateTime.now());
        carritoRepository.save(carrito);
    }

    private Carrito obtenerCarritoActivo(Long usuarioId) {
        return carritoRepository.findByUsuarioIdAndEstado(usuarioId, Carrito.EstadoCarrito.ACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay carrito activo para el usuario " + usuarioId));
    }

    private CarritoResponseDTO convertirADto(Carrito carrito) {
        List<CarritoResponseDTO.ItemResponseDTO> items = carrito.getItems().stream()
                .map(item -> CarritoResponseDTO.ItemResponseDTO.builder()
                        .id(item.getId())
                        .productoId(item.getProductoId())
                        .nombreProducto(item.getNombreProducto())
                        .cantidad(item.getCantidad())
                        .precioUnitario(item.getPrecioUnitario())
                        .subtotal(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())))
                        .build())
                .collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(CarritoResponseDTO.ItemResponseDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CarritoResponseDTO.builder()
                .id(carrito.getId())
                .usuarioId(carrito.getUsuarioId())
                .estado(carrito.getEstado().name())
                .items(items)
                .total(total)
                .actualizadoEn(carrito.getActualizadoEn())
                .build();
    }

}
