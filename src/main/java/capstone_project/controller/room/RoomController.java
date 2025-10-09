package capstone_project.controller.room;

import capstone_project.dtos.request.room.CreateRoomRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.room.CreateRoomResponse;
import capstone_project.service.services.room.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${room.api.base-path}")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;



    /**
    Api này là create room, trong create room này sẽ có 2 trường hợp để CREAT ROOM CHAT
    TH1: Customer cần tư vấn
    Là customer chỉ cần click vào button Chat là Room chat sẽ được tạo với type là “SUPPORT” và đợi staff join room và
     reply (có api join room dành cho staff ở phía dưới để join vào những trường hợp room type là SUPPORT)
    TH2: Tạo room sau khi order đó có status là ON_PLANNING
    Là nó sẽ tự động lấy orderID được truyền vào để tìm contract và kiểm tra status của Order => sau đó thì nó sẽ lấy CusID ở Order và StaffId ở Contract để
     tạo listUsers cho room chat đó => đây là trường hợp tạo room cho customer có order, Room này được tạo với type là ORDER_TYPE
     */
    @PostMapping("")
    public ResponseEntity<ApiResponse<CreateRoomResponse>> createRoom(@RequestBody @Valid CreateRoomRequest createRoomRequest) {
        final var createRoom = roomService.createRoom(createRoomRequest);
        return ResponseEntity.ok(ApiResponse.ok(createRoom));
    }

    /**
    Đây là api để lấy tất cả room dựa vào userID
     (nó sẽ response list Room và khi vào click vào room nào để xem message thì phải chạy thêm api getMessagesByRoomId bên ChatController)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<CreateRoomResponse>>> getAllRoomsByUserId(@PathVariable @Valid UUID userId) {
        final var getAllRoomsByUserId = roomService.listRoomActiveByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(getAllRoomsByUserId));
    }

    /**
     Đây là api chỉnh status của room chat thành active dựa vào orderId
     */
    @PutMapping("/active/{orderId}")
    public ResponseEntity<ApiResponse<Boolean>> activeRoomsByOrderId(@PathVariable @Valid UUID orderId) {
        final var activeRoomsByOrderId = roomService.activeRoomByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.ok(activeRoomsByOrderId));
    }

    /**
     Đây là api chỉnh status của room chat thành inactive dựa vào orderId
     */
    @PutMapping("/in-active/{orderId}")
    public ResponseEntity<ApiResponse<Boolean>> inactiveRoomsByOrderId(@PathVariable @Valid UUID orderId) {
        final var inactiveRoomsByOrderId = roomService.cancelRoomByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.ok(inactiveRoomsByOrderId));
    }

    /**
     Đây là api dành cho staff join room vào những Room type “SUPPORT” khi đã
     join vào thành công thì type của ROOM sẽ được update thành “SUPPORTED” và id của staff sẽ được thêm vào room
     */
    @PutMapping("/join/{roomId}/{staffId}")
    public ResponseEntity<ApiResponse<Boolean>> joinRoom(
            @PathVariable @Valid String roomId,
            @PathVariable @Valid UUID staffId) {

        final var joined = roomService.joinRoom(roomId, staffId);
        return ResponseEntity.ok(ApiResponse.ok(joined));
    }

    /**
     Đây là api get tất cả room có type là SUPPORT và SUPPORTED chỉ dành cho staff
     (nên sắp xếp những room có type SUPPORT lên đầu và highlight chúng trên UI để staff có thể join vào và reply customer)
     */
    @GetMapping("/get-list-room-support-for-staff")
    public ResponseEntity<ApiResponse<List<CreateRoomResponse>>> getRoomsSupportAndSupported() {
        final var rooms = roomService.getListSupportRoomsForStaff();
        return ResponseEntity.ok(ApiResponse.ok(rooms));
    }

    /**
     Đây là api check customer đó đã có room loại SUPPORT hoặc SUPPORTED hay chưa để biết tạo cho customer đó khi muốn customer cần tư vấn
     (thường là dùng api này để check trước khi create room cho customer nếu đã có rồi thì get room và message cho customer)
     */
    @GetMapping("customer/{userId}/has-supported-room")
    public ResponseEntity<ApiResponse<Boolean>> isCustomerHasRoomSupported(@PathVariable @Valid UUID userId) {
        final var hasRoom = roomService.isCustomerHasRoomSupported(userId);
        return ResponseEntity.ok(ApiResponse.ok(hasRoom));
    }

    @GetMapping("customer/{userId}/get-supported-room")
    public ResponseEntity<ApiResponse<CreateRoomResponse>> getCustomerHasRoomSupported(@PathVariable @Valid UUID userId) {
        final var hasRoom = roomService.getCustomerHasRoomSupported(userId);
        return ResponseEntity.ok(ApiResponse.ok(hasRoom));
    }
}
