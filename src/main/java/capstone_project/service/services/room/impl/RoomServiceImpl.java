package capstone_project.service.services.room.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.enums.RoomEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.dtos.request.room.CreateRoomRequest;
import capstone_project.dtos.response.room.CreateRoomResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.chat.ParticipantInfo;
import capstone_project.entity.chat.RoomEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.service.mapper.room.RoomMapper;
import capstone_project.service.services.room.RoomService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {
    private final Firestore firestore;
    private final RoomMapper roomMapper;
    private final UserEntityService userEntityService;
    private final OrderEntityService orderEntityService;


    @Override
    @Transactional
    public CreateRoomResponse createRoom(CreateRoomRequest request) {
        if(request == null){
            log.warn("Input not found");
            throw new BadRequestException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        Optional<OrderEntity> getOrder = orderEntityService.findEntityById(UUID.fromString(request.orderId()));
//        if(getOrder.isEmpty() || !getOrder.get().getStatus().equals(OrderStatusEnum.SEALED_COMPLETED.name())){
//            log.warn("Order not found or Order's status is not SEALED_COMPLETED");
//            throw new BadRequestException(
//                    ErrorEnum.NOT_FOUND.getMessage() + "Không thể tạo chat room với đơn hàng này bởi vì đơn hàng này chưa có status “SEALED_COMPLETED”",
//                    ErrorEnum.NOT_FOUND.getErrorCode()
//            );
//        }


        List<UUID> uuidList = request.userIds().stream()
                .map(UUID::fromString).toList();
        Map<UUID, UserEntity> userMap = userEntityService.findAllByIdIn(uuidList)
                .stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        List<ParticipantInfo> participantInfoList = new ArrayList<>();
        for(String userId : request.userIds()) {
            ParticipantInfo participantInfo = new ParticipantInfo();
            participantInfo.setUserId(userId);
            UserEntity user = userMap.get(UUID.fromString(userId));
            if (user != null) {
                participantInfo.setRoleName(user.getRole().getRoleName());
            }
            participantInfoList.add(participantInfo);
        }

        CollectionReference rooms = firestore.collection("Rooms");
        DocumentReference roomDoc = rooms.document();
        RoomEntity room = RoomEntity.builder()
                .roomId(roomDoc.getId())
                .orderId(request.orderId())
                .status(CommonStatusEnum.ACTIVE.name())
                .type(RoomEnum.PRIVATE.name())
                .participants(participantInfoList)
                .build();
        try {
            roomDoc.set(room);
            log.info("Room created with id: {}", room.getRoomId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create room in Firestore", e);
        }
        return roomMapper.toCreateRoomResponse(room);
    }

    @Override
    public boolean cancelRoomByOrderId(UUID orderId) {
        if(orderId == null){
            log.warn("orderId not found");
            throw new BadRequestException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        try {
            CollectionReference rooms = firestore.collection("Rooms");
            Query query = rooms.whereEqualTo("orderId", orderId.toString());
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            if (documents.isEmpty()) {
                throw new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                );
            }
            for (QueryDocumentSnapshot doc : documents) {
                doc.getReference().update("status", CommonStatusEnum.INACTIVE.name());
                log.info("Room [{}] status set to UN_ACTIVE", doc.getId());
            }
            return true;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch room by orderId from Firestore", e);
        }
    }

    @Override
    public List<CreateRoomResponse> listRoomActiveByUserId(UUID userId) {
        if(userId == null){
            log.warn("userId not found");
            throw new BadRequestException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        List<RoomEntity> result = new ArrayList<>();
        try {
            CollectionReference rooms = firestore.collection("Rooms");
            Query query = rooms.whereEqualTo("status", CommonStatusEnum.ACTIVE.name());
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

            for (QueryDocumentSnapshot doc : documents) {
                RoomEntity room = doc.toObject(RoomEntity.class);
                boolean hasUser = room.getParticipants().stream()
                        .anyMatch(p -> p.getUserId().equals(userId.toString()));
                if (hasUser) {
                    result.add(room);
                }
            }
            return roomMapper.toCreateRoomResponseList(result);
        }catch (Exception e){
            throw new RuntimeException("Failed to fetch room by orderId from Firestore", e);
        }
    }

    @Override
    public boolean activeRoomByOrderId(UUID orderId) {
        if(orderId == null){
            log.warn("orderId not found");
            throw new BadRequestException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        try {
            CollectionReference rooms = firestore.collection("Rooms");
            Query query = rooms.whereEqualTo("orderId", orderId.toString());
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            if (documents.isEmpty()) {
                throw new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                );
            }
            for (QueryDocumentSnapshot doc : documents) {
                doc.getReference().update("status", CommonStatusEnum.ACTIVE.name());
                log.info("Room [{}] status set to ACTIVE", doc.getId());
            }
            return true;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch room by orderId from Firestore", e);
        }
    }

    @Override
    public boolean deleteRoomByOrderId(UUID orderId) {
        return false;
    }
}
