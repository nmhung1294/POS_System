package service;

import dto.InvoiceDto;
import dto.InvoiceItemDto;
import repository.InvoiceRepository;
import repository.InvoiceItemRepository;
import repository.StockRepository;
import repository.CustomerRepository;
import repository.PaymentMethodRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of InvoiceService.
 */
public class InvoiceServiceImpl implements InvoiceService {

    private static final Logger logger = Logger.getLogger(InvoiceServiceImpl.class.getName());
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final StockRepository stockRepository;
    private final CustomerRepository customerRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository, 
                             InvoiceItemRepository invoiceItemRepository,
                             StockRepository stockRepository,
                             CustomerRepository customerRepository,
                             PaymentMethodRepository paymentMethodRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.stockRepository = stockRepository;
        this.customerRepository = customerRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Override
    public void saveInvoice(InvoiceDto invoiceDto) throws Exception {
        // Use transaction to ensure data consistency
        // If any operation fails, all changes are rolled back
        Connection conn = null;
        try {
            conn = util.DBUtil.getTransactionalConnection();
            
            // 1. Create invoice
            invoiceRepository.createInvoice(
                invoiceDto.getId(),
                invoiceDto.getCustomerMobile(),
                invoiceDto.getDiscount(),
                invoiceDto.getPaidAmount(),
                invoiceDto.getPaymentMethodId(),
                invoiceDto.getBalance(),
                invoiceDto.getDateTime()
            );

            // 2. Create invoice items and update stock
            for (InvoiceItemDto item : invoiceDto.getItems()) {
                invoiceItemRepository.createInvoiceItem(
                    item.getStockId(),
                    item.getQty(),
                    invoiceDto.getId()
                );

                // Decrease stock quantity
                stockRepository.decreaseStockQuantity(
                    item.getStockId(),
                    Double.parseDouble(item.getQty())
                );
            }

            // 3. Update customer points
            if (invoiceDto.isWithdrawPoints()) {
                customerRepository.updateCustomerPoints(
                    invoiceDto.getCustomerMobile(),
                    invoiceDto.getNewPoints()
                );
            } else {
                double points = calculatePoints(invoiceDto.getPaidAmount().doubleValue());
                customerRepository.addCustomerPoints(
                    invoiceDto.getCustomerMobile(),
                    points
                );
            }

            // Commit transaction - all operations successful
            conn.commit();
            logger.info("Invoice saved successfully with transaction: " + invoiceDto.getId());
            
        } catch (Exception e) {
            // Rollback transaction on any error
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warning("Transaction rolled back for invoice: " + invoiceDto.getId());
                } catch (SQLException rollbackEx) {
                    logger.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
                }
            }
            logger.log(Level.SEVERE, "Error saving invoice: " + invoiceDto.getId(), e);
            throw new Exception("Failed to save invoice - transaction rolled back", e);
        } finally {
            // Close connection
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    logger.log(Level.WARNING, "Failed to close connection", closeEx);
                }
            }
        }
    }

    @Override
    public Map<String, String> getPaymentMethods() throws Exception {
        return paymentMethodRepository.findAllPaymentMethods();
    }

    @Override
    public double calculatePoints(double amount) {
        // 1 point per 100 currency units (adjust as needed)
        return Math.floor(amount / 100);
    }
}
