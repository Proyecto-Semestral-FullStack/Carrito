package ms_carrito.carrito.repository;

import ms_carrito.carrito.model.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito,Long> {
    Optional<Carrito> findByUsuarioIdAndEstado(Long usuarioId, Carrito.EstadoCarrito estado);
}
