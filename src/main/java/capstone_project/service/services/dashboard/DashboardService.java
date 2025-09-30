package capstone_project.service.services.dashboard;

import capstone_project.dtos.response.dashboard.MonthlyOrderCount;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DashboardService {
    int countAllOrder();

    int countOrderEntitiesBySenderId(UUID senderId);

    int countOrderEntitiesBySenderCompanyNameContainingIgnoreCase(String senderCompanyName);

    int countOrderEntitiesByReceiverNameContainingIgnoreCase(String receiverName);

    List<MonthlyOrderCount> countTotalOrderByMonthOverYear(int year);

    Map<String, Long> countAllByOrderStatus();

    Map<String, Long> countByOrderStatus(String status);

    Map<String, Long> countOrderByWeek(int amount);

    Map<String, Long> countOrderByYear(int amount);

    Map<String, Long> countAllByUserStatus();

    Integer countAllUsers();

}
