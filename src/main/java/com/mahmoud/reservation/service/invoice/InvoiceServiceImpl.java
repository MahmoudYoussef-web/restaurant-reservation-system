package com.mahmoud.reservation.service.invoice;

import com.mahmoud.reservation.dto.invoice.InvoiceResponse;
import com.mahmoud.reservation.entity.Invoice;
import com.mahmoud.reservation.entity.Order;
import com.mahmoud.reservation.entity.OrderItem;
import com.mahmoud.reservation.enums.OrderStatus;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.repository.InvoiceRepository;
import com.mahmoud.reservation.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByOrder(Long orderId) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseGet(() -> generateInvoice(orderId));
        return toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return toResponse(invoice);
    }

    private Invoice generateInvoice(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Invoice can only be generated for completed orders");
        }

        if (invoiceRepository.existsByOrderId(orderId)) {
            return invoiceRepository.findByOrderId(orderId).get();
        }

        BigDecimal subtotal = order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxPercent = new BigDecimal("14.00");
        BigDecimal serviceChargePercent = new BigDecimal("10.00");

        BigDecimal taxAmount = subtotal.multiply(taxPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal serviceChargeAmount = subtotal.multiply(serviceChargePercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxAmount).add(serviceChargeAmount);

        Invoice invoice = Invoice.builder()
                .order(order)
                .subtotal(subtotal)
                .taxPercent(taxPercent)
                .serviceChargePercent(serviceChargePercent)
                .taxAmount(taxAmount)
                .serviceChargeAmount(serviceChargeAmount)
                .total(total)
                .generatedAt(Instant.now())
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Generated invoice {} for order {}", saved.getId(), orderId);
        return saved;
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .orderId(invoice.getOrder().getId())
                .subtotal(invoice.getSubtotal())
                .taxPercent(invoice.getTaxPercent())
                .serviceChargePercent(invoice.getServiceChargePercent())
                .taxAmount(invoice.getTaxAmount())
                .serviceChargeAmount(invoice.getServiceChargeAmount())
                .total(invoice.getTotal())
                .generatedAt(invoice.getGeneratedAt())
                .build();
    }
}
