package capstone_project.service.services.dashboard.impl;

import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.dtos.response.dashboard.MonthlyOrderCount;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.service.services.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final CustomerEntityService customerEntityService;
    private final DriverEntityService driverEntityService;

    @Override
    public int countAllOrder() {
        log.info("In DashboardServiceImpl.countAllOrder()");

        return orderEntityService.countAllOrderEntities();
    }

    @Override
    public int countOrderEntitiesBySenderId(UUID senderId) {
        log.info("In DashboardServiceImpl.countOrderEntitiesBySenderId()");

        return orderEntityService.countOrderEntitiesBySenderId(senderId);
    }

    @Override
    public int countOrderEntitiesBySenderCompanyNameContainingIgnoreCase(String senderCompanyName) {
        log.info("In DashboardServiceImpl.countOrderEntitiesBySenderCompanyName()");

        return orderEntityService.countOrderEntitiesByReceiverNameContainingIgnoreCase(senderCompanyName);
    }

    @Override
    public int countOrderEntitiesByReceiverNameContainingIgnoreCase(String receiverName) {
        log.info("In DashboardServiceImpl.countOrderEntitiesByReceiverName()");

        return orderEntityService.countOrderEntitiesByReceiverNameContainingIgnoreCase(receiverName);
    }

    @Override
    public List<MonthlyOrderCount> countTotalOrderByMonthOverYear(int year) {
        log.info("In DashboardServiceImpl.countTotalOrderByMonthOverYear()");

        List<Object[]> results = orderEntityService.countTotalOrderByMonthOverYear(year);

        if (results.isEmpty()) {
            log.info("[countTotalOrderByMonthOverYear] No orders found for year {}", year);
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
        log.info("In DashboardServiceImpl.countAllByBookingStatus()");

        List<Object[]> results = orderEntityService.countAllByOrderStatus();
        if (results.isEmpty()) {
            log.info("[countAllByBookingStatus] numOfBookings: 0");
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
        log.info("In DashboardServiceImpl.countByBookingStatus()");

        List<Object[]> results = orderEntityService.countByOrderStatus(status);
        if (results.isEmpty()) {
            log.info("[countByBookingStatus] numOfBookings: 0");
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
        log.info("In DashboardServiceImpl.countOrderByWeek()");

        List<Object[]> results = orderEntityService.countOrderByWeek(amount);
        if (results.isEmpty()) {
            log.info("[countOrderByWeek] numOfBookings: 0");
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
        log.info("In DashboardServiceImpl.countOrderByWeek()");

        List<Object[]> results = orderEntityService.countOrderByYear(amount);
        if (results.isEmpty()) {
            log.info("[countOrderByWeek] numOfBookings: 0");
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
        log.info("In DashboardServiceImpl.countAllByUserStatus()");

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
    public Integer countAllUsers() {
        log.info("In DashboardServiceImpl.countAllUsers()");

        return userEntityService.countAllUsers();
    }
}
