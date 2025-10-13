package capstone_project.entity.setting;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "stipulation_settings", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StipulationSettingEntity extends BaseEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contents", columnDefinition = "jsonb")
    private Map<String, String> contents;

}
