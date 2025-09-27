package capstone_project.common.utils;

public final class GeoUtils {
    /**
     * Bán kính trung bình của Trái Đất tính theo kilômét.
     *
     * <p>Hằng số này dùng để chuyển góc tâm (radian) thu được từ công thức Haversine
     * thành khoảng cách tuyến tính: khoảng_cách = EARTH_RADIUS_KM * góc_tâm</p>
     */
    private static final double EARTH_RADIUS_KM = 6371.0088; // mean Earth radius

    private GeoUtils() {}

    public static double calculateDistanceKmBetweenTwoPlaces(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        // Thành phần của công thức Haversine:
        // a = sin^2(dLat/2) + cos(lat1) * cos(lat2) * sin^2(dLon/2)
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        // c là góc giữa hai điểm trên mặt cầu (radian)
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        // Khoảng cách = bán kính Trái Đất * góc tâm
        return EARTH_RADIUS_KM * c;
    }
}

