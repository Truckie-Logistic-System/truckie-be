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
        log.info("[BinPacker] Starting pack() with {} packages and {} size rules", details.size(), sizeRules.size());
        
        // Check if sizeRules is empty (before loop)
        if (sizeRules.isEmpty()) {
            log.error("[BinPacker] No vehicle size rules provided for packing");
            throw new IllegalArgumentException("Không có loại xe nào được cấu hình cho danh mục hàng hóa này");
        }
        
        // Log size rules for debugging
        for (SizeRuleEntity rule : sizeRules) {
            log.info("[BinPacker] SizeRule: name='{}', weight={}, length={}, width={}, height={}",
                    rule.getSizeRuleName(), rule.getMaxWeight(), rule.getMaxLength(), 
                    rule.getMaxWidth(), rule.getMaxHeight());
        }
        
        // convert OrderDetailEntity -> BoxItem
        List<BoxItem> boxes = new ArrayList<>();
        for (OrderDetailEntity d : details) {
            OrderSizeEntity s = d.getOrderSizeEntity();
            if (s == null) {
                log.error("[BinPacker] OrderDetail {} has NULL OrderSizeEntity!", d.getId());
                throw new IllegalArgumentException("Kiện hàng " + d.getId() + " không có thông tin kích thước. Vui lòng cập nhật kích thước cho kiện hàng.");
            }
            
            int lx = convertToInt(s.getMaxLength());
            int ly = convertToInt(s.getMaxWidth());
            int lz = convertToInt(s.getMaxHeight());
            long w = convertWeightToLong(d.getWeightTons());
            
            log.info("[BinPacker] Package: id={}, weight={}kg, size={}x{}x{} (units)",
                    d.getId(), w, lx, ly, lz);
            
            // Validate package weight against maximum vehicle capacity
            SizeRuleEntity largestVehicle = sizeRules.get(sizeRules.size() - 1);
            long maxVehicleWeight = convertWeightToLong(largestVehicle.getMaxWeight());
            
            if (w > maxVehicleWeight) {
                double packageTons = w / 1000.0;
                double maxTons = maxVehicleWeight / 1000.0;
                log.error("[BinPacker] Package {} exceeds capacity: {}kg > {}kg (max)", d.getId(), w, maxVehicleWeight);
                throw new BadRequestException(
                    String.format("Không có loại xe nào chứa được kiện hàng (trọng lượng %.2f tấn vượt quá giới hạn %.2f tấn). " +
                    "Vui lòng chia kiện hàng này thành nhiều kiện nhỏ hơn.",
                    packageTons, maxTons),
                    ErrorEnum.NO_VEHICLE_AVAILABLE.getErrorCode()
                );
            }
            
            boxes.add(new BoxItem(d.getId(), lx, ly, lz, w));
        }

        // OPTIMIZED sorting strategy: density-aware multi-criteria sorting
        // This ensures optimal packing by considering weight, volume, and density together
        boxes.sort((a, b) -> {
            // Calculate density (weight/volume ratio) for both packages
            double densityA = a.volume > 0 ? (double) a.weight / a.volume : 0.0;
            double densityB = b.volume > 0 ? (double) b.weight / b.volume : 0.0;
            
            // Primary: Sort by density category (heavy-dense items first)
            // This prevents placing light items first and wasting weight capacity
            boolean aIsHeavyDense = densityA > 15.0; // >15 kg/dm³ = metals, machinery
            boolean bIsHeavyDense = densityB > 15.0;
            if (aIsHeavyDense != bIsHeavyDense) {
                return bIsHeavyDense ? 1 : -1; // Heavy-dense items first
            }

            // Secondary: Within same density category, sort by volume (largest first)
            // This ensures large items are placed before small ones
            int cmp = Long.compare(b.volume, a.volume);
            if (cmp != 0) return cmp;

            // Tertiary: Sort by weight (heaviest first) for items with same volume
            cmp = Long.compare(b.weight, a.weight);
            if (cmp != 0) return cmp;

            // Quaternary: Sort by longest dimension (to minimize container upgrades)
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
                // Sort containers by COMBINED utilization score (weight + volume)
                // Prefer containers with balanced utilization for better packing efficiency
                List<ContainerState> sortedContainers = used.stream()
                        .sorted((c1, c2) -> {
                            double score1 = calculateCombinedUtilization(c1);
                            double score2 = calculateCombinedUtilization(c2);
                            // Higher score first (better utilized)
                            return Double.compare(score2, score1);
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

            // Strategy 3: Open new vehicle with BEST-FIT size (optimal resource utilization)
            if (!placed) {
                SizeRuleEntity bestRule = null;
                ContainerState bestContainer = null;
                double bestFitScore = -1.0;

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
                            // Calculate fit score: balance between minimizing waste and avoiding upgrades later
                            double fitScore = calculateVehicleFitScore(box, rule, maxX, maxY, maxZ);
                            
                            // Select vehicle with BEST fit score (not just smallest)
                            if (fitScore > bestFitScore) {
                                bestFitScore = fitScore;
                                bestRule = rule;
                                bestContainer = candidate;
                                bestContainer.addPlacement(p);
                            }
                        }
                    }
                }

                if (bestContainer != null) {
                    used.add(bestContainer);
                    log.info("[BinPacker] Opened new vehicle {} ({}) for box {}",
                            used.size(), bestRule.getSizeRuleName(), box.id);
                } else {
                    double packageTons = box.weight / 1000.0;
                    double packageLengthM = box.lx / (double) UNIT_MULTIPLIER;
                    double packageWidthM = box.ly / (double) UNIT_MULTIPLIER;
                    double packageHeightM = box.lz / (double) UNIT_MULTIPLIER;
                    
                    // Find the largest vehicle dimensions for error message
                    SizeRuleEntity largestRule = sizeRules.get(sizeRules.size() - 1);
                    double maxVehicleLength = largestRule.getMaxLength() != null ? largestRule.getMaxLength().doubleValue() : 0;
                    double maxVehicleWidth = largestRule.getMaxWidth() != null ? largestRule.getMaxWidth().doubleValue() : 0;
                    double maxVehicleHeight = largestRule.getMaxHeight() != null ? largestRule.getMaxHeight().doubleValue() : 0;
                    
                    log.error("[BinPacker] CRITICAL: No suitable vehicle found for package: " +
                            "id={}, size={}x{}x{}m (vehicle max={}x{}x{}m), weight={}T (vehicle max={}T)",
                            box.id, packageLengthM, packageWidthM, packageHeightM,
                            maxVehicleLength, maxVehicleWidth, maxVehicleHeight,
                            packageTons, largestRule.getMaxWeight());
                    
                    throw new BadRequestException(
                        String.format("Kiện hàng có kích thước %.2fx%.2fx%.2f m, %.2f tấn không vừa với bất kỳ loại xe nào. " +
                        "Xe lớn nhất chỉ chứa được %.2fx%.2fx%.2f m. Vui lòng chia nhỏ kiện hàng.",
                        packageLengthM, packageWidthM, packageHeightM, packageTons,
                        maxVehicleLength, maxVehicleWidth, maxVehicleHeight),
                        ErrorEnum.NO_VEHICLE_AVAILABLE.getErrorCode()
                    );
                }
            }
        }

        // Log packing summary with volume and weight utilization
        log.info("✅ Bin packing completed: {} packages assigned to {} vehicles", boxes.size(), used.size());
        for (int i = 0; i < used.size(); i++) {
            ContainerState container = used.get(i);
            
            // Calculate weight utilization
            long maxWeightGram = convertWeightToLong(container.rule.getMaxWeight());
            double weightUtil = (double) container.currentWeight / maxWeightGram * 100;
            
            // Calculate volume utilization
            long usedVolume = container.placements.stream()
                    .mapToLong(p -> (long) p.lx * p.ly * p.lz)
                    .sum();
            long totalVolume = (long) container.maxX * container.maxY * container.maxZ;
            double volumeUtil = (double) usedVolume / totalVolume * 100;
            
            log.info("   Vehicle {}: {} - {} packages, Weight: {:.1f}%, Volume: {:.1f}%",
                    i + 1, container.rule.getSizeRuleName(), container.placements.size(), 
                    weightUtil, volumeUtil);
            
            // Add intelligent warnings based on utilization patterns
            if (volumeUtil < 30 && weightUtil > 70) {
                log.info("      ℹ️  Heavy-Dense cargo detected (low volume, high weight). " +
                        "Normal for metals, machinery, or dense materials.");
            } else if (volumeUtil > 70 && weightUtil < 30) {
                log.info("      ℹ️  Light-Bulky cargo detected (high volume, low weight). " +
                        "Normal for cotton, foam, paper, or packaging materials.");
            } else if (volumeUtil < 30 && weightUtil < 30) {
                log.warn("      ⚠️  INEFFICIENT packing: Low utilization on BOTH volume ({:.1f}%) and weight ({:.1f}%). " +
                        "Consider consolidating with other shipments or using a smaller vehicle.",
                        volumeUtil, weightUtil);
            } else if (volumeUtil > 80 && weightUtil > 80) {
                log.info("      ✅ Excellent utilization! Both volume ({:.1f}%) and weight ({:.1f}%) are well optimized.",
                        volumeUtil, weightUtil);
            }
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

    /**
     * Calculate COMBINED utilization score for a container (weight + volume)
     * Higher score = better utilization = prefer this container
     */
    private static double calculateCombinedUtilization(ContainerState container) {
        // Weight utilization
        long maxWeightGram = convertWeightToLong(container.rule.getMaxWeight());
        double weightUtil = maxWeightGram > 0 ? (double) container.currentWeight / maxWeightGram : 0.0;
        
        // Volume utilization
        long usedVolume = container.placements.stream()
                .mapToLong(p -> (long) p.lx * p.ly * p.lz)
                .sum();
        long totalVolume = (long) container.maxX * container.maxY * container.maxZ;
        double volumeUtil = totalVolume > 0 ? (double) usedVolume / totalVolume : 0.0;
        
        // Combined score: weight is more important (60%) than volume (40%)
        // This prevents wasting expensive weight capacity
        return (weightUtil * 0.6) + (volumeUtil * 0.4);
    }

    /**
     * Calculate fit score for selecting the best vehicle for a package
     * Considers: size fit, weight fit, and potential for future packing
     * Higher score = better fit
     */
    private static double calculateVehicleFitScore(BoxItem box, SizeRuleEntity rule, int maxX, int maxY, int maxZ) {
        // 1. Dimension fit ratio (avoid too much wasted space)
        double lengthFit = (double) box.lx / maxX;
        double widthFit = (double) box.ly / maxY;
        double heightFit = (double) box.lz / maxZ;
        double avgDimensionFit = (lengthFit + widthFit + heightFit) / 3.0;
        
        // 2. Weight fit ratio
        long maxWeightGram = convertWeightToLong(rule.getMaxWeight());
        double weightFit = maxWeightGram > 0 ? (double) box.weight / maxWeightGram : 0.0;
        
        // 3. Volume fit ratio
        long boxVolume = (long) box.lx * box.ly * box.lz;
        long containerVolume = (long) maxX * maxY * maxZ;
        double volumeFit = containerVolume > 0 ? (double) boxVolume / containerVolume : 0.0;
        
        // Combined fit score:
        // - Prefer balanced utilization (30% dimension, 35% weight, 35% volume)
        // - Penalize vehicles that are TOO LARGE (waste) or TOO SMALL (won't fit more items later)
        // - Sweet spot: 0.4-0.7 utilization on first item
        double rawScore = (avgDimensionFit * 0.3) + (weightFit * 0.35) + (volumeFit * 0.35);
        
        // Bonus for "good fit" range (40-70% utilization)
        if (rawScore >= 0.4 && rawScore <= 0.7) {
            rawScore += 0.2; // Boost score for vehicles in sweet spot
        } else if (rawScore < 0.2) {
            rawScore *= 0.5; // Penalize vehicles that are way too large
        }
        
        return rawScore;
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
            // Tính tổng trọng lượng theo KG (convert từ weightTons)
            // weightTons luôn lưu theo TẤN, weightBaseUnit + unit chỉ để hiển thị
            BigDecimal totalWeightInKg = c.placements.stream()
                    .map(pl -> {
                        OrderDetailEntity detail = detailMap.get(pl.box.id);
                        if (detail == null) return BigDecimal.ZERO;

                        // Dùng weightTons (luôn là tấn) để tính toán
                        if (detail.getWeightTons() != null) {
                            return detail.getWeightTons().multiply(BigDecimal.valueOf(1000));
                        }
                        
                        return BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Phát hiện đơn vị chủ đạo dựa trên số lượng kiện (majority rule)
            Map<String, Long> unitCounts = c.placements.stream()
                    .map(pl -> {
                        OrderDetailEntity detail = detailMap.get(pl.box.id);
                        return detail != null ? detail.getUnit() : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            
            String dominantUnit = unitCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Kí");

            // Convert trọng lượng về đơn vị hiển thị phù hợp
            BigDecimal displayLoad;
            String displayUnit;
            
            if (totalWeightInKg.compareTo(BigDecimal.valueOf(1000)) >= 0) {
                // Nếu >= 1000 kg → hiển thị theo Tấn
                displayLoad = totalWeightInKg.divide(BigDecimal.valueOf(1000), 2, java.math.RoundingMode.HALF_UP);
                displayUnit = "Tấn";
            } else {
                // Nếu < 1000 kg → hiển thị theo Kg
                displayLoad = totalWeightInKg;
                displayUnit = "Kg";
            }

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
                    .currentLoad(displayLoad)
                    .currentLoadUnit(displayUnit)
                    // Vehicle dimensions for 3D visualization (in meters)
                    .maxLength(c.rule.getMaxLength())
                    .maxWidth(c.rule.getMaxWidth())
                    .maxHeight(c.rule.getMaxHeight())
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
