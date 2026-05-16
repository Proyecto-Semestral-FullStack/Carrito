package ms_carrito.carrito.model;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public class ItemCarrito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @Column(name = "producto_id", nullable = false)
    private Long productoId; // [REF LOGICA] → ms-catalogo

    @Column(name = "nombre_producto", nullable = false)
    private String nombreProducto; // Snapshot

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario; // Snapshot

    @Column(name = "agregado_en", nullable = false)
    private LocalDateTime agregadoEn = LocalDateTime.now();
}
