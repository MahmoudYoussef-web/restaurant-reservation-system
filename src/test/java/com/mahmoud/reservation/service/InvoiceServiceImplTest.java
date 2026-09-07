package com.mahmoud.reservation.service;

import com.mahmoud.reservation.dto.invoice.InvoiceResponse;
import com.mahmoud.reservation.entity.Invoice;
import com.mahmoud.reservation.entity.MenuCategory;
import com.mahmoud.reservation.entity.MenuItem;
import com.mahmoud.reservation.entity.Order;
import com.mahmoud.reservation.entity.OrderItem;
import com.mahmoud.reservation.enums.OrderStatus;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.repository.InvoiceRepository;
import com.mahmoud.reservation.repository.OrderRepository;
import com.mahmoud.reservation.service.invoice.InvoiceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private Order completedOrder() {
        MenuItem item = MenuItem.builder().id(6L)
                .category(MenuCategory.builder().id(3L).name("Mains").build())
                .name("Margherita").price(new BigDecimal("200.00")).available(true).build();
        Order order = Order.builder().id(1L).tableId(5L).restaurantId(1L)
                .status(OrderStatus.COMPLETED).items(new HashSet<>()).build();
        OrderItem line = OrderItem.builder().id(11L).order(order).menuItem(item)
                .quantity(2).unitPrice(new BigDecimal("200.00")).build();
        order.setItems(new HashSet<>(Set.of(line)));
        return order;
    }

    @Test
    void getInvoiceByOrder_shouldCalculateTaxAndService() {
        when(invoiceRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(completedOrder()));
        when(invoiceRepository.existsByOrderId(1L)).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> {
            Invoice inv = i.getArgument(0);
            inv.setId(100L);
            return inv;
        });

        InvoiceResponse res = invoiceService.getInvoiceByOrder(1L);

        // subtotal 400, tax 14% = 56, service 10% = 40, total 496
        assertThat(res.getSubtotal()).isEqualByComparingTo("400.00");
        assertThat(res.getTaxAmount()).isEqualByComparingTo("56.00");
        assertThat(res.getServiceChargeAmount()).isEqualByComparingTo("40.00");
        assertThat(res.getTotal()).isEqualByComparingTo("496.00");
    }

    @Test
    void getInvoiceByOrder_shouldThrow_whenOrderNotCompleted() {
        Order order = completedOrder();
        order.setStatus(OrderStatus.SERVED);
        when(invoiceRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> invoiceService.getInvoiceByOrder(1L))
                .isInstanceOf(BadRequestException.class);

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void getInvoiceByOrder_shouldReturnExisting_withoutRegenerating() {
        Order order = completedOrder();
        Invoice existing = Invoice.builder().id(50L).order(order)
                .subtotal(new BigDecimal("400.00"))
                .taxPercent(new BigDecimal("14.00"))
                .serviceChargePercent(new BigDecimal("10.00"))
                .taxAmount(new BigDecimal("56.00"))
                .serviceChargeAmount(new BigDecimal("40.00"))
                .total(new BigDecimal("496.00")).build();
        when(invoiceRepository.findByOrderId(1L)).thenReturn(Optional.of(existing));

        InvoiceResponse res = invoiceService.getInvoiceByOrder(1L);

        assertThat(res.getId()).isEqualTo(50L);
        verify(invoiceRepository, never()).save(any());
    }
}
