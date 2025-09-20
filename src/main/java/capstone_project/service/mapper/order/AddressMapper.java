package capstone_project.service.mapper.order;

import capstone_project.dtos.response.order.AddressResponse;
import capstone_project.entity.user.address.AddressEntity;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    /**
     * Map an AddressEntity to AddressResponse
     *
     * @param entity AddressEntity to map
     * @return AddressResponse
     */
    public AddressResponse toDto(AddressEntity entity) {
        if (entity == null) {
            return null;
        }

        // Build full address string
        StringBuilder fullAddress = new StringBuilder();
        if (entity.getStreet() != null && !entity.getStreet().isEmpty()) {
            fullAddress.append(entity.getStreet());
        }

        if (entity.getWard() != null && !entity.getWard().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(entity.getWard());
        }

        if (entity.getProvince() != null && !entity.getProvince().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(entity.getProvince());
        }

        return AddressResponse.builder()
                .id(entity.getId())
                .street(entity.getStreet())
                .ward(entity.getWard())
                .province(entity.getProvince())
                .addressType(entity.getAddressType())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .fullAddress(fullAddress.toString())
                .build();
    }
}
