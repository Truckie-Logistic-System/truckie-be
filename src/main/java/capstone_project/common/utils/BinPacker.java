package capstone_project.common.utils;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.dtos.response.order.contract.OrderDetailForPackingResponse;
import capstone_project.dtos.response.order.contract.PackedDetailResponse;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.entity.pricing.VehicleTypeRuleEntity;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class BinPacker {

    private static final boolean ALLOW_ROTATION = true;
    public static final int UNIT_MULTIPLIER = 10;

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

    // 1 xe
    public static class ContainerState {

        public final VehicleTypeRuleEntity rule;
        final int maxX, maxY, maxZ;
        public long currentWeight;
        public List<Placement> placements = new ArrayList<>();
        List<int[]> extremePoints = new ArrayList<>(); // each point [x,y,z]

        public ContainerState(VehicleTypeRuleEntity rule, int maxX, int maxY, int maxZ) {
            this.rule = rule;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.currentWeight = 0;
            // initial extreme point = origin (0,0,0)
            this.extremePoints.add(new int[]{0, 0, 0});
        }

        boolean checkWeightAfterAdd(long addWeight) {
            BigDecimal maxW = rule.getMaxWeight() == null ? BigDecimal.valueOf(Long.MAX_VALUE) : rule.getMaxWeight();
            long maxWeightGram = Math.round(maxW.doubleValue() * 1000.0);
            return (currentWeight + addWeight) <= maxWeightGram;
        }

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

        private void pruneExtremePoints() {
            // remove points outside container or duplicates
            Set<String> seen = new HashSet<>();
            List<int[]> out = new ArrayList<>();
            for (int[] p : extremePoints) {
                if (p[0] < 0 || p[1] < 0 || p[2] < 0) continue;
                if (p[0] > maxX || p[1] > maxY || p[2] > maxZ) continue;
                String k = p[0] + ":" + p[1] + ":" + p[2];
                if (!seen.contains(k)) {
                    seen.add(k);
                    out.add(p);
                }
            }
            extremePoints = out;
        }
    }

    // Generate 6 rotation permutations
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

    // Try place box into container; returns Placement if success
    public static Placement tryPlaceBoxInContainer(BoxItem box, ContainerState container) {
        log.debug("Trying to place box {} ({}x{}x{}) in container {} ({}x{}x{}) with {} existing placements",
                box.id, box.lx, box.ly, box.lz,
                container.rule.getVehicleTypeRuleName(), container.maxX, container.maxY, container.maxZ,
                container.placements.size());

        // iterate extreme points
        for (int[] p : container.extremePoints) {
            int px = p[0], py = p[1], pz = p[2];
            List<int[]> rots = ALLOW_ROTATION ? rotations(box.lx, box.ly, box.lz) : Collections.singletonList(new int[]{box.lx, box.ly, box.lz});

            for (int[] dim : rots) {
                int lx = dim[0], ly = dim[1], lz = dim[2];

                // bounds check
                if (px + lx > container.maxX || py + ly > container.maxY || pz + lz > container.maxZ) {
                    log.debug("  - Out of bounds: point={},{},{}, dim={}x{}x{}", px, py, pz, lx, ly, lz);
                    continue;
                }

                // collision check
                boolean collide = false;
                for (Placement ex : container.placements) {
                    if (intersect(px, py, pz, lx, ly, lz, ex.x, ex.y, ex.z, ex.lx, ex.ly, ex.lz)) {
                        log.debug("  - Collision with existing placement at {},{},{}", ex.x, ex.y, ex.z);
                        collide = true;
                        break;
                    }
                }
                if (collide) continue;

                // weight check
                if (!container.checkWeightAfterAdd(box.weight)) {
                    log.debug("  - Weight limit exceeded: current={}, add={}, max={}",
                            container.currentWeight, box.weight,
                            Math.round(container.rule.getMaxWeight().doubleValue() * 1000.0));
                    continue;
                }

                log.info("SUCCESS: Placed at {},{},{} with dim {}x{}x{}", px, py, pz, lx, ly, lz);
                return new Placement(box, px, py, pz, lx, ly, lz);
            }
        }

        log.debug("FAILED: No valid placement found for box {} in container {}", box.id, container.rule.getVehicleTypeRuleName());
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
     * Main packing function.
     *
     * @param details      order details list (entities)
     * @param vehicleRules sorted vehicle rules (from small->large or as you prefer)
     *
     * @return List<ContainerState> each corresponds to one used container (vehicle)
     */
    public static List<ContainerState> pack(List<OrderDetailEntity> details, List<VehicleTypeRuleEntity> vehicleRules) {
        // convert OrderDetailEntity -> BoxItem (SỬA LỖI CONVERT WEIGHT)
        List<BoxItem> boxes = new ArrayList<>();
        for (OrderDetailEntity d : details) {
            OrderSizeEntity s = d.getOrderSizeEntity();
            if (s == null) throw new IllegalArgumentException("missing size for detail " + d.getId());
            int lx = convertToInt(s.getMaxLength());
            int ly = convertToInt(s.getMaxWidth());
            int lz = convertToInt(s.getMaxHeight());
            long w = convertWeightToLong(d.getWeight());

            log.info("Converting order detail: id={}, original_size={}x{}x{}, weight={}, converted={}x{}x{}, weight_grams={}",
                    d.getId(), s.getMaxLength(), s.getMaxWidth(), s.getMaxHeight(), d.getWeight(),
                    lx, ly, lz, w);

            boxes.add(new BoxItem(d.getId(), lx, ly, lz, w));
        }

        // Sắp xếp tối ưu hơn
        boxes.sort((a, b) -> {
            int cmp = Long.compare(b.weight, a.weight);
            if (cmp != 0) return cmp;

            int aMaxDim = Math.max(a.lx, Math.max(a.ly, a.lz));
            int bMaxDim = Math.max(b.lx, Math.max(b.ly, b.lz));
            cmp = Integer.compare(bMaxDim, aMaxDim);
            if (cmp != 0) return cmp;

            return Long.compare(b.volume, a.volume);
        });

        List<ContainerState> used = new ArrayList<>();

        for (BoxItem box : boxes) {
            boolean placed = false;

            // 1. Thử thêm vào xe hiện có TRƯỚC
            log.info("=== STEP 1: Trying to place box {} in existing vehicles ===", box.id);
            if (!used.isEmpty()) {
                for (ContainerState c : used) {
                    log.info("  - Trying vehicle: {}", c.rule.getVehicleTypeRuleName());
                    Placement p = tryPlaceBoxInContainer(box, c);
                    if (p != null) {
                        c.addPlacement(p);
                        placed = true;
                        log.info("SUCCESS: Added box {} to existing vehicle {}", box.id, c.rule.getVehicleTypeRuleName());
                        break;
                    } else {
                        log.info("FAILED: Cannot place box {} in vehicle {}", box.id, c.rule.getVehicleTypeRuleName());
                    }
                }
            } else {
                log.info("  - No existing vehicles available");
            }

            if (placed) {
                log.info("=== Box {} PLACED in Step 1, SKIPPING other steps ===", box.id);
                continue;
            }

            // 2. Thử UPGRADE từng xe hiện có
            log.info("=== STEP 2: Trying to upgrade existing vehicles for box {} ===", box.id);
            if (!used.isEmpty()) {
                for (int i = 0; i < used.size(); i++) {
                    ContainerState current = used.get(i);
                    log.info("Considering upgrade for vehicle: {}", current.rule.getVehicleTypeRuleName());

                    VehicleTypeRuleEntity upgradedRule = findNextBiggerRule(current.rule, vehicleRules);
                    while (upgradedRule != null && !placed) {
                        log.info("    - Trying upgrade to: {}", upgradedRule.getVehicleTypeRuleName());
                        ContainerState upgraded = upgradeContainer(current, upgradedRule);
                        if (upgraded != null) {
                            Placement p = tryPlaceBoxInContainer(box, upgraded);
                            if (p != null) {
                                upgraded.addPlacement(p);
                                used.set(i, upgraded);
                                placed = true;
                                log.info("SUCCESS: Upgraded from {} to {} for box {}",
                                        current.rule.getVehicleTypeRuleName(), upgradedRule.getVehicleTypeRuleName(), box.id);
                                break;
                            } else {
                                log.info("FAILED: Cannot place box {} in upgraded vehicle {}", box.id, upgradedRule.getVehicleTypeRuleName());
                            }
                        } else {
                            log.info("FAILED: Cannot upgrade to {}", upgradedRule.getVehicleTypeRuleName());
                        }
                        upgradedRule = findNextBiggerRule(upgradedRule, vehicleRules);
                    }
                    if (placed) break;
                }
            } else {
                log.info("  - No existing vehicles to upgrade");
            }

            if (placed) {
                log.info("=== Box {} PLACED in Step 2, SKIPPING Step 3 ===", box.id);
                continue;
            }

            // 3. Mở xe mới - tìm xe NHỎ NHẤT có thể chở
            log.info("=== STEP 3: Opening new vehicle for box {} ===", box.id);
            VehicleTypeRuleEntity bestRule = null;
            ContainerState bestContainer = null;

            for (VehicleTypeRuleEntity rule : vehicleRules) {
                int maxX = convertToInt(rule.getMaxLength());
                int maxY = convertToInt(rule.getMaxWidth());
                int maxZ = convertToInt(rule.getMaxHeight());

                log.info("  - Vehicle rule: {} - max_dims={}x{}x{}, max_weight={}",
                        rule.getVehicleTypeRuleName(), maxX, maxY, maxZ, rule.getMaxWeight());

                if (box.lx <= maxX && box.ly <= maxY && box.lz <= maxZ) {
                    ContainerState candidate = new ContainerState(rule, maxX, maxY, maxZ);
                    if (!candidate.checkWeightAfterAdd(box.weight)) {
                        log.info("Weight limit exceeded: box weight={}, max weight={}",
                                box.weight, Math.round(rule.getMaxWeight().doubleValue() * 1000.0));
                        continue;
                    }

                    Placement p = tryPlaceBoxInContainer(box, candidate);
                    if (p != null) {
                        log.info("Can place box in vehicle {}", rule.getVehicleTypeRuleName());
                        // Tìm xe NHỎ NHẤT
                        if (bestRule == null || compareVehicleTypeRules(rule, bestRule) < 0) {
                            bestRule = rule;
                            bestContainer = candidate;
                            bestContainer.addPlacement(p);
                            log.info("New best vehicle: {}", rule.getVehicleTypeRuleName());
                        }
                    } else {
                        log.info("Cannot place box in vehicle {} - packing failed", rule.getVehicleTypeRuleName());
                    }
                } else {
                    log.info("Box too large for vehicle {}: box={}x{}x{}, vehicle={}x{}x{}",
                            rule.getVehicleTypeRuleName(), box.lx, box.ly, box.lz, maxX, maxY, maxZ);
                }
            }

            if (bestContainer != null) {
                used.add(bestContainer);
                log.info("SUCCESS: Opened new vehicle {} for box {}", bestRule.getVehicleTypeRuleName(), box.id);
            } else {
                log.error("CRITICAL ERROR: No vehicle can carry box {} ({}x{}x{}, weight={})",
                        box.id, box.lx, box.ly, box.lz, box.weight);
                throw new RuntimeException("Không có loại xe nào chứa được kiện: " + box.id);
            }
        }

        log.info("=== PACKING COMPLETED: Used {} vehicles for {} boxes ===", used.size(), boxes.size());
        return used;
    }

    /**
     * Tìm vehicle rule lớn hơn tiếp theo trong danh sách đã sorted
     */
    private static VehicleTypeRuleEntity findNextBiggerRule(VehicleTypeRuleEntity current, List<VehicleTypeRuleEntity> sortedRules) {
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

    private static boolean tryUpgradeExistingContainer(BoxItem box, List<ContainerState> used,
                                                       List<VehicleTypeRuleEntity> vehicleRules) {
        log.info("🔄 Attempting to upgrade existing containers for box {}", box.id);

        for (int i = 0; i < used.size(); i++) {
            ContainerState current = used.get(i);
            log.info("  - Checking container: {}", current.rule.getVehicleTypeRuleName());

            Placement directPlacement = tryPlaceBoxInContainer(box, current);
            if (directPlacement != null) {
                current.addPlacement(directPlacement);
                log.info("Directly placed box {} in existing vehicle {}", box.id, current.rule.getVehicleTypeRuleName());
                return true;
            }

            VehicleTypeRuleEntity currentRule = current.rule;
            for (VehicleTypeRuleEntity biggerRule : vehicleRules) {
                if (compareVehicleTypeRules(biggerRule, currentRule) <= 0) continue;

                ContainerState upgraded = upgradeContainer(current, biggerRule);
                if (upgraded != null) {
                    Placement p = tryPlaceBoxInContainer(box, upgraded);
                    if (p != null) {
                        upgraded.addPlacement(p);
                        used.set(i, upgraded);
                        log.info("Upgraded vehicle from {} to {} for box {}",
                                currentRule.getVehicleTypeRuleName(), biggerRule.getVehicleTypeRuleName(), box.id);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int compareVehicleTypeRules(VehicleTypeRuleEntity a, VehicleTypeRuleEntity b) {
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

    public static ContainerState upgradeContainer(ContainerState current, VehicleTypeRuleEntity upgradedRule) {
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

    // Convert ContainerState -> ContractRuleAssignResponse
    public static List<ContractRuleAssignResponse> toContractResponses(
            List<ContainerState> used,
            List<OrderDetailEntity> details
    ) {
        // Map để lấy lại dữ liệu chuẩn từ DB
        Map<UUID, OrderDetailEntity> detailMap = details.stream()
                .collect(Collectors.toMap(OrderDetailEntity::getId, Function.identity()));

        List<ContractRuleAssignResponse> out = new ArrayList<>();
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
                        return detail.getWeight() != null ? detail.getWeight() : BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // assignedDetails = data chuẩn từ DB
            List<OrderDetailForPackingResponse> assigned = c.placements.stream()
                    .map(pl -> {
                        OrderDetailEntity detail = detailMap.get(pl.box.id);
                        return new OrderDetailForPackingResponse(
                                detail.getId().toString(),
                                detail.getWeight(),
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

            ContractRuleAssignResponse resp = ContractRuleAssignResponse.builder()
                    .vehicleIndex(vehicleIndex++)
                    .vehicleTypeRuleId(c.rule.getId())
                    .vehicleTypeRuleName(c.rule.getVehicleTypeRuleName())
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
        public Map<ContainerState, BigDecimal> containerLoads; // Thêm map này
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

            BigDecimal weightTon = d.getWeight();
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
                                .map(OrderDetailEntity::getWeight)
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
                                                    VehicleTypeRuleEntity vehicleRule,
                                                    int numContainers) {
        List<ContainerState> containers = new ArrayList<>();
        for (int i = 0; i < numContainers; i++) {
            containers.add(new ContainerState(
                    vehicleRule,
                    convertToInt(vehicleRule.getMaxLength()),
                    convertToInt(vehicleRule.getMaxWidth()),
                    convertToInt(vehicleRule.getMaxHeight())
            ));
        }
        return packManual(details, containers);
    }
}