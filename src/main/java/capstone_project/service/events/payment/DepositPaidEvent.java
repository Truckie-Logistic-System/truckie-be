package capstone_project.service.events.payment;

import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.transaction.TransactionEntity;

/**
 * Event fired when a deposit payment is successfully processed
 */
public class DepositPaidEvent {
    private final OrderEntity order;
    private final ContractEntity contract;
    private final TransactionEntity transaction;
    private final double depositAmount;
    private final double totalAmount;

    public DepositPaidEvent(OrderEntity order, ContractEntity contract, TransactionEntity transaction, double depositAmount, double totalAmount) {
        this.order = order;
        this.contract = contract;
        this.transaction = transaction;
        this.depositAmount = depositAmount;
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

    public double getDepositAmount() {
        return depositAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    @Override
    public String toString() {
        return "DepositPaidEvent{" +
                "orderCode=" + (order != null ? order.getOrderCode() : "null") +
                ", contractId=" + (contract != null ? contract.getId() : "null") +
                ", transactionId=" + (transaction != null ? transaction.getId() : "null") +
                ", depositAmount=" + depositAmount +
                '}';
    }
}
