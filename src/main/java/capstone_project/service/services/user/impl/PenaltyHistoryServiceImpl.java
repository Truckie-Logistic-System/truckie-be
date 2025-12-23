package capstone_project.service.services.user.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.user.PenaltyHistoryRequest;
import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.repository.entityServices.user.PenaltyHistoryEntityService;
import capstone_project.repository.repositories.user.PenaltyHistoryRepository;
import capstone_project.service.mapper.user.PenaltyHistoryMapper;
import capstone_project.service.services.user.PenaltyHistoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PenaltyHistoryServiceImpl implements PenaltyHistoryService {

    private final PenaltyHistoryEntityService entityService;
    private final PenaltyHistoryRepository penaltyHistoryRepository;
    private final PenaltyHistoryMapper mapper;

    @Override
    public List<PenaltyHistoryResponse> getAll() {
        List<PenaltyHistoryEntity> list = entityService.findAll();
        return list.stream()
                .map(mapper::toPenaltyHistoryResponse)
                .toList();
    }

    @Override
    public PenaltyHistoryResponse getById(UUID id) {
        PenaltyHistoryEntity e = penaltyHistoryRepository.findDetailById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
        return mapper.toPenaltyHistoryResponse(e);
    }

    
    @Override
    public List<PenaltyHistoryResponse> getByDriverId(UUID driverId) {
        List<PenaltyHistoryEntity> penalties = entityService.findByDriverId(driverId);
        return penalties.stream()
                .map(mapper::toPenaltyHistoryResponse)
                .toList();
    }

    @Override
    public List<String> getTrafficViolationReasons() {
        return List.of(
            "Vượt tốc độ",
            "Đỗ sai quy định",
            "Vi phạm tín hiệu giao thông",
            "Không đủ giấy tờ xe",
            "Quá tải",
            "Đi sai tuyến đường",
            "Vi phạm thời gian lái xe",
            "Vi phạm điều kiện phương tiện",
            "Khác"
        );
    }
}
