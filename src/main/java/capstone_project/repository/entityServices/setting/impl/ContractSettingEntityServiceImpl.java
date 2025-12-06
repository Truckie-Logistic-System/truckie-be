package capstone_project.repository.entityServices.setting.impl;

import capstone_project.entity.setting.ContractSettingEntity;
import capstone_project.repository.entityServices.setting.ContractSettingEntityService;
import capstone_project.repository.repositories.setting.ContractSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractSettingEntityServiceImpl implements ContractSettingEntityService {

    private final ContractSettingRepository contractSettingRepository;

    @Override
    public ContractSettingEntity save(ContractSettingEntity newSetting) {
        return contractSettingRepository.findFirstByOrderByCreatedAtAsc()
                .map(existing -> {
                    // update
                    existing.setDepositPercent(newSetting.getDepositPercent());
                    existing.setDepositDeadlineHours(newSetting.getDepositDeadlineHours());
                    existing.setSigningDeadlineHours(newSetting.getSigningDeadlineHours());
                    existing.setFullPaymentDaysBeforePickup(newSetting.getFullPaymentDaysBeforePickup());
                    existing.setInsuranceRateNormal(newSetting.getInsuranceRateNormal());
                    existing.setInsuranceRateFragile(newSetting.getInsuranceRateFragile());
                    existing.setVatRate(newSetting.getVatRate());
                    return contractSettingRepository.save(existing);
                })
                .orElseGet(() -> contractSettingRepository.save(newSetting));
    }

    @Override
    public Optional<ContractSettingEntity> findEntityById(UUID uuid) {
        return contractSettingRepository.findById(uuid);
    }

    @Override
    public List<ContractSettingEntity> findAll() {
        return contractSettingRepository.findAll();
    }

    @Override
    public Optional<ContractSettingEntity> findFirstByOrderByCreatedAtAsc() {
        return contractSettingRepository.findFirstByOrderByCreatedAtAsc();
    }
}
