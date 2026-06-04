package com.mahmoud.reservation.service.invoice;

import com.mahmoud.reservation.dto.invoice.InvoiceResponse;

public interface InvoiceService {

    InvoiceResponse getInvoiceByOrder(Long orderId);

    InvoiceResponse getInvoiceById(Long invoiceId);
}
