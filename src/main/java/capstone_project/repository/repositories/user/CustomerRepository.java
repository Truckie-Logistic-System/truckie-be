package capstone_project.repository.repositories.user;

import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends BaseRepository<CustomerEntity> {
    Optional<CustomerEntity> findByUserId(UUID id);

    List<CustomerEntity> findByCompanyNameContainingIgnoreCase(String companyName);

    List<CustomerEntity> findByRepresentativeNameContainingIgnoreCase(String name);

    List<CustomerEntity> findByUser_Role_RoleName(String userRoleRoleName);

    @Query(value = """
            SELECT EXTRACT(MONTH FROM c.created_at) AS month, COUNT(*) AS count
            FROM customers c
            WHERE EXTRACT(YEAR FROM c.created_at) = ?1
            GROUP BY month
            ORDER BY month;
            """, nativeQuery = true)
    List<Object[]> newCustomerByMonthOverYear(@Param("year") int year);

    @Query(value = """
            WITH monthly_counts AS (
                SELECT
                    EXTRACT(YEAR FROM created_at) AS year,
                    EXTRACT(MONTH FROM created_at) AS month,
                    COUNT(*) AS new_users
                FROM customers
                WHERE EXTRACT(YEAR FROM created_at) = :year
                GROUP BY 1, 2
            )
            SELECT
                year,
                month,
                new_users,
                SUM(new_users) OVER (PARTITION BY year ORDER BY month) AS cumulative_users,
                ROUND(
                        (new_users - LAG(new_users) OVER (PARTITION BY year ORDER BY month))
                            / NULLIF(LAG(new_users) OVER (PARTITION BY year ORDER BY month), 0) * 100,
                        2
                ) AS growth_rate
            FROM monthly_counts
            ORDER BY month;
            """, nativeQuery = true)
    List<Object[]> getCustomerGrowthRateByYear(@Param("year") int year);

    @Query(value = """
                SELECT c2.id              AS customerId,
                       u.full_name                AS customerName,
                       c2.company_name  AS companyName,
                       COALESCE(SUM(t.amount), 0) AS totalRevenue
                FROM transaction t
                         JOIN contracts c ON t.contract_id = c.id
                         JOIN orders o ON c.order_id = o.id
                         JOIN customers c2 ON o.customer_id = c2.id
                         JOIN users u ON c2.user_id = u.id
                WHERE t.status = 'PAID'
                GROUP BY c2.id, u.full_name, c2.company_name
                ORDER BY totalRevenue DESC
                LIMIT :amount;
            """, nativeQuery = true)
    List<Object[]> getTopCustomersByRevenue(int amount);

}
