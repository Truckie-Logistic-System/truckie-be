package capstone_project.common.utils;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.dtos.response.order.contract.OrderDetailForPackingResponse;
import capstone_project.dtos.response.order.contract.PackedDetailResponse;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.entity.pricing.SizeRuleEntity;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class BinPacker {

    private static final boolean ALLOW_ROTATION = true;
    public static final int UNIT_MULTIPLIER = 10;

    // * đại diện cho 1 kiện hàng theo chuẩn hóa (dài rộng cao, trọng lượng)
    public static class BoxItem {

        public final UUID id;
        public final int lx, ly, lz;
        public final long weight;
        public final long volume;

        public BoxItem(UUID id, int lx, int ly, int lz, long weight) {
            this.id = id;
            this.lx = lx;
            this.ly = ly;
            this.lz = lz;
            this.weight = weight;
            this.volume = (long) lx * ly * lz;
        }
    }

    // * đại diện cho 1 vị trí đặt kiện hàng trong container
    /* x, y, z: tọa độ điểm của kiện trong container
     * lx, ly, lz: kích thước thực tế của kiện theo từng trục X, Y, Z
     * */
    public static class Placement {

        public int x;
        public int y;
        public int z;
        public int lx;
        public int ly;
        public int lz;
        public BoxItem box;

        Placement(BoxItem box, int x, int y, int z,
                  int lx, int ly, int lz) {
            this.box = box;
            this.x = x;
            this.y = y;
            this.z = z;
            this.lx = lx;
            this.ly = ly;
            this.lz = lz;
        }
    }

    // * đại diện cho trạng thái hiện tại của container (xe)

    /**
     * Kích thước xe (maxLength, maxWidth, maxHeight)
     * Trọng lượng hiện đang chở (currentWeight)
     * Các kiện hàng (Placement) đã được sắp xếp bên trong
     * Quy tắc xe (VehicleRuleEntity rule) — để tham chiếu đến các thông số khác
     * */
    public static class ContainerState {

        public final SizeRuleEntity rule;
        final int maxX, maxY, maxZ;
        public long currentWeight;
        public List<Placement> placements = new ArrayList<>();  // check kiện đã đặt -->
        List<int[]> extremePoints = new ArrayList<>(); // each point [x,y,z] - điểm cực trị để đặt kiện tiếp theo

        public ContainerState(SizeRuleEntity rule, int maxX, int maxY, int maxZ) {
            this.rule = rule;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.currentWeight = 0;
            // initial extreme point = origin (0,0,0)
            this.extremePoints.add(new int[]{0, 0, 0});
        }

        // * Sau khi thêm kiện  --> update lại trọng lượng --> kiểm tra trọng lượng có vượt quá giới hạn không
        boolean checkWeightAfterAdd(long addWeight) {
            BigDecimal maxW = rule.getMaxWeight() == null ? BigDecimal.valueOf(Long.MAX_VALUE) : rule.getMaxWeight();
            long maxWeightGram = Math.round(maxW.doubleValue() * 1000.0);
            return (currentWeight + addWeight) <= maxWeightGram;
        }

        // * Thêm kiện vào container và cập nhật các điểm cực trị mới

        /**
         * Trước khi đặt [(0,0,0)]
         * Đặt kiện có kích thước (lx,ly,lz // 4, 3, 2) tại (x,y,z)
         * 3 điểm cực trị mới được thêm vào:
         * - (lx,0,0)  //  (4,0,0)
         * - (0,ly,0)  //  (0,3,0)
         * - (0,0,lz)  //  (0,0,2)
         * */
        public void addPlacement(Placement p) {
            placements.add(p);
            currentWeight += p.box.weight;
            // add new extreme points: right, front, top of placed block
            int nx = p.x + p.lx;
            int ny = p.y + p.ly;
            int nz = p.z + p.lz;
            extremePoints.add(new int[]{nx, p.y, p.z});
            extremePoints.add(new int[]{p.x, ny, p.z});
            extremePoints.add(new int[]{p.x, p.y, nz});
            // remove dominated extreme points (simple filter)
            pruneExtremePoints();
        }

        // * Loại bỏ các điểm cực trị không hợp lệ or trùng lặp or nằm ngoài container
        private void pruneExtremePoints() {
            // remove points outside container or duplicates
            Set<String> seen = new HashSet<>();
            List<int[]> out = new ArrayList<>();
            for (int[] p : extremePoints) {
                if (p[0] < 0 || p[1] < 0 || p[2] < 0) continue;   // sinh ra điểm âm -> bỏ
                if (p[0] > maxX || p[1] > maxY || p[2] > maxZ) continue;   // ngoài công -> bỏ
                String k = p[0] + ":" + p[1] + ":" + p[2];
                if (!seen.contains(k)) {
                    seen.add(k);
                    out.add(p);
                }
            }
            extremePoints = out;
        }
    }

    // * Tạo 6 cách xoay kiện --> bỏ các cách trùng lặp
    public static List<int[]> rotations(int lx, int ly, int lz) {
        List<int[]> r = new ArrayList<>();
        r.add(new int[]{lx, ly, lz});
        r.add(new int[]{lx, lz, ly});
        r.add(new int[]{ly, lx, lz});
        r.add(new int[]{ly, lz, lx});
        r.add(new int[]{lz, lx, ly});
        r.add(new int[]{lz, ly, lx});
        // remove dupes
        Set<String> seen = new HashSet<>();
        List<int[]> uniq = new ArrayList<>();
        for (int[] a : r) {
            String k = a[0] + ":" + a[1] + ":" + a[2];
            if (!seen.contains(k)) {
                seen.add(k);
                uniq.add(a);
            }
        }
        return uniq;
    }

    // * thử đặt kiện vào container, trả về Placement nếu thành công (chỉ là hàm mô phỏng và không làm thay đổi dữ liệu thật)
    /**
     *
     * */
    public static Placement tryPlaceBoxInContainer(BoxItem box, ContainerState container) {

        // iterate extreme points
        for (int[] p : container.extremePoints) {
            int px = p[0], py = p[1], pz = p[2];
            // * kiểm tra hướng xoay phù hợp
            List<int[]> rots = ALLOW_ROTATION ? rotations(box.lx, box.ly, box.lz) : Collections.singletonList(new int[]{box.lx, box.ly, box.lz});

            for (int[] dim : rots) {
                int lx = dim[0], ly = dim[1], lz = dim[2];

                // bounds check
                if (px + lx > container.maxX || py + ly > container.maxY || pz + lz > container.maxZ) {
                    
                    continue;
                }

                // * kiểm tra va chạm với các kiện đã đặt
                boolean collide = false;
                for (Placement ex : container.placements) {
                    if (intersect(px, py, pz, lx, ly, lz, ex.x, ex.y, ex.z, ex.lx, ex.ly, ex.lz)) {
                        
                        collide = true;
                        break;
                    }
                }
                if (collide) continue;

                // weight check
                if (!container.checkWeightAfterAdd(box.weight)) {
                    continue;
                }

                return new Placement(box, px, py, pz, lx, ly, lz);
            }
        }

        return null;
    }

    public static boolean intersect(int ax, int ay, int az, int alx, int aly, int alz,
                                    int bx, int by, int bz, int blx, int bly, int blz) {
        boolean xOverlap = ax < bx + blx && bx < ax + alx;
        boolean yOverlap = ay < by + bly && by < ay + aly;
        boolean zOverlap = az < bz + blz && bz < az + alz;
        return xOverlap && yOverlap && zOverlap;
    }

    /**
     * Optimized bin packing algorithm for vehicle assignment.
     * 
     * Strategy:
     * 1. Validate package weights against maximum vehicle capacity (10 tons)
     * 2. Sort packages by weight (heaviest first) and volume for optimal packing
     * 3. Try to place packages in existing vehicles first (minimize vehicle count)
     * 4. Upgrade to larger vehicles when needed (cost optimization)
     * 5. Open new vehicles only when necessary (resource optimization)
     *
     * @param details      order details list (entities)
     * @param sizeRules sorted vehicle rules (from small->large capacity)
     *
     * @return List<ContainerState> each corresponds to one used container (vehicle)
     * @throws RuntimeException if any package exceeds maximum vehicle capacity
     */
    public static List<ContainerState> pack(List<OrderDetailEntity> details, List<SizeRuleEntity> sizeRules) {
        // convert OrderDetailEntity -> BoxItem
        List<BoxItem> boxes = new ArrayList<>();
        for (OrderDetailEntity d : details) {
            OrderSizeEntity s = d.getOrderSizeEntity();
            if (s == null) throw new IllegalArgumentException("missing size for detail " + d.getId());
            int lx = convertToInt(s.getMaxLength());
            int ly = convertToInt(s.getMaxWidth());
            int lz = convertToInt(s.getMaxHeight());
            long w = convertWeightToLong(d.getWeightTons());

            // Check if sizeRules is empty
            if (sizeRules.isEmpty()) {
                throw new IllegalArgumentException("No vehicle size rules provided for packing");
            }
            
            // Validate package weight against maximum vehicle capacity
            SizeRuleEntity largestVehicle = sizeRules.get(sizeRules.size() - 1);
            long maxVehicleWeight = convertWeightToLong(largestVehicle.getMaxWeight());
            
            if (w > maxVehicleWeight) {
                double packageTons = w / 1000.0;
                double maxTons = maxVehicleWeight / 1000.0;
                log.error("❌ Package {} exceeds maximum vehicle capacity: {:.2f} tons > {:.2f} tons",
                        d.getId(), packageTons, maxTons);
                throw new RuntimeException(
                    String.format("Không có loại xe nào chứa được kiện hàng: %s (trọng lượng %.2f tấn vượt quá giới hạn %.2f tấn). " +
                    "Vui lòng chia kiện hàng này thành nhiều kiện nhỏ hơn.",
                    d.getId(), packageTons, maxTons)
                );
            }
            
            boxes.add(new BoxItem(d.getId(), lx, ly, lz, w));
        }

        // Optimized sorting strategy: heaviest first, then by volume
        // This ensures large/heavy packages are placed first for better space utilization
        boxes.sort((a, b) -> {
            // Primary: Sort by weight (heaviest first)
            int cmp = Long.compare(b.weight, a.weight);
            if (cmp != 0) return cmp;

            // Secondary: Sort by volume (largest first) for better packing
            cmp = Long.compare(b.volume, a.volume);
            if (cmp != 0) return cmp;

            // Tertiary: Sort by longest dimension (to minimize container upgrades)
            int aMaxDim = Math.max(a.lx, Math.max(a.ly, a.lz));
            int bMaxDim = Math.max(b.lx, Math.max(b.ly, b.lz));
            return Integer.compare(bMaxDim, aMaxDim);
        });

        log.info("📦 Starting bin packing for {} packages", boxes.size());

        List<ContainerState> used = new ArrayList<>();

        for (BoxItem box : boxes) {
            boolean placed = false;

            // Strategy 1: Try to place in existing vehicles (minimize vehicle count)
            if (!used.isEmpty()) {
                // Sort containers by remaining capacity (place in fullest container that fits)
                List<ContainerState> sortedContainers = used.stream()
                        .sorted((c1, c2) -> {
                            long remaining1 = convertWeightToLong(c1.rule.getMaxWeight()) - c1.currentWeight;
                            long remaining2 = convertWeightToLong(c2.rule.getMaxWeight()) - c2.currentWeight;
                            return Long.compare(remaining1, remaining2);
                        })
                        .toList();

                for (ContainerState c : sortedContainers) {
                    Placement p = tryPlaceBoxInContainer(box, c);
                    if (p != null) {
                        c.addPlacement(p);
                        placed = true;
                        log.debug("✅ Placed box {} in existing vehicle {} ({})",
                                box.id, c.rule.getSizeRuleName(), used.indexOf(c) + 1);
                        break;
                    }
                }
            }

            if (placed) continue;

            // Strategy 2: Try upgrading existing vehicles (cost optimization)
            if (!used.isEmpty() && !placed) {
                // Try upgrading the vehicle that's closest to capacity
                List<ContainerState> sortedByCapacity = used.stream()
                        .sorted((c1, c2) -> {
                            double util1 = (double) c1.currentWeight / convertWeightToLong(c1.rule.getMaxWeight());
                            double util2 = (double) c2.currentWeight / convertWeightToLong(c2.rule.getMaxWeight());
                            return Double.compare(util2, util1); // Highest utilization first
                        })
                        .toList();

                for (int i = 0; i < sortedByCapacity.size() && !placed; i++) {
                    ContainerState current = sortedByCapacity.get(i);
                    int originalIndex = used.indexOf(current);

                    SizeRuleEntity upgradedRule = findNextBiggerRule(current.rule, sizeRules);
                    while (upgradedRule != null && !placed) {
                        ContainerState upgraded = upgradeContainer(current, upgradedRule);
                        if (upgraded != null) {
                            Placement p = tryPlaceBoxInContainer(box, upgraded);
                            if (p != null) {
                                upgraded.addPlacement(p);
                                used.set(originalIndex, upgraded);
                                placed = true;
                                log.debug("⬆️ Upgraded vehicle {} from {} to {} for box {}",
                                        originalIndex + 1, current.rule.getSizeRuleName(),
                                        upgraded.rule.getSizeRuleName(), box.id);
                                break;
                            }
                        }
                        upgradedRule = findNextBiggerRule(upgradedRule, sizeRules);
                    }
                }
            }

            if (placed) continue;

            // Strategy 3: Open new vehicle with optimal size (resource optimization)
            if (!placed) {
                SizeRuleEntity bestRule = null;
                ContainerState bestContainer = null;

                for (SizeRuleEntity rule : sizeRules) {
                    int maxX = convertToInt(rule.getMaxLength());
                    int maxY = convertToInt(rule.getMaxWidth());
                    int maxZ = convertToInt(rule.getMaxHeight());

                    // Check if package dimensions fit within vehicle dimensions
                    if (box.lx <= maxX && box.ly <= maxY && box.lz <= maxZ) {
                        ContainerState candidate = new ContainerState(rule, maxX, maxY, maxZ);

                        // Check weight capacity
                        if (!candidate.checkWeightAfterAdd(box.weight)) {
                            continue;
                        }

                        Placement p = tryPlaceBoxInContainer(box, candidate);
                        if (p != null) {
                            // Select the SMALLEST vehicle that fits (cost optimization)
                            if (bestRule == null || compareSizeRules(rule, bestRule) < 0) {
                                bestRule = rule;
                                bestContainer = candidate;
                                bestContainer.addPlacement(p);
                            }
                        }
                    }
                }

                if (bestContainer != null) {
                    used.add(bestContainer);
                    log.debug("🚛 Opened new vehicle {} ({}) for box {}",
                            used.size(), bestRule.getSizeRuleName(), box.id);
                } else {
                    double packageTons = box.weight / 1000.0;
                    log.error("❌ CRITICAL: No suitable vehicle found for box {} ({}x{}x{}, {:.2f} tons)",
                            box.id, box.lx, box.ly, box.lz, packageTons);
                    throw new RuntimeException(
                        String.format("Không có loại xe nào phù hợp với kiện hàng: %s (kích thước %dx%dx%d, trọng lượng %.2f tấn)",
                        box.id, box.lx, box.ly, box.lz, packageTons)
                    );
                }
            }
        }

        // Log packing summary
        log.info("✅ Bin packing completed: {} packages assigned to {} vehicles", boxes.size(), used.size());
        for (int i = 0; i < used.size(); i++) {
            ContainerState container = used.get(i);
            double utilization = (double) container.currentWeight / convertWeightToLong(container.rule.getMaxWeight()) * 100;
            log.info("   Vehicle {}: {} - {} packages, {:.1f}% weight utilization",
                    i + 1, container.rule.getSizeRuleName(), container.placements.size(), utilization);
        }

        return used;
    }

    /**
     * Tìm vehicle rule lớn hơn tiếp theo trong danh sách đã sorted
     */
    private static SizeRuleEntity findNextBiggerRule(SizeRuleEntity current, List<SizeRuleEntity> sortedRules) {
        int currentIndex = -1;
        for (int i = 0; i < sortedRules.size(); i++) {
            if (sortedRules.get(i).getId().equals(current.getId())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex != -1 && currentIndex + 1 < sortedRules.size()) {
            return sortedRules.get(currentIndex + 1);
        }
        return null;
    }

    public static ContainerState upgradeContainer(ContainerState current, SizeRuleEntity upgradedRule) {
        int maxX = convertToInt(upgradedRule.getMaxLength());
        int maxY = convertToInt(upgradedRule.getMaxWidth());
        int maxZ = convertToInt(upgradedRule.getMaxHeight());

        ContainerState upgraded = new ContainerState(upgradedRule, maxX, maxY, maxZ);

        // repack lại toàn bộ các box cũ
        for (BoxItem box : current.placements.stream().map(p -> p.box).toList()) {
            Placement p = tryPlaceBoxInContainer(box, upgraded);
            if (p == null) {
                return null; // upgrade thất bại
            }
            upgraded.addPlacement(p);
        }
        return upgraded;
    }

    private static int compareSizeRules(SizeRuleEntity a, SizeRuleEntity b) {
        int cmp = a.getMaxWeight().compareTo(b.getMaxWeight());
        if (cmp != 0) return cmp;
        cmp = a.getMaxLength().compareTo(b.getMaxLength());
        if (cmp != 0) return cmp;
        cmp = a.getMaxWidth().compareTo(b.getMaxWidth());
        if (cmp != 0) return cmp;
        return a.getMaxHeight().compareTo(b.getMaxHeight());
    }

    public static int convertToInt(BigDecimal bd) {
        if (bd == null) return 0;
        return (int) Math.ceil(bd.doubleValue() * UNIT_MULTIPLIER);
    }

    public static long convertWeightToLong(BigDecimal w) {
        if (w == null) return 0L;
        // assume weight in kg -> convert to grams
        return Math.round(w.doubleValue() * 1000.0);
    }

    // Convert ContainerState -> ContractRuleAssignResponse
    public static List<capstone_project.dtos.response.order.contract.ContractRuleAssignResponse> toContractResponses(
            List<ContainerState> used,
            List<OrderDetailEntity> details
    ) {
        // Map để lấy lại dữ liệu chuẩn từ DB
        Map<UUID, OrderDetailEntity> detailMap = details.stream()
                .collect(Collectors.toMap(OrderDetailEntity::getId, Function.identity()));

        List<capstone_project.dtos.response.order.contract.ContractRuleAssignResponse> out = new ArrayList<>();
        int vehicleIndex = 0;

        for (ContainerState c : used) {
            // Phát hiện đơn vị chủ đạo từ các detail
            String dominantUnit = c.placements.stream()
                    .map(pl -> {
                        OrderDetailEntity detail = detailMap.get(pl.box.id);
                        return detail != null ? detail.getUnit() : null;
                    })
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("Kí"); // Default là Kí

            // Tính currentLoadPrecise bằng weightBaseUnit (tương thích với dữ liệu cũ)
            BigDecimal currentLoadPrecise = c.placements.stream()
                    .map(pl -> {
                        OrderDetailEntity detail = detailMap.get(pl.box.id);
                        if (detail == null) return BigDecimal.ZERO;

                        // Ưu tiên dùng weightBaseUnit
                        BigDecimal baseWeight = detail.getWeightBaseUnit();
                        if (baseWeight != null) {
                            return baseWeight;
                        }
                        // Fallback về weight
                        return detail.getWeightTons() != null ? detail.getWeightTons() : BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // assignedDetails = data chuẩn từ DB
            List<OrderDetailForPackingResponse> assigned = c.placements.stream()
                    .map(pl -> {
                        OrderDetailEntity detail = detailMap.get(pl.box.id);
                        return new OrderDetailForPackingResponse(
                                detail.getId().toString(),
                                detail.getWeightTons(),
                                detail.getWeightBaseUnit(),
                                detail.getUnit(),
                                detail.getTrackingCode()
                        );
                    })
                    .collect(Collectors.toList());

            // packedDetailDetails = thông tin packing
            List<PackedDetailResponse> packedDetails = c.placements.stream()
                    .map(pl -> new PackedDetailResponse(
                            pl.box.id.toString(),
                            BigDecimal.valueOf(pl.x).divide(BigDecimal.valueOf(UNIT_MULTIPLIER)),
                            BigDecimal.valueOf(pl.y).divide(BigDecimal.valueOf(UNIT_MULTIPLIER)),
                            BigDecimal.valueOf(pl.z).divide(BigDecimal.valueOf(UNIT_MULTIPLIER)),
                            BigDecimal.valueOf(pl.lx).divide(BigDecimal.valueOf(UNIT_MULTIPLIER)),
                            BigDecimal.valueOf(pl.ly).divide(BigDecimal.valueOf(UNIT_MULTIPLIER)),
                            BigDecimal.valueOf(pl.lz).divide(BigDecimal.valueOf(UNIT_MULTIPLIER)),
                            pl.lx + "x" + pl.ly + "x" + pl.lz,
                            pl.lx,
                            pl.ly,
                            pl.lz
                    ))
                    .toList();

            capstone_project.dtos.response.order.contract.ContractRuleAssignResponse resp = capstone_project.dtos.response.order.contract.ContractRuleAssignResponse.builder()
                    .vehicleIndex(vehicleIndex++)
                    .sizeRuleId(c.rule.getId())
                    .sizeRuleName(c.rule.getSizeRuleName())
                    .currentLoad(currentLoadPrecise)
                    .currentLoadUnit(dominantUnit) // Sử dụng đơn vị động
                    .assignedDetails(assigned)
                    .packedDetailDetails(packedDetails)
                    .build();

            out.add(resp);
        }

        return out;
    }

    public static class ManualResult {
        public List<ContainerState> containers;
        public List<BoxItem> unplaced;
        public Map<ContainerState, BigDecimal> containerLoads;
    }

    public static ManualResult packManual(List<OrderDetailEntity> details, List<ContainerState> fixedContainers) {
        List<BoxItem> boxes = new ArrayList<>();
        Map<UUID, BigDecimal> originalWeights = new HashMap<>();

        for (OrderDetailEntity d : details) {
            OrderSizeEntity s = d.getOrderSizeEntity();
            if (s == null) continue;

            int lx = convertToInt(s.getMaxLength());
            int ly = convertToInt(s.getMaxWidth());
            int lz = convertToInt(s.getMaxHeight());

            BigDecimal weightTon = d.getWeightTons();
            boxes.add(new BoxItem(d.getId(), lx, ly, lz, weightTon.multiply(BigDecimal.valueOf(1000)).longValue()));
            originalWeights.put(d.getId(), weightTon);
        }

        boxes.sort((a, b) -> Long.compare(b.weight, a.weight));
        fixedContainers.sort(Comparator.comparing(c -> c.rule.getMaxWeight()));

        List<BoxItem> unplaced = new ArrayList<>();
        Map<ContainerState, BigDecimal> containerLoads = new HashMap<>();

        for (ContainerState container : fixedContainers) {
            BigDecimal initialLoad = container.placements.stream()
                    .map(p -> {
                        return details.stream()
                                .filter(d -> d.getId().equals(p.box.id))
                                .findFirst()
                                .map(OrderDetailEntity::getWeightTons)
                                .orElse(BigDecimal.ZERO);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            containerLoads.put(container, initialLoad);
        }

        for (BoxItem box : boxes) {
            boolean placed = false;

            boolean canFitAnyContainer = fixedContainers.stream().anyMatch(container -> {
                int maxL = convertToInt(container.rule.getMaxLength());
                int maxW = convertToInt(container.rule.getMaxWidth());
                int maxH = convertToInt(container.rule.getMaxHeight());
                BigDecimal boxWeightBD = originalWeights.get(box.id);
                BigDecimal maxWeightBD = container.rule.getMaxWeight();
                return box.lx <= maxL && box.ly <= maxW && box.lz <= maxH && boxWeightBD.compareTo(maxWeightBD) <= 0;
            });

            if (!canFitAnyContainer) {
                BigDecimal originalWeight = originalWeights.get(box.id);
                throw new BadRequestException(
                        String.format("Box ID %s (weight=%s tấn, size=%dx%dx%d) is too large or heavy for all vehicles.",
                                box.id, originalWeight.stripTrailingZeros().toPlainString(), box.lx, box.ly, box.lz),
                        ErrorEnum.INVALID.getErrorCode()
                );
            }

            for (ContainerState container : fixedContainers) {
                BigDecimal currentLoadTon = containerLoads.get(container);
                BigDecimal nextLoadTon = currentLoadTon.add(originalWeights.get(box.id));
                BigDecimal maxWeightTon = container.rule.getMaxWeight();

                if (nextLoadTon.compareTo(maxWeightTon) > 0) continue;

                Placement placement = tryPlaceBoxInContainer(box, container);
                if (placement != null) {
                    container.addPlacement(placement);
                    containerLoads.put(container, nextLoadTon);
                    container.currentWeight = nextLoadTon.multiply(BigDecimal.valueOf(1000)).longValue();
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                unplaced.add(box);
            }
        }

        ManualResult result = new ManualResult();
        result.containers = fixedContainers;
        result.unplaced = unplaced;
        result.containerLoads = containerLoads;

        return result;
    }

    public static ManualResult packManualForDetails(List<OrderDetailEntity> details,
                                                    SizeRuleEntity sizeRule,
                                                    int numContainers) {
        List<ContainerState> containers = new ArrayList<>();
        for (int i = 0; i < numContainers; i++) {
            containers.add(new ContainerState(
                    sizeRule,
                    convertToInt(sizeRule.getMaxLength()),
                    convertToInt(sizeRule.getMaxWidth()),
                    convertToInt(sizeRule.getMaxHeight())
            ));
        }
        return packManual(details, containers);
    }
}
