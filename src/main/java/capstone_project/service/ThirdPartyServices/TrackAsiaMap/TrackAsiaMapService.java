package capstone_project.service.ThirdPartyServices.TrackAsiaMap;

import capstone_project.dtos.response.trackasiamap.TrackAsiaSearchResponse;

public interface TrackAsiaMapService {
    TrackAsiaSearchResponse search(String query);
}
