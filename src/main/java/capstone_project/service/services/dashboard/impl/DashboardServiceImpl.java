package capstone_project.service.services.dashboard.impl;

import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.dtos.response.dashboard.*;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.transaction.TransactionEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.service.services.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import static capstone_project.common.enums.ErrorEnum.NOT_FOUND;
import static capstone_project.common.enums.ErrorEnum.NULL;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final OrderEntityService orderEntityService;
    private final ContractEntityService contractEntityService;
    private final UserEntityService userEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final CustomerEntityService customerEntityService;
    private final DriverEntityService driverEntityService;
    private final TransactionEntityService transactionEntityService;

    @Override
    public int countAllOrder() {

        return orderEntityService.countAllOrderEntities();
    }

    @Override
    public int countOrderEntitiesBySenderId(UUID senderId) {

        return orderEntityService.countOrderEntitiesBySenderId(senderId);
    }

    @Override
    public int countOrderEntitiesBySenderCompanyNameContainingIgnoreCase(String senderCompanyName) {

        return orderEntityService.countOrderEntitiesByReceiverNameContainingIgnoreCase(senderCompanyName);
    }

    @Override
    public int countOrderEntitiesByReceiverNameContainingIgnoreCase(String receiverName) {

        return orderEntityService.countOrderEntitiesByReceiverNameContainingIgnoreCase(receiverName);
    }

    @Override
    public List<MonthlyOrderCount> countTotalOrderByMonthOverYear(int year) {

        List<Object[]> results = orderEntityService.countTotalOrderByMonthOverYear(year);

        if (results.isEmpty()) {
            
            throw new BadRequestException(NULL.getMessage(), NULL.getErrorCode());
        }

        results.sort(Comparator.comparing(row -> ((Number) row[0]).intValue()));

        return results.stream()
                .map(row -> new MonthlyOrderCount(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue()
                ))
                .toList();
    }

    @Override
    public Map<String, Long> countAllByOrderStatus() {

        List<Object[]> results = orderEntityService.countAllByOrderStatus();
        if (results.isEmpty()) {
            
            throw new BadRequestException(NOT_FOUND.getMessage(),
                    NOT_FOUND.getErrorCode());
        }

        Map<String, Long> statusCounts = new HashMap<>();

        for (Object[] row : results) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();

            statusCounts.put(status, count);
        }

        return statusCounts;
    }

    @Override
    public Map<String, Long> countByOrderStatus(String status) {

        List<Object[]> results = orderEntityService.countByOrderStatus(status);
        if (results.isEmpty()) {
            
            throw new BadRequestException(NOT_FOUND.getMessage(),
                    NOT_FOUND.getErrorCode());
        }

        Map<String, Long> statusCounts = new HashMap<>();

        for (Object[] row : results) {
            status = (String) row[0];
            long count = ((Number) row[1]).longValue();

            statusCounts.put(status, count);
        }

        return statusCounts;
    }

    @Override
    public Map<String, Long> countOrderByWeek(int amount) {

        List<Object[]> results = orderEntityService.countOrderByWeek(amount);
        if (results.isEmpty()) {
            
            throw new BadRequestException(NOT_FOUND.getMessage(),
                    NOT_FOUND.getErrorCode());
        }

        Map<String, Long> statusCounts = new HashMap<>();

        String week = "Week";

        for (Object[] row : results) {
            week = (String) row[0];
            long count = ((Number) row[1]).longValue();

            statusCounts.put(week + " " + amount, count);
        }

        return statusCounts;
    }

    @Override
    public Map<String, Long> countOrderByYear(int amount) {

        List<Object[]> results = orderEntityService.countOrderByYear(amount);
        if (results.isEmpty()) {
            
            throw new BadRequestException(NOT_FOUND.getMessage(),
                    NOT_FOUND.getErrorCode());
        }

        Map<String, Long> statusCounts = new HashMap<>();

        String year = "Year";
        ;

        for (Object[] row : results) {
            year = (String) row[0];
            long count = ((Number) row[1]).longValue();

            statusCounts.put(year + " " + amount, count);
        }

        return statusCounts;
    }

    @Override
    public Map<String, Long> countAllByUserStatus() {

        List<Object[]> results = userEntityService.countAllByUserStatus();
        if (results.isEmpty()) {
            log.error("[countAllByUserStatus] nums: 0");
            throw new BadRequestException(NOT_FOUND.getMessage(),
                    NOT_FOUND.getErrorCode());
        }

        Map<String, Long> statusCounts = new HashMap<>();

        for (Object[] row : results) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();

            statusCounts.put(status, count);
        }

        return statusCounts;
    }

    @Override
    public Map<String, Long> countUsersByRole() {

        List<Object[]> results = userEntityService.countUsersByRole();
        if (results.isEmpty()) {
            log.error("[countUsersByRole] nums: 0");
            throw new BadRequestException(NOT_FOUND.getMessage(),
                    NOT_FOUND.getErrorCode());
        }

        Map<String, Long> roleCounts = new HashMap<>();

        for (Object[] row : results) {
            String role = (String) row[0];
            long count = ((Number) row[1]).longValue();

            roleCounts.put(role, count);
        }

        return roleCounts;
    }

    @Override
    public Integer countAllUsers() {

        return userEntityService.countAllUsers();
    }

    @Override
    public List<MonthlyNewCustomerCountResponse> newCustomerByMonthOverYear(int year) {

        List<Object[]> results = customerEntityService.newCustomerByMonthOverYear(year);

        if (results.isEmpty()) {
            
            throw new BadRequestException(NULL.getMessage(), NULL.getErrorCode());
        }

        results.sort(Comparator.comparing(row -> ((Number) row[0]).intValue()));

        return results.stream()
                .map(row -> new MonthlyNewCustomerCountResponse(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    @Override
    public List<CustomerGrowthRateByYearResponse> getCustomerGrowthRateByYear(int year) {

        List<Object[]> results = customerEntityService.getUserGrowthRateByYear(year);

        if (results.isEmpty()) {
            
            throw new BadRequestException(NULL.getMessage(), NULL.getErrorCode());
        }

        results.sort(Comparator.comparing(row -> ((Number) row[1]).intValue()));

        return results.stream()
                .map(row -> new CustomerGrowthRateByYearResponse(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue(),
                        ((Number) row[3]).intValue(),
                        row[3] != null ? ((Number) row[3]).doubleValue() : 0.0
                ))
                .toList();
    }

    @Override
    public List<TopSenderResponse> topSenderByMonthAndYear(Integer month, Integer year, int amount) {

        if (amount <= 0) {
            log.error("[topSenderByMonthAndYear] Invalid amount: {}", amount);
            throw new BadRequestException(NULL.getMessage(), NULL.getErrorCode());
        }

        List<Object[]> results = orderEntityService.topSenderByMonthAndYear(month, year, amount);

        if (results.isEmpty()) {
            log.warn("[topSenderByMonthAndYear] No senders found for month: {}, year: {}", month, year);
            throw new BadRequestException(NOT_FOUND.getMessage(), NOT_FOUND.getErrorCode());
        }

        List<TopSenderResponse> topSenders = new ArrayList<>();

        for (Object[] row : results) {
            UUID customerId = UUID.fromString(row[0].toString());
            String companyName = (String) row[1];
            int orderCount = ((Number) row[2]).intValue();
            int rank = ((Number) row[3]).intValue();

            topSenders.add(new TopSenderResponse(customerId.toString(), companyName, orderCount, rank));
        }

        return topSenders;
    }

    @Override
    public List<TopDriverResponse> topDriverByMonthAndYear(Integer month, Integer year, int amount) {

        if (amount <= 0) {
            log.error("[topDriverByMonthAndYear] Invalid amount: {}", amount);
            throw new BadRequestException(NULL.getMessage(), NULL.getErrorCode());
        }

        List<Object[]> results = orderEntityService.topDriverByMonthAndYear(month, year, amount);

        if (results.isEmpty()) {
            log.warn("[topDriverByMonthAndYear] No drivers found for month: {}, year: {}", month, year);
            throw new BadRequestException(NOT_FOUND.getMessage(), NOT_FOUND.getErrorCode());
        }

        List<TopDriverResponse> topDrivers = new ArrayList<>();

        for (Object[] row : results) {
            UUID driverId = UUID.fromString(row[0].toString());
            String driverName = (String) row[1];
            int tripCount = ((Number) row[2]).intValue();
            int rank = ((Number) row[3]).intValue();

            topDrivers.add(new TopDriverResponse(driverId.toString(), driverName, tripCount, rank));
        }

        return topDrivers;
    }

    @Override
    public OnTImeVSLateDeliveriesResponse getOnTimeVsLateDeliveriesWithPercentage(Integer month, Integer year) {

        List<Object[]> result = orderDetailEntityService.getOnTimeVsLateDeliveriesWithPercentage(month, year);

        if (result.isEmpty()) {
            log.warn("[getOnTimeVsLateDeliveriesWithPercentage] No delivery data found for month: {}, year: {}", month, year);
            throw new BadRequestException(NOT_FOUND.getMessage(), NOT_FOUND.getErrorCode());
        }

        Object[] row = result.get(0);

        int onTimeCount = ((Number) row[0]).intValue();
        int lateCount = ((Number) row[1]).intValue();
        double onTimePercentage = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
        double latePercentage = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;

        return new OnTImeVSLateDeliveriesResponse(onTimeCount, lateCount, onTimePercentage, latePercentage);
    }

    @Override
    public List<OnTimeDeliveriesDriverResponse> topOnTimeDeliveriesByDriversWithPercentage(Integer month, Integer year, int amount) {

        List<Object[]> results = orderDetailEntityService.topOnTimeDeliveriesByDriversWithPercentage(month, year, amount);

        if (results.isEmpty()) {
            log.warn("[topOnTimeDeliveriesByDriversWithPercentage] No driver data found for month: {}, year: {}", month, year);
            throw new BadRequestException(NOT_FOUND.getMessage(), NOT_FOUND.getErrorCode());
        }

        List<OnTimeDeliveriesDriverResponse> topDrivers = new ArrayList<>();

        for (Object[] row : results) {
            UUID driverId = UUID.fromString(row[0].toString());
            String driverName = (String) row[1];
            int totalDeliveries = ((Number) row[2]).intValue();
            int onTimeDeliveries = ((Number) row[3]).intValue();
            double onTimePercentage = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;

            topDrivers.add(new OnTimeDeliveriesDriverResponse(driverId.toString(), driverName, totalDeliveries, onTimeDeliveries, onTimePercentage));
        }
        return topDrivers;
    }

    @Override
    public List<LateDeliveriesDriverResponse> topLateDeliveriesByDriversWithPercentage(Integer month, Integer year, int amount) {

        List<Object[]> results = orderDetailEntityService.topLateDeliveriesByDriversWithPercentage(month, year, amount);

        if (results.isEmpty()) {
            log.warn("[topLateDeliveriesByDriversWithPercentage] No driver data found for month: {}, year: {}", month, year);
            throw new BadRequestException(NOT_FOUND.getMessage(), NOT_FOUND.getErrorCode());
        }

        List<LateDeliveriesDriverResponse> topDrivers = new ArrayList<>();

        for (Object[] row : results) {
            UUID driverId = UUID.fromString(row[0].toString());
            String driverName = (String) row[1];
            int totalDeliveries = ((Number) row[2]).intValue();
            int lateDeliveries = ((Number) row[3]).intValue();
            double latePercentage = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;

            topDrivers.add(new LateDeliveriesDriverResponse(driverId.toString(), driverName, totalDeliveries, lateDeliveries, latePercentage));
        }
        return topDrivers;
    }

    @Override
    public BigDecimal getTotalRevenueInYear() {

        return transactionEntityService.getTotalRevenueInYear();
    }

    @Override
    public Map<Integer, Long> getTotalRevenueCompareYear() {

        List<Object[]> results = transactionEntityService.getTotalRevenueCompareYear();

        if (results.isEmpty()) {
            
            throw new BadRequestException(NULL.getMessage(), NULL.getErrorCode());
        }

        Map<Integer, Long> revenueByYear = new HashMap<>();

        for (Object[] row : results) {
            Integer year = ((Number) row[0]).intValue();
            Long total = ((Number) row[1]).longValue();
            revenueByYear.put(year, total);
        }
        return revenueByYear;
    }

    @Override
    public Map<Integer, Long> getTotalRevenueByMonth() {

        List<Object[]> results = transactionEntityService.getTotalRevenueByMonth();

        if (results.isEmpty()) {
            
            throw new BadRequestException(NULL.getMessage(), NULL.getErrorCode());
        }

        Map<Integer, Long> revenueByMonth = new HashMap<>();

        for (Object[] row : results) {
            Integer month = ((Number) row[0]).intValue();
            Long total = ((Number) row[1]).longValue();
            revenueByMonth.put(month, total);
        }
        return revenueByMonth;
    }

    @Override
    public Map<Integer, Long> getTotalRevenueByLast4Weeks() {

        List<Object[]> results = transactionEntityService.getTotalRevenueByLast4Weeks();

        if (results.isEmpty()) {
            
            throw new BadRequestException(NULL.getMessage(), NULL.getErrorCode());
        }

        Map<Integer, Long> revenueByWeek = new HashMap<>();

        for (Object[] row : results) {
            String weekLabel = (String) row[0];
            Integer weekNumber = Integer.parseInt(weekLabel.split("-")[1]);
            Long total = ((Number) row[1]).longValue();
            revenueByWeek.put(weekNumber, total);
        }
        return revenueByWeek;
    }

    @Override
    public List<TopPayCustomerResponse> getTopCustomersByRevenue(int amount) {

        if (amount <= 0) {
            log.error("[getTopCustomersByRevenue] Invalid amount: {}", amount);
            throw new BadRequestException(NULL.getMessage(), NULL.getErrorCode());
        }

        List<Object[]> results = customerEntityService.getTopCustomersByRevenue(amount);

        if (results.isEmpty()) {
            log.warn("[getTopCustomersByRevenue] No customer data found");
            throw new BadRequestException(NOT_FOUND.getMessage(), NOT_FOUND.getErrorCode());
        }

        List<TopPayCustomerResponse> topCustomers = new ArrayList<>();

        for (Object[] row : results) {
            UUID customerId = UUID.fromString(row[0].toString());
            String customerName = (String) row[1];
            String companyName = (String) row[2];
            BigDecimal totalRevenue = (BigDecimal) row[3];

            topCustomers.add(new TopPayCustomerResponse(customerId.toString(), customerName, companyName, totalRevenue));
        }

        return topCustomers;
    }

}
