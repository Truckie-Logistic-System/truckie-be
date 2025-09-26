package capstone_project.common.utils;

import capstone_project.dtos.request.user.AddressRequest;
import capstone_project.dtos.response.user.GeocodingResponse;
import capstone_project.dtos.response.user.AddressResponse;
import capstone_project.entity.user.address.AddressEntity;

import java.math.BigDecimal;
import java.util.Optional;

public class AddressUtil {

    private static final String ADDRESS_SEPARATOR = ", ";
    private static final BigDecimal DEFAULT_LATITUDE = new BigDecimal("21.0285");
    private static final BigDecimal DEFAULT_LONGITUDE = new BigDecimal("105.8542");

    public static String buildFullAddress(AddressRequest request) {
        String street = Optional.ofNullable(request.street()).orElse("").trim();
        String ward = Optional.ofNullable(request.ward()).orElse("").trim();
        String province = Optional.ofNullable(request.province()).orElse("").trim();

        java.util.List<String> parts = new java.util.ArrayList<>();
        if (!street.isEmpty()) parts.add(street);
        if (!ward.isEmpty()) parts.add(ward + " ward");
        if (!province.isEmpty()) parts.add(province + " city");

        return String.join(ADDRESS_SEPARATOR, parts);
    }

    public static AddressResponse buildResponseFromGeocoding(GeocodingResponse response) {
        return new AddressResponse(
                null,
                response.province(),
                response.ward(),
                response.street(),
                null, // addressType
                response.latitude(),
                response.longitude(),
                null
        );
    }

    public static AddressResponse buildFallbackResponse(String address) {
        String[] parts = address.split(ADDRESS_SEPARATOR);
        String street = parts.length > 0 ? parts[0].trim() : "";
        String ward = parts.length > 1 ? parts[1].trim() : "";
        String province = parts.length > 2 ? parts[2].trim() : "";

        return new AddressResponse(
                null,
                province,
                ward,
                street,
                null, // addressType
                DEFAULT_LATITUDE,
                DEFAULT_LONGITUDE,
                null
        );
    }

    public static void setCoordinatesOnEntity(AddressEntity entity, BigDecimal latitude, BigDecimal longitude) {
        entity.setLatitude(latitude);
        entity.setLongitude(longitude);
    }
}