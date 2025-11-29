package capstone_project.service.events.payment;

import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.transaction.TransactionEntity;

/**
 * Event fired when a full payment is successfully processed
 */
public class FullPaymentPaidEvent {
    private final OrderEntity order;
    private final ContractEntity contract;
    private final TransactionEntity transaction;
    private final double totalAmount;

    public FullPaymentPaidEvent(OrderEntity order, ContractEntity contract, TransactionEntity transaction, double totalAmount) {
        this.order = order;
        this.contract = contract;
        this.transaction = transaction;
        this.totalAmount = totalAmount;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public ContractEntity getContract() {
        return contract;
    }

    public TransactionEntity getTransaction() {
        return transaction;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    @Override
    public String toString() {
        return "FullPaymentPaidEvent{" +
                "orderCode=" + (order != null ? order.getOrderCode() : "null") +
                ", contractId=" + (contract != null ? contract.getId() : "null") +
                ", transactionId=" + (transaction != null ? transaction.getId() : "null") +
                ", totalAmount=" + totalAmount +
                '}';
    }
}
