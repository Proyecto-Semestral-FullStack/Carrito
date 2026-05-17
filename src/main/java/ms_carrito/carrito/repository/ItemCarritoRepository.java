package ms_carrito.carrito.repository;

import ms_carrito.carrito.model.ItemCarrito;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCarritoRepository extends JpaRepository<ItemCarrito,Long> {
}
