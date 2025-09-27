package capstone_project.service.ThirdPartyServices.TrackAsia;

import capstone_project.dtos.response.trackasia.AutoComplete.TrackAsiaAutocompleteResponse;
import capstone_project.dtos.response.trackasia.PlaceDetails.TrackAsiaPlaceDetailsResponse;
import capstone_project.dtos.response.trackasia.ReverseGeocoding.TrackAsiaReverseGeocodeResponse;
import capstone_project.dtos.response.trackasia.Search.TrackAsiaSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;

public interface TrackAsiaService {
    // Search for places using TrackAsiaMap API
    TrackAsiaSearchResponse search(String query);

    // Autocomplete method - parameters optional (pass null to skip)
    TrackAsiaAutocompleteResponse autocomplete(String input,
                                               Integer size,
                                               String bounds,
                                               String location,
                                               Boolean newAdmin,
                                               Boolean includeOldAdmin);

    // Reverse geocode method - parameters optional (pass null to skip)
    TrackAsiaReverseGeocodeResponse reverseGeocode(String latlng,
                                                   Integer radius,
                                                   Boolean newAdmin,
                                                   Boolean includeOldAdmin);

    // Get place details by place ID - parameters optional (pass null to skip)
    TrackAsiaPlaceDetailsResponse placeDetails(String placeId, String outputFormat, Boolean newAdmin, Boolean includeOldAdmin);

    // Get directions between origin and destination - parameters optional (pass null to skip)
    JsonNode directions(String origin, String destination, String mode, String outputFormat, Boolean newAdmin);
}
