package capstone_project.service.services.user;

import capstone_project.dtos.request.user.AddressRequest;
import capstone_project.dtos.response.user.AddressResponse;

import java.util.List;
import java.util.UUID;

public interface AddressService {
    List<AddressResponse> getAllAddresses();

    List<AddressResponse> getAddressesByCustomerId(UUID customerId);

    AddressResponse calculateLatLong(String address);

    AddressResponse createAddress(AddressRequest request);

    AddressResponse updateAddress(UUID id,AddressRequest request);

    AddressResponse getAddressById(UUID id);






}
