package ms_carrito.carrito.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carrito")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId; // [REF LOGICA] → ms-usuarios

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCarrito estado = EstadoCarrito.ACTIVO;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemCarrito> items = new ArrayList<>();

    public enum EstadoCarrito { ACTIVO, PROCESADO, ABANDONADO }}
