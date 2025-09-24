package capstone_project.service.services.billOfLanding.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.utils.PdfUtil;
import static capstone_project.common.constants.PdfSettingConstants.*;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.response.order.BillOfLandingResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.repositories.order.order.OrderDetailRepository;
import capstone_project.repository.repositories.order.order.OrderRepository;
import capstone_project.service.mapper.order.OrderMapper;
import capstone_project.service.mapper.user.CustomerMapper;
import capstone_project.service.mapper.user.UserMapper;
import capstone_project.service.mapper.vehicle.VehicleAssignmentMapper;
import capstone_project.service.services.billOfLanding.BillOfLandingService;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.itextpdf.barcodes.Barcode128;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.properties.AreaBreakType;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillOfLandingServiceImpl implements BillOfLandingService {

    private final ContractEntityService contractEntityService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final OrderDetailRepository orderDetailRepository;

    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;
    private final VehicleAssignmentMapper vehicleAssignmentMapper;
    private final UserMapper userMapper;

    private final String BILL_OF_LANDING_PREFIX = "VN-TRUCKIE-";
    private final OrderRepository orderRepository;
    private static final int MAX_CARGO_ROWS_PER_PAGE = 12;

    @Override
    public BillOfLandingResponse getBillOfLandingById(UUID contractId) {
        log.info("getBillOfLandingById");

        ContractEntity contractEntity = contractEntityService.findEntityById(contractId)
                .orElseThrow(() -> new NotFoundException(
                        "Contract not found with id: " + contractId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        OrderEntity orderEntity = contractEntity.getOrderEntity();
        UserEntity staff = contractEntity.getStaff();
        AddressEntity deliveryAddress = orderEntity.getDeliveryAddress();
        AddressEntity pickupAddress = orderEntity.getPickupAddress();
        CustomerEntity customer = orderEntity.getSender();
        CategoryEntity category = orderEntity.getCategory();

        List<OrderDetailEntity> orderDetails =
                Optional.ofNullable(orderEntity.getOrderDetailEntities())
                        .orElse(Collections.emptyList());

        List<VehicleAssignmentEntity> vehicleAssignments = orderEntity.getOrderDetailEntities().stream()
                .peek(orderDetail -> log.info("OrderDetail: {}", orderDetail))
                .map(OrderDetailEntity::getVehicleAssignmentEntity)
                .filter(Objects::nonNull)
                .toList();

        String datePart = new SimpleDateFormat("yyMMdd").format(new Date());
        String contractShort = contractId.toString().substring(0, 6).toUpperCase();
        String randomPart = RandomStringUtils.randomAlphanumeric(4).toUpperCase();

        String billOfLadingCode = BILL_OF_LANDING_PREFIX + datePart + "-" + contractShort + "-" + randomPart;

        return BillOfLandingResponse.builder()
                .id(UUID.randomUUID().toString())
                .code(billOfLadingCode)
                .staff(userMapper.mapUserResponse(staff))
                .customer(customerMapper.mapCustomerResponse(customer))
                .order(orderMapper.toGetOrderResponse(orderEntity))
                .vehicleAssignmentResponse(vehicleAssignments.stream()
                        .map(vehicleAssignmentMapper::toGetVehicleAssignmentForBillOfLandingResponse)
                        .toList())
                .createdAt(new Date().toString())
                .build();
    }

    @Override
    public Map<String, byte[]> generateBillOfLadingAndCargoManifests(UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode()));

        List<OrderDetailEntity> orderDetails = Optional.ofNullable(order.getOrderDetailEntities())
                .orElse(Collections.emptyList());

        // Collect all unique vehicle assignments for this order
        List<VehicleAssignmentEntity> vehicleAssignments = orderDetails.stream()
                .map(OrderDetailEntity::getVehicleAssignmentEntity)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        String orderCode = order.getOrderCode() != null ? order.getOrderCode() : order.getId().toString();
        String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());

        Map<String, byte[]> result = new HashMap<>();

        // 1. Generate main waybill PDF (one file, contains all order details + list of trips)
        String waybillFileName = String.format("van-don-%s-%s.pdf", orderCode, dateStr);
        byte[] waybillPdf = generateMainWaybillPdf(order, orderDetails, vehicleAssignments);
        result.put(waybillFileName, waybillPdf);

        // 2. Generate cargo manifest for each trip: include STT (TRIP01 ...) and tracking code of vehicle assignment
        for (int i = 0; i < vehicleAssignments.size(); i++) {
            VehicleAssignmentEntity assignment = vehicleAssignments.get(i);
            String tripIndex = String.format("TRIP%02d", i + 1);
            String assignmentTracking = assignment.getTrackingCode() != null ? assignment.getTrackingCode() :
                    (assignment.getVehicleEntity() != null ? assignment.getVehicleEntity().getLicensePlateNumber() : "NA");
            List<OrderDetailEntity> detailsForTrip = orderDetails.stream()
                    .filter(od -> assignment.equals(od.getVehicleAssignmentEntity()))
                    .toList();

            String manifestFileName = String.format("bang-ke-%s-%s-%s-%s.pdf", orderCode, tripIndex, assignmentTracking, dateStr);
            byte[] manifestPdf = generateCargoManifestPdf(order, assignment, detailsForTrip);
            result.put(manifestFileName, manifestPdf);
        }

        return result;
    }

    private byte[] generateMainWaybillPdf(OrderEntity order, List<OrderDetailEntity> orderDetails, List<VehicleAssignmentEntity> vehicleAssignments) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            // slightly smaller margins to help fit content into two pages
            document.setMargins(20, 20, 18, 20);

            // Set font for Vietnamese text and apply standard base font size
            PdfFont font;
            try {
                byte[] fontBytes = new ClassPathResource("/fonts/DejaVuSans.ttf").getInputStream().readAllBytes();
                com.itextpdf.io.font.FontProgram fontProgram =
                        com.itextpdf.io.font.FontProgramFactory.createFont(fontBytes);
                font = PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H,
                        com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
                document.setFont(font);
                new PdfUtil().applyStandardBaseFontSize(document);
            } catch (IOException e) {
                log.error("Error loading font: {}", e.getMessage());
                font = PdfFontFactory.createFont();
                document.setFont(font);
                new PdfUtil().applyStandardBaseFontSize(document);
            }

            String orderCode = order.getOrderCode() != null ? order.getOrderCode() : order.getId().toString();
            String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String billCode = "VAN-DON-" + orderCode + "-" + dateStr;

            // Header and parties
            addHeader(document, billCode); // keeps the main waybill title

            // --- Add barcode at the top: use order tracking code if available, otherwise orderCode ---
            String orderTracking = extractTrackingCodeFromOrder(order);
            try {
                Barcode128 barcode = new Barcode128(pdf);
                barcode.setCode(orderTracking != null ? orderTracking : orderCode);
                PdfFormXObject barcodeObject = barcode.createFormXObject(pdf);
                Image barcodeImage = new Image(barcodeObject).setWidth(220).setHorizontalAlignment(HorizontalAlignment.CENTER);
                document.add(barcodeImage);
                document.add(new Paragraph("").setMarginBottom(6));
            } catch (Exception ex) {
                log.debug("Could not render barcode for main waybill: {}", ex.getMessage());
            }

            UserEntity staff = new UserEntity();
            staff.setFullName("Staff Member");
            staff.setPhoneNumber("N/A");
            try {
                ContractEntity contract = contractEntityService.getContractByOrderId(order.getId()).orElse(null);
                if (contract != null && contract.getStaff() != null) staff = contract.getStaff();
            } catch (Exception ex) {
                log.debug("No contract found for order {}: {}", order.getId(), ex.getMessage());
            }

            addPartiesInformation(document, order.getSender(), order.getReceiverName(), order.getReceiverPhone(), staff);

            // Addresses
            addShippingAddresses(document, order.getPickupAddress(), order.getDeliveryAddress(), order);

            // Cargo (all order details) - use paged implementation to avoid mid-row splits
            addCargoInformationPaged(document, orderDetails);

            // Transport info - list all vehicle assignments
            addTransportInformation(document, vehicleAssignments);

            // Financial summary - try to get contract for details (financials only on main waybill)
            ContractEntity contract = null;
            try {
                contract = contractEntityService.getContractByOrderId(order.getId()).orElse(null);
            } catch (Exception ignored) {}
            addFinancialInformation(document, contract, order);

            addSignatureBlocks(document);
            addFooterInformation(document, billCode);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating main waybill PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate main waybill PDF", e);
        }
    }

    private byte[] generateCargoManifestPdf(OrderEntity order, VehicleAssignmentEntity assignment, List<OrderDetailEntity> detailsForTrip) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(20, 20, 18, 20);

            PdfFont font;
            try {
                byte[] fontBytes = new ClassPathResource("/fonts/DejaVuSans.ttf").getInputStream().readAllBytes();
                com.itextpdf.io.font.FontProgram fontProgram =
                        com.itextpdf.io.font.FontProgramFactory.createFont(fontBytes);
                font = PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H,
                        com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
                document.setFont(font);
                new PdfUtil().applyStandardBaseFontSize(document);
            } catch (IOException e) {
                log.error("Error loading font: {}", e.getMessage());
                font = PdfFontFactory.createFont();
                document.setFont(font);
                new PdfUtil().applyStandardBaseFontSize(document);
            }

            String orderCode = order != null && order.getOrderCode() != null ? order.getOrderCode() : (order != null ? String.valueOf(order.getId()) : "NA");
            String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String assignmentTracking = assignment != null && assignment.getTrackingCode() != null
                    ? assignment.getTrackingCode()
                    : (assignment != null && assignment.getVehicleEntity() != null && assignment.getVehicleEntity().getLicensePlateNumber() != null
                    ? assignment.getVehicleEntity().getLicensePlateNumber()
                    : "NA");

            String manifestCode = "BANG-KE-" + orderCode + "-" + assignmentTracking + "-" + dateStr;
            addHeader(document, manifestCode, false);

            Paragraph manifestTitle = new Paragraph("BẢNG KÊ HÀNG HÓA THEO XE")
                    .setFontSize(TITLE_FONT_SIZE)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(8);
            document.add(manifestTitle);

            // --- Barcode for vehicle assignment ---
            try {
                Barcode128 barcode = new Barcode128(pdf);
                String barcodeValue = assignmentTracking != null && !assignmentTracking.isBlank() ? assignmentTracking : orderCode;
                barcode.setCode(barcodeValue);
                PdfFormXObject barcodeObject = barcode.createFormXObject(pdf);
                Image barcodeImage = new Image(barcodeObject).setWidth(220).setHorizontalAlignment(HorizontalAlignment.CENTER);
                document.add(barcodeImage);
                document.add(new Paragraph("").setMarginBottom(6));
            } catch (Exception ex) {
                log.debug("Could not render barcode for manifest: {}", ex.getMessage());
            }

            // References: main waybill and order info
            String mainWaybillNumber = "VAN-DON-" + orderCode + "-" + dateStr;
            String mainWaybillIssueDate = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
            String orderDate = formatDate(getOrderCreatedDate(order));

            Table refTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            refTable.addCell(new Cell().add(new Paragraph("Tham chiếu vận đơn gốc / Reference Waybill").setBold()));
            refTable.addCell(new Cell().add(new Paragraph("Thông tin đơn hàng / Order information").setBold()));
            refTable.addCell(new Cell().add(new Paragraph("Số vận đơn: " + mainWaybillNumber)));
            refTable.addCell(new Cell().add(new Paragraph("Số đơn hàng: " + orderCode)));
            refTable.addCell(new Cell().add(new Paragraph("Ngày phát hành: " + mainWaybillIssueDate)));
            refTable.addCell(new Cell().add(new Paragraph("Ngày lập đơn: " + orderDate)));
            document.add(refTable);
            document.add(new Paragraph("").setMarginBottom(8));

            // Parties and vehicle info
            UserEntity staff = new UserEntity();
            staff.setFullName("Staff Member");
            staff.setPhoneNumber("N/A");
            try {
                ContractEntity contract = contractEntityService.getContractByOrderId(order.getId()).orElse(null);
                if (contract != null && contract.getStaff() != null) staff = contract.getStaff();
            } catch (Exception ex) {
                log.debug("No contract found for order {}: {}", order.getId(), ex.getMessage());
            }

            // Vehicle and driver info block
            Paragraph vehicleTitle = new Paragraph("2. THÔNG TIN PHƯƠNG TIỆN / VEHICLE INFORMATION")
                    .setBold()
                    .setFontSize(11)
                    .setMarginBottom(4);
            document.add(vehicleTitle);

            Table vehicleTable = new Table(UnitValue.createPercentArray(new float[]{40, 60})).useAllAvailableWidth();
            String plateNumber = (assignment.getVehicleEntity() != null ? assignment.getVehicleEntity().getLicensePlateNumber() : "N/A");
            String driverName = "N/A";
            String driverPhone = "N/A";
            String driverLicense = "N/A";
            if (assignment.getDriver1() != null) {
                try {
                    if (assignment.getDriver1().getUser() != null) {
                        if (assignment.getDriver1().getUser().getFullName() != null) driverName = assignment.getDriver1().getUser().getFullName();
                        if (assignment.getDriver1().getUser().getPhoneNumber() != null) driverPhone = assignment.getDriver1().getUser().getPhoneNumber();
                    }
                } catch (Exception ignored) {}
                driverLicense = extractDriverLicense(assignment.getDriver1());
            }

            vehicleTable.addCell(new Cell().add(new Paragraph("Biển số xe / Plate")));
            vehicleTable.addCell(new Cell().add(new Paragraph(plateNumber)));
            vehicleTable.addCell(new Cell().add(new Paragraph("Tài xế / Driver (Tên, SĐT, GPLX)")));
            vehicleTable.addCell(new Cell().add(new Paragraph(driverName + " - " + driverPhone + " - " + driverLicense)));
            // Estimated departure time: take earliest estimatedStartTime from detailsForTrip if present
            String departureTime = "N/A";
            if (detailsForTrip != null && !detailsForTrip.isEmpty()) {
                OrderDetailEntity d0 = detailsForTrip.get(0);
                if (d0.getEstimatedStartTime() != null) {
                    departureTime = d0.getEstimatedStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                }
            }
            vehicleTable.addCell(new Cell().add(new Paragraph("Thời gian xuất phát dự kiến")));
            vehicleTable.addCell(new Cell().add(new Paragraph(departureTime)));
            document.add(vehicleTable);
            document.add(new Paragraph("").setMarginBottom(6));

            // Cargo details: only detailsForTrip
            addCargoInformationPaged(document, detailsForTrip);

            // Totals summary (count, total weight, total volume)
            DecimalFormat df = new DecimalFormat("#,###.##");
            int totalPackages = detailsForTrip.size();
            BigDecimal totalWeight = BigDecimal.ZERO;
            BigDecimal totalVolume = BigDecimal.ZERO;
            for (OrderDetailEntity d : detailsForTrip) {
                if (d.getWeight() != null) totalWeight = totalWeight.add(d.getWeight());
                // Try to detect volume via reflection fields if present (length*width*height / 1e6..) - fallback to zero
                // For now, use 0 if not present
            }

            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            summaryTable.addCell(new Cell().add(new Paragraph("Tổng số kiện hàng trên xe / Total packages").setBold()));
            summaryTable.addCell(new Cell().add(new Paragraph(String.valueOf(totalPackages)).setBold()));
            summaryTable.addCell(new Cell().add(new Paragraph("Tổng trọng lượng (kg) / Total weight (kg)").setBold()));
            summaryTable.addCell(new Cell().add(new Paragraph(df.format(totalWeight)).setBold()));
            summaryTable.addCell(new Cell().add(new Paragraph("Tổng thể tích (m³) / Total volume (m³)").setBold()));
            summaryTable.addCell(new Cell().add(new Paragraph(df.format(totalVolume)).setBold()));
            document.add(summaryTable);
            document.add(new Paragraph("").setMarginBottom(8));

            // Signatures
            addSignatureBlocks(document);

            // Footer minimal reference (manifest code)
            addFooterInformation(document, manifestCode);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating cargo manifest PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate cargo manifest PDF", e);
        }
    }

    private void addHeader(Document document, String billOfLadingCode) {
        addHeader(document, billOfLadingCode, true);
    }

    private void addHeader(Document document, String billOfLadingCode, boolean includeMainTitle) {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{65, 35})).useAllAvailableWidth();

        Cell companyCell = new Cell();
        companyCell.setBorder(null);
        companyCell.setPadding(12);
        Paragraph companyName = new Paragraph("TRUCKIE LOGISTICS CO., LTD")
                .setBold()
                .setFontSize(MAIN_TITLE_FONT_SIZE);
        Paragraph companyAddress = new Paragraph("123 Logistics Street, District 1, Ho Chi Minh City, Vietnam")
                .setFontSize(BASE_FONT_SIZE);
        Paragraph companyContact = new Paragraph("Tel: +84 28 1234 5678 | Email: info@truckie.com")
                .setFontSize(BASE_FONT_SIZE);
        Paragraph companyTaxId = new Paragraph("MST/Tax ID: 0123456789")
                .setFontSize(BASE_FONT_SIZE);

        companyCell.add(companyName);
        companyCell.add(companyAddress);
        companyCell.add(companyContact);
        companyCell.add(companyTaxId);

        Cell documentInfoCell = new Cell();
        documentInfoCell.setBorder(null);
        documentInfoCell.setPadding(12);
        Paragraph documentNumber = new Paragraph("Mã vận đơn / Waybill No.:")
                .setFontSize(BASE_FONT_SIZE)
                .setBold();
        Paragraph documentNumberValue = new Paragraph(billOfLadingCode)
                .setFontSize(BASE_FONT_SIZE)
                .setBold()
                .setFontColor(ColorConstants.RED);
        Paragraph issueDateLabel = new Paragraph("Ngày phát hành / Issue Date:")
                .setFontSize(BASE_FONT_SIZE);
        Paragraph issueDateValue = new Paragraph(new SimpleDateFormat("dd/MM/yyyy").format(new Date()))
                .setFontSize(BASE_FONT_SIZE);

        documentInfoCell.add(documentNumber);
        documentInfoCell.add(documentNumberValue);
        documentInfoCell.add(issueDateLabel);
        documentInfoCell.add(issueDateValue);

        headerTable.addCell(companyCell);
        headerTable.addCell(documentInfoCell);

        document.add(headerTable);
        document.add(new Paragraph("").setMarginBottom(10));

        if (includeMainTitle) {
            Paragraph title = new Paragraph("VẬN ĐƠN ĐƯỜNG BỘ / ROAD TRANSPORT WAYBILL")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(TITLE_FONT_SIZE)
                    .setBold()
                    .setMarginBottom(12);
            document.add(title);
        }
    }

    private void addPartiesInformation(Document document, CustomerEntity sender, String receiverName, String receiverPhone, UserEntity staff) {
        Paragraph sectionTitle = new Paragraph("1. THÔNG TIN CÁC BÊN / PARTIES INFORMATION")
                .setBold()
                .setFontSize(SECTION_TITLE_FONT_SIZE)
                .setMarginBottom(5);

        Table partiesTable = new Table(UnitValue.createPercentArray(2)).useAllAvailableWidth();
        partiesTable.setBorder(new SolidBorder(ColorConstants.BLACK, 1));
        partiesTable.setKeepTogether(true);

        Cell senderHeaderCell = new Cell();
        senderHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        senderHeaderCell.setBold();
        senderHeaderCell.add(new Paragraph("NGƯỜI GỬI HÀNG / SENDER").setFontSize(TABLE_HEADER_FONT_SIZE));
        partiesTable.addCell(senderHeaderCell);

        Cell receiverHeaderCell = new Cell();
        receiverHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        receiverHeaderCell.setBold();
        receiverHeaderCell.add(new Paragraph("NGƯỜI NHẬN HÀNG / RECEIVER").setFontSize(TABLE_HEADER_FONT_SIZE));
        partiesTable.addCell(receiverHeaderCell);

        Cell senderDetailsCell = new Cell();
        String senderFullName = sender.getUser().getFullName();
        String senderPhone = sender.getUser().getPhoneNumber();
        String senderEmail = sender.getUser().getEmail();

        senderDetailsCell.add(new Paragraph("Tên / Name: " + senderFullName).setBold().setFontSize(TABLE_CELL_FONT_SIZE));
        senderDetailsCell.add(new Paragraph("Điện thoại / Phone: " + senderPhone).setFontSize(TABLE_CELL_FONT_SIZE));
        senderDetailsCell.add(new Paragraph("Email: " + senderEmail).setFontSize(TABLE_CELL_FONT_SIZE));
        partiesTable.addCell(senderDetailsCell);

        Cell receiverDetailsCell = new Cell();
        receiverDetailsCell.add(new Paragraph("Tên / Name: " + receiverName).setBold().setFontSize(TABLE_CELL_FONT_SIZE));
        receiverDetailsCell.add(new Paragraph("Điện thoại / Phone: " + receiverPhone).setFontSize(TABLE_CELL_FONT_SIZE));
        partiesTable.addCell(receiverDetailsCell);

        Div partiesBlock = new Div().setKeepTogether(true);
        partiesBlock.add(sectionTitle);
        partiesBlock.add(partiesTable);
        document.add(partiesBlock);

        Paragraph transportCompanyTitle = new Paragraph("ĐƠN VỊ VẬN TẢI / TRANSPORT COMPANY")
                .setBold()
                .setFontSize(SECTION_TITLE_FONT_SIZE)
                .setMarginTop(8)
                .setKeepTogether(true);

        Table companyTable = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        companyTable.setBorder(new SolidBorder(ColorConstants.BLACK, 1));
        companyTable.setKeepTogether(true);

        Cell companyCell = new Cell();
        companyCell.add(new Paragraph("Tên công ty / Company name: TRUCKIE LOGISTICS CO., LTD").setBold().setFontSize(TABLE_CELL_FONT_SIZE));
        companyCell.add(new Paragraph("Địa chỉ / Address: 123 Logistics Street, District 1, Ho Chi Minh City, Vietnam").setFontSize(TABLE_CELL_FONT_SIZE));
        companyCell.add(new Paragraph("Mã số thuế / Tax ID: 0123456789").setFontSize(TABLE_CELL_FONT_SIZE));
        companyCell.add(new Paragraph("Người phụ trách / Staff in charge: " + staff.getFullName() + " (" + staff.getPhoneNumber() + ")").setFontSize(TABLE_CELL_FONT_SIZE));

        companyTable.addCell(companyCell);
        Div companyBlock = new Div().setKeepTogether(true);
        companyBlock.add(transportCompanyTitle);
        companyBlock.add(companyTable);
        document.add(companyBlock);
        document.add(new Paragraph("").setMarginBottom(8));
    }

    private void addShippingAddresses(Document document, AddressEntity pickupAddress, AddressEntity deliveryAddress, OrderEntity order) {
        Paragraph sectionTitle = new Paragraph("2. THÔNG TIN VẬN CHUYỂN / SHIPPING INFORMATION")
                .setBold()
                .setFontSize(SECTION_TITLE_FONT_SIZE)
                .setMarginBottom(5)
                .setKeepTogether(true);

        Table addressTable = new Table(UnitValue.createPercentArray(2)).useAllAvailableWidth();
        addressTable.setBorder(new SolidBorder(ColorConstants.BLACK, 1));
        addressTable.setKeepTogether(true);

        // Pickup address
        Cell pickupHeaderCell = new Cell();
        pickupHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        pickupHeaderCell.setBold();
        pickupHeaderCell.add(new Paragraph("ĐỊA ĐIỂM LẤY HÀNG / PICKUP LOCATION"));
        addressTable.addCell(pickupHeaderCell);

        // Delivery address
        Cell deliveryHeaderCell = new Cell();
        deliveryHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        deliveryHeaderCell.setBold();
        deliveryHeaderCell.add(new Paragraph("ĐỊA ĐIỂM GIAO HÀNG / DELIVERY LOCATION"));
        addressTable.addCell(deliveryHeaderCell);

        // Pickup details
        Cell pickupDetailsCell = new Cell();
        // Use the available fields based on your AddressEntity structure
        String pickupAddressStr = pickupAddress.getStreet() + ", " +
                pickupAddress.getWard() + ", " +
                pickupAddress.getProvince(); // No district property found
        pickupDetailsCell.add(new Paragraph("Địa chỉ / Address: " + pickupAddressStr));

        // Get estimated start time from the first order detail's estimatedStartTime if available
        String pickupTimeStr = "N/A";
        if (order.getOrderDetailEntities() != null && !order.getOrderDetailEntities().isEmpty()) {
            OrderDetailEntity firstDetail = order.getOrderDetailEntities().get(0);
            LocalDateTime estimatedStartTime = firstDetail.getEstimatedStartTime();
            if (estimatedStartTime != null) {
                pickupTimeStr = estimatedStartTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
        }
        pickupDetailsCell.add(new Paragraph("Thời gian lấy hàng dự kiến / Pickup time: " + pickupTimeStr));
        addressTable.addCell(pickupDetailsCell);

        // Delivery details
        Cell deliveryDetailsCell = new Cell();
        String deliveryAddressStr = deliveryAddress.getStreet() + ", " +
                deliveryAddress.getWard() + ", " +
                deliveryAddress.getProvince();
        deliveryDetailsCell.add(new Paragraph("Địa chỉ / Address: " + deliveryAddressStr));

        // Get estimated end time from the first order detail's estimatedEndTime if available
        String deliveryTimeStr = "N/A";
        if (order.getOrderDetailEntities() != null && !order.getOrderDetailEntities().isEmpty()) {
            OrderDetailEntity firstDetail = order.getOrderDetailEntities().get(0);
            LocalDateTime estimatedEndTime = firstDetail.getEstimatedEndTime();
            if (estimatedEndTime != null) {
                deliveryTimeStr = estimatedEndTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
        }
        deliveryDetailsCell.add(new Paragraph("Thời gian giao hàng dự kiến / Expected delivery time: " + deliveryTimeStr));
        addressTable.addCell(deliveryDetailsCell);

        Div addressBlock = new Div().setKeepTogether(true);
        addressBlock.add(sectionTitle);
        addressBlock.add(addressTable);
        document.add(addressBlock);
        document.add(new Paragraph("").setMarginBottom(8)); // Reduced from 10
    }

    private void addCargoInformationPaged(Document document, List<OrderDetailEntity> orderDetails) {
        if (orderDetails == null) orderDetails = Collections.emptyList();

        DecimalFormat df = new DecimalFormat("#,###.##");
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        for (OrderDetailEntity d : orderDetails) {
            if (d.getWeight() != null) totalWeight = totalWeight.add(d.getWeight());
        }

        int totalRows = orderDetails.size();
        int pages = Math.max(1, (int) Math.ceil((double) totalRows / MAX_CARGO_ROWS_PER_PAGE));

        for (int p = 0; p < pages; p++) {
            int start = p * MAX_CARGO_ROWS_PER_PAGE;
            int end = Math.min(start + MAX_CARGO_ROWS_PER_PAGE, totalRows);

            Paragraph sectionTitle;
            if (p == 0) {
                sectionTitle = new Paragraph("3. THÔNG TIN HÀNG HÓA / CARGO INFORMATION")
                        .setBold()
                        .setFontSize(SECTION_TITLE_FONT_SIZE)
                        .setMarginBottom(5)
                        .setKeepTogether(true);
            } else {
                sectionTitle = new Paragraph("3. THÔNG TIN HÀNG HÓA (Tiếp theo) / CARGO INFORMATION (continued)")
                        .setBold()
                        .setFontSize(SECTION_TITLE_FONT_SIZE)
                        .setMarginBottom(5)
                        .setKeepTogether(true);
            }

            Table cargoTable = new Table(UnitValue.createPercentArray(new float[]{5, 30, 15, 15, 15, 20})).useAllAvailableWidth();
            cargoTable.setKeepTogether(true);

            Cell noHeaderCell = new Cell().add(new Paragraph("STT").setBold().setFontSize(TABLE_HEADER_FONT_SIZE));
            Cell descHeaderCell = new Cell().add(new Paragraph("Mô tả hàng hóa / Description").setBold().setFontSize(TABLE_HEADER_FONT_SIZE));
            Cell quantityHeaderCell = new Cell().add(new Paragraph("Số lượng / Quantity").setBold().setFontSize(TABLE_HEADER_FONT_SIZE));
            Cell weightHeaderCell = new Cell().add(new Paragraph("Khối lượng (kg) / Weight").setBold().setFontSize(TABLE_HEADER_FONT_SIZE));
            Cell volumeHeaderCell = new Cell().add(new Paragraph("Thể tích (m³) / Volume").setBold().setFontSize(TABLE_HEADER_FONT_SIZE));
            Cell valueHeaderCell = new Cell().add(new Paragraph("Giá trị / Value (VND)").setBold().setFontSize(TABLE_HEADER_FONT_SIZE));

            noHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            descHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            quantityHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            weightHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            volumeHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            valueHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);

            cargoTable.addCell(noHeaderCell);
            cargoTable.addCell(descHeaderCell);
            cargoTable.addCell(quantityHeaderCell);
            cargoTable.addCell(weightHeaderCell);
            cargoTable.addCell(volumeHeaderCell);
            cargoTable.addCell(valueHeaderCell);

            for (int i = start; i < end; i++) {
                OrderDetailEntity detail = orderDetails.get(i);

                Cell idxCell = new Cell().add(new Paragraph(String.valueOf(i + 1)).setFontSize(TABLE_CELL_FONT_SIZE));
                idxCell.setKeepTogether(true);
                cargoTable.addCell(idxCell);

                String desc = detail.getDescription() != null ? detail.getDescription() : "";
                Cell descCell = new Cell().add(new Paragraph(desc).setFontSize(TABLE_CELL_FONT_SIZE));
                descCell.setKeepTogether(true);
                cargoTable.addCell(descCell);

                Integer qty = 1;
                try {
                    Method m = detail.getClass().getMethod("getQuantity");
                    Object v = m.invoke(detail);
                    if (v instanceof Number) qty = ((Number) v).intValue();
                } catch (Exception ignored) {}
                Cell qtyCell = new Cell().add(new Paragraph(String.valueOf(qty)).setFontSize(TABLE_CELL_FONT_SIZE));
                qtyCell.setKeepTogether(true);
                cargoTable.addCell(qtyCell);

                BigDecimal weight = detail.getWeight() != null ? detail.getWeight() : BigDecimal.ZERO;
                Cell weightCell = new Cell().add(new Paragraph(df.format(weight)).setFontSize(TABLE_CELL_FONT_SIZE));
                weightCell.setKeepTogether(true);
                cargoTable.addCell(weightCell);

                BigDecimal volume = BigDecimal.ZERO;
                Cell volCell = new Cell().add(new Paragraph(df.format(volume)).setFontSize(TABLE_CELL_FONT_SIZE));
                volCell.setKeepTogether(true);
                cargoTable.addCell(volCell);

                BigDecimal value = BigDecimal.ZERO;
                Cell valCell = new Cell().add(new Paragraph(df.format(value)).setFontSize(TABLE_CELL_FONT_SIZE));
                valCell.setKeepTogether(true);
                cargoTable.addCell(valCell);
            }

            if (p == pages - 1) {
                Cell totalLabelCell = new Cell(1, 2).add(new Paragraph("TỔNG CỘNG / TOTAL").setBold().setFontSize(TABLE_HEADER_FONT_SIZE));
                totalLabelCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                cargoTable.addCell(totalLabelCell);

                int totalQuantity = orderDetails.size();
                Cell totalQuantityCell = new Cell().add(new Paragraph(String.valueOf(totalQuantity)).setBold().setFontSize(TABLE_CELL_FONT_SIZE));
                totalQuantityCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                cargoTable.addCell(totalQuantityCell);

                Cell totalWeightCell = new Cell().add(new Paragraph(df.format(totalWeight)).setBold().setFontSize(TABLE_CELL_FONT_SIZE));
                totalWeightCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                cargoTable.addCell(totalWeightCell);

                Cell totalVolumeCell = new Cell().add(new Paragraph(df.format(totalVolume)).setBold().setFontSize(TABLE_CELL_FONT_SIZE));
                totalVolumeCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                cargoTable.addCell(totalVolumeCell);

                BigDecimal totalValue = BigDecimal.ZERO;
                Cell totalValueCell = new Cell().add(new Paragraph(df.format(totalValue)).setBold().setFontSize(TABLE_CELL_FONT_SIZE));
                totalValueCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
                cargoTable.addCell(totalValueCell);
            }

            Div cargoBlock = new Div().setKeepTogether(true);
            cargoBlock.add(sectionTitle);
            cargoBlock.add(cargoTable);
            document.add(cargoBlock);

            if (p < pages - 1) {
                // use fully-qualified enum to avoid symbol resolution issues
                document.add(new AreaBreak(com.itextpdf.layout.properties.AreaBreakType.NEXT_PAGE));
            }
        }

        // Add cargo condition and special instructions once (after all chunks)
        Paragraph cargoConditionTitle = new Paragraph("Tình trạng hàng hóa / Cargo condition:")
                .setBold()
                .setMarginTop(5)
                .setFontSize(TABLE_CELL_FONT_SIZE)
                .setKeepTogether(true);
        document.add(cargoConditionTitle);

        Table conditionTable = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        conditionTable.setKeepTogether(true);
        Cell conditionCell = new Cell();
        conditionCell.setHeight(40);
        conditionCell.add(new Paragraph("□ Nguyên vẹn / Intact   □ Dễ vỡ / Fragile   □ Cần giữ lạnh / Temperature controlled   □ Khác / Other: _____________").setFontSize(TABLE_CELL_FONT_SIZE));
        conditionTable.addCell(conditionCell);
        document.add(conditionTable);

        Paragraph specialInstructionsTitle = new Paragraph("Chỉ dẫn đặc biệt / Special instructions:")
                .setBold()
                .setMarginTop(5)
                .setFontSize(TABLE_CELL_FONT_SIZE)
                .setKeepTogether(true);
        document.add(specialInstructionsTitle);

        Table instructionsTable = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        instructionsTable.setKeepTogether(true);
        Cell instructionsCell = new Cell();
        instructionsCell.setHeight(40);
        instructionsCell.add(new Paragraph("").setFontSize(TABLE_CELL_FONT_SIZE));
        instructionsTable.addCell(instructionsCell);
        document.add(instructionsTable);

        document.add(new Paragraph("").setMarginBottom(10));
    }

    private void addTransportInformation(Document document, List<VehicleAssignmentEntity> vehicleAssignments) {
        Paragraph sectionTitle = new Paragraph("4. THÔNG TIN PHƯƠNG TIỆN / TRANSPORT INFORMATION")
                .setBold()
                .setFontSize(11)
                .setMarginBottom(5)
                .setKeepTogether(true);

        Table transportTable = new Table(UnitValue.createPercentArray(new float[]{5, 20, 20, 20, 35})).useAllAvailableWidth();
        transportTable.setKeepTogether(true);

        // Header row
        Cell noHeaderCell = new Cell().add(new Paragraph("STT").setBold());
        Cell vehicleTypeHeaderCell = new Cell().add(new Paragraph("Loại xe / Vehicle type").setBold());
        Cell plateNumberHeaderCell = new Cell().add(new Paragraph("Biển số xe / Plate number").setBold());
        Cell driverHeaderCell = new Cell().add(new Paragraph("Tài xế / Driver").setBold());
        Cell routeHeaderCell = new Cell().add(new Paragraph("Tuyến đường / Route").setBold());

        // Style header cells
        noHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        vehicleTypeHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        plateNumberHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        driverHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        routeHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);

        transportTable.addCell(noHeaderCell);
        transportTable.addCell(vehicleTypeHeaderCell);
        transportTable.addCell(plateNumberHeaderCell);
        transportTable.addCell(driverHeaderCell);
        transportTable.addCell(routeHeaderCell);

        // Add vehicle assignment rows
        if (vehicleAssignments.isEmpty()) {
            Cell emptyCell = new Cell(1, 5);
            emptyCell.add(new Paragraph("Chưa có thông tin phương tiện / No vehicle information available"));
            transportTable.addCell(emptyCell);
        } else {
            for (int i = 0; i < vehicleAssignments.size(); i++) {
                VehicleAssignmentEntity assignment = vehicleAssignments.get(i);
                VehicleEntity vehicle = assignment.getVehicleEntity(); // Using the correct property name

                transportTable.addCell(new Cell().add(new Paragraph(String.valueOf(i + 1))));

                // Vehicle type - using the correct property names or defaults
                String vehicleType = "N/A";
                if (vehicle != null && vehicle.getVehicleTypeEntity() != null) {
                    vehicleType = "Standard";
                }
                transportTable.addCell(new Cell().add(new Paragraph(vehicleType)));

                // Plate number - using the correct property name
                String plateNumber = vehicle != null ? vehicle.getLicensePlateNumber() : "N/A"; // Fixed: getLicensePlate() -> getLicensePlateNumber()
                transportTable.addCell(new Cell().add(new Paragraph(plateNumber)));

                // Driver info - using the correct property names and a safe approach
                String driverInfo = "N/A";
                if (assignment.getDriver1() != null && assignment.getDriver1().getUser() != null) {
                    String driverName = assignment.getDriver1().getUser().getFullName();
                    String driverPhone = assignment.getDriver1().getUser().getPhoneNumber(); // Fixed: getPhone() -> getPhoneNumber()
                    driverInfo = driverName + " (" + driverPhone + ")";
                }
                transportTable.addCell(new Cell().add(new Paragraph(driverInfo)));

                // Route info (simplified)
                transportTable.addCell(new Cell().add(new Paragraph("Standard route")));
            }
        }

        Div transportBlock = new Div().setKeepTogether(true);
        transportBlock.add(sectionTitle);
        transportBlock.add(transportTable);
        document.add(transportBlock);

        document.add(new Paragraph("").setMarginBottom(10));
    }

    private void addFinancialInformation(Document document, ContractEntity contract, OrderEntity order) {
        Paragraph sectionTitle = new Paragraph("5. THÔNG TIN TÀI CHÍNH / FINANCIAL INFORMATION")
                .setBold()
                .setFontSize(11)
                .setMarginBottom(5)
                .setKeepTogether(true);

        Table financialTable = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
        financialTable.setKeepTogether(true);

        // Header
        Cell serviceCostHeaderCell = new Cell().add(new Paragraph("Mô tả / Description").setBold());
        Cell amountHeaderCell = new Cell().add(new Paragraph("Số tiền / Amount (VND)").setBold());
        serviceCostHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        amountHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        financialTable.addCell(serviceCostHeaderCell);
        financialTable.addCell(amountHeaderCell);

        DecimalFormat df = new DecimalFormat("#,###.##");
        BigDecimal totalAmount = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;

        // Use enhanced transaction summarization that compares amount and createdAt vs order total
        BigDecimal depositAmount = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        if (contract != null) {
            Map<String, BigDecimal> txnSummary = sumTransactionsFromContract(contract, totalAmount);
            if (txnSummary != null) {
                depositAmount = txnSummary.getOrDefault("deposit", BigDecimal.ZERO);
                totalPaid = txnSummary.getOrDefault("paid", BigDecimal.ZERO);
            }
        }

        // If no explicit deposit found but there are payments, assume deposit equals payments (best-effort)
        if (depositAmount.compareTo(BigDecimal.ZERO) == 0 && totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            depositAmount = totalPaid;
        }

        BigDecimal remainingAmount = totalAmount.subtract(depositAmount);

        financialTable.addCell(new Cell().add(new Paragraph("Cước vận chuyển / Freight charge")));
        financialTable.addCell(new Cell().add(new Paragraph(df.format(totalAmount))));

        financialTable.addCell(new Cell().add(new Paragraph("Đã thanh toán (tiền đặt cọc) / Paid (deposit)")));
        financialTable.addCell(new Cell().add(new Paragraph(df.format(depositAmount))));

        Cell totalLabelCell = new Cell().add(new Paragraph("Còn lại / Remaining").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY);
        Cell totalAmountCell = new Cell().add(new Paragraph(df.format(remainingAmount)).setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY);
        financialTable.addCell(totalLabelCell);
        financialTable.addCell(totalAmountCell);

        Div financialBlock = new Div().setKeepTogether(true);
        financialBlock.add(sectionTitle);
        financialBlock.add(financialTable);
        document.add(financialBlock);

        // Payment method - default tick on bank transfer
        Paragraph paymentTitle = new Paragraph("Hình thức thanh toán / Payment method:")
                .setBold()
                .setMarginTop(5);
        document.add(paymentTitle);

        // Use checked box for bank transfer
        Paragraph paymentChoices = new Paragraph("☐ Thanh toán online / Online payment   ☑ Chuyển khoản / Bank transfer   ☐ Tiền mặt / Cash");
        document.add(paymentChoices);

        document.add(new Paragraph("").setMarginBottom(10));
    }

    private void addSignatureBlocks(Document document) {
        Paragraph sectionTitle = new Paragraph("6. XÁC NHẬN / CONFIRMATION")
                .setBold()
                .setFontSize(SECTION_TITLE_FONT_SIZE)
                .setMarginBottom(5)
                .setKeepTogether(true);

        Table signaturesTable = new Table(UnitValue.createPercentArray(3)).useAllAvailableWidth();
        signaturesTable.setKeepTogether(true);

        Cell senderSignatureHeaderCell = new Cell();
        senderSignatureHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        senderSignatureHeaderCell.setBold();
        senderSignatureHeaderCell.add(new Paragraph("NGƯỜI GỬI / SENDER").setFontSize(TABLE_HEADER_FONT_SIZE));
        signaturesTable.addCell(senderSignatureHeaderCell);

        // Transport company signature
        Cell companySignatureHeaderCell = new Cell();
        companySignatureHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        companySignatureHeaderCell.setBold();
        companySignatureHeaderCell.add(new Paragraph("ĐƠN VỊ VẬN TẢI / CARRIER").setFontSize(TABLE_HEADER_FONT_SIZE));
        signaturesTable.addCell(companySignatureHeaderCell);

        // Receiver signature
        Cell receiverSignatureHeaderCell = new Cell();
        receiverSignatureHeaderCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        receiverSignatureHeaderCell.setBold();
        receiverSignatureHeaderCell.add(new Paragraph("NGƯỜI NHẬN / RECEIVER"));
        signaturesTable.addCell(receiverSignatureHeaderCell);

        // Signature spaces
        Cell senderSignatureCell = new Cell();
        senderSignatureCell.setHeight(80);
        senderSignatureCell.add(new Paragraph("Chữ ký, họ tên / Signature, name").setFontSize(TABLE_CELL_FONT_SIZE * 0.9f));
        signaturesTable.addCell(senderSignatureCell);

        Cell companySignatureCell = new Cell();
        companySignatureCell.setHeight(80);
        companySignatureCell.add(new Paragraph("Chữ ký, họ tên, đóng dấu / Signature, name, stamp").setFontSize(TABLE_CELL_FONT_SIZE * 0.9f));
        signaturesTable.addCell(companySignatureCell);

        Cell receiverSignatureCell = new Cell();
        receiverSignatureCell.setHeight(80);
        receiverSignatureCell.add(new Paragraph("Chữ ký, họ tên / Signature, name").setFontSize(8));
        signaturesTable.addCell(receiverSignatureCell);

        Div sigBlock = new Div().setKeepTogether(true);
        sigBlock.add(sectionTitle);
        sigBlock.add(signaturesTable);
        document.add(sigBlock);

        document.add(new Paragraph("").setMarginBottom(8));
    }

    private void addFooterInformation(Document document, String billOfLadingCode) {
        Paragraph qrTitle = new Paragraph("Quét mã QR để tra cứu / Scan QR code to track:")
                .setFontSize(FOOTER_FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(qrTitle);

        Paragraph qrPlaceholder = new Paragraph("[QR Code for: " + billOfLadingCode + "]")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(FOOTER_FONT_SIZE + 2);
        document.add(qrPlaceholder);

        Paragraph termsTitle = new Paragraph("ĐIỀU KHOẢN VẬN CHUYỂN / TERMS AND CONDITIONS")
                .setBold()
                .setFontSize(FOOTER_FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(termsTitle);

        Paragraph terms = new Paragraph(/* same text */)
                .setFontSize(FOOTER_FONT_SIZE - 2)
                .setTextAlignment(TextAlignment.JUSTIFIED);
        document.add(terms);

        Paragraph footer = new Paragraph("TRUCKIE LOGISTICS - Đồng hành cùng doanh nghiệp / Your reliable logistics partner")
                .setFontSize(FOOTER_FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(footer);
    }

    private String extractDriverLicense(Object driverEntity) {
        if (driverEntity == null) return "N/A";
        String[] methodNames = new String[] {"getDrivingLicenseNumber", "getLicenseNumber", "getDrivingLicense", "getDriverLicense", "getLicenseNo", "getLicense"};
        for (String mn : methodNames) {
            try {
                Method m = driverEntity.getClass().getMethod(mn);
                Object v = m.invoke(driverEntity);
                if (v != null) return String.valueOf(v);
            } catch (Exception ignored) {}
        }
        // try user object inside driver entity
        try {
            Method gu = driverEntity.getClass().getMethod("getUser");
            Object user = gu.invoke(driverEntity);
            if (user != null) {
                for (String mn : methodNames) {
                    try {
                        Method m = user.getClass().getMethod(mn);
                        Object v = m.invoke(user);
                        if (v != null) return String.valueOf(v);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return "N/A";
    }

    private Object getOrderCreatedDate(OrderEntity order) {
        if (order == null) return null;
        // Try common getters
        String[] names = new String[] {"getCreatedAt", "getCreatedDate", "getCreatedTime", "getCreatedOn"};
        for (String n : names) {
            try {
                Method m = order.getClass().getMethod(n);
                return m.invoke(order);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String formatDate(Object dt) {
        if (dt == null) return "N/A";
        try {
            if (dt instanceof Date) {
                return new SimpleDateFormat("dd/MM/yyyy").format((Date) dt);
            }
            if (dt instanceof LocalDateTime) {
                return ((LocalDateTime) dt).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            return String.valueOf(dt);
        } catch (Exception e) {
            return String.valueOf(dt);
        }
    }

    private String extractTrackingCodeFromOrder(OrderEntity order) {
        if (order == null) return null;
        // Try common tracking getters
        String[] names = new String[] {"getTrackingCode", "getTracking", "getTrackingId", "getOrderTrackingCode", "getTrackingCodeValue"};
        for (String n : names) {
            try {
                Method m = order.getClass().getMethod(n);
                Object v = m.invoke(order);
                if (v != null) return String.valueOf(v);
            } catch (Exception ignored) {}
        }
        // fallback to orderCode
        return order.getOrderCode() != null ? order.getOrderCode() : String.valueOf(order.getId());
    }

    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> sumTransactionsFromContract(ContractEntity contract, BigDecimal totalOrderAmount) {
        Map<String, BigDecimal> result = new HashMap<>();
        BigDecimal deposit = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;

        try {
            Method gt = contract.getClass().getMethod("getTransactions");
            Object txObj = gt.invoke(contract);
            if (txObj instanceof Collection) {
                Collection<?> txs = (Collection<?>) txObj;
                // Build list of lightweight tx records with amount, createdAt, type/desc
                class TxRec { BigDecimal amount; Date createdAt; String type; Object raw; }
                List<TxRec> list = new ArrayList<>();
                for (Object tx : txs) {
                    BigDecimal amount = BigDecimal.ZERO;
                    Date createdAt = null;
                    String typeStr = "";

                    try {
                        Method mAmt = tx.getClass().getMethod("getAmount");
                        Object va = mAmt.invoke(tx);
                        if (va instanceof BigDecimal) amount = (BigDecimal) va;
                        else if (va instanceof Number) amount = BigDecimal.valueOf(((Number) va).doubleValue());
                    } catch (Exception ignored) {}

                    // createdAt variations
                    String[] dateMethods = new String[] {"getCreatedAt", "getCreatedDate", "getCreatedTime", "getCreatedOn", "getTime"};
                    for (String dm : dateMethods) {
                        try {
                            Method m = tx.getClass().getMethod(dm);
                            Object dv = m.invoke(tx);
                            if (dv instanceof Date) { createdAt = (Date) dv; break; }
                            if (dv instanceof LocalDateTime) { createdAt = Date.from(((LocalDateTime) dv).atZone(ZoneId.systemDefault()).toInstant()); break; }
                        } catch (Exception ignored) {}
                    }

                    // type/desc
                    String[] typeMethods = new String[] {"getType", "getTransactionType", "getCategory", "getNote", "getDescription"};
                    for (String tm : typeMethods) {
                        try {
                            Method m = tx.getClass().getMethod(tm);
                            Object tv = m.invoke(tx);
                            if (tv != null) { typeStr = String.valueOf(tv); break; }
                        } catch (Exception ignored) {}
                    }

                    TxRec r = new TxRec();
                    r.amount = amount != null ? amount : BigDecimal.ZERO;
                    r.createdAt = createdAt;
                    r.type = typeStr != null ? typeStr : "";
                    r.raw = tx;
                    list.add(r);
                }

                // sort by createdAt asc (nulls last)
                list.sort(Comparator.comparing((TxRec r) -> r.createdAt, Comparator.nullsLast(Date::compareTo)));

                // Determine a reference date (median) to split early vs late transactions
                Date medianDate = null;
                if (!list.isEmpty()) {
                    medianDate = list.get(list.size() / 2).createdAt;
                }

                // Heuristics:
                // - If tx type contains "deposit" -> deposit
                // - Else if tx is earlier than medianDate AND amount is relatively small vs order (<= 35%) -> deposit
                // - Else if amount < totalOrderAmount and createdAt is earlier than latest -> deposit
                // Otherwise treat as payment (paid)
                Date latestDate = null;
                for (TxRec r : list) {
                    if (r.createdAt != null && (latestDate == null || r.createdAt.after(latestDate))) latestDate = r.createdAt;
                }

                for (TxRec r : list) {
                    boolean isDeposit = false;
                    String t = r.type != null ? r.type.toLowerCase() : "";
                    if (t.contains("deposit") || t.contains("cọc")) {
                        isDeposit = true;
                    } else if (totalOrderAmount != null && totalOrderAmount.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal pct = BigDecimal.ZERO;
                        try {
                            pct = r.amount.divide(totalOrderAmount, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100"));
                        } catch (Exception ignored) {}
                        // amount <= 35% of order AND earlier than median -> deposit
                        if (medianDate != null && r.createdAt != null && r.createdAt.before(medianDate) && r.amount.compareTo(totalOrderAmount.multiply(new BigDecimal("0.35"))) <= 0) {
                            isDeposit = true;
                        } else if (latestDate != null && r.createdAt != null && r.createdAt.before(latestDate) && r.amount.compareTo(totalOrderAmount) < 0) {
                            // earlier than the last payment and amount < total -> likely deposit
                            isDeposit = true;
                        }
                    } else {
                        // no total amount: treat earliest transactions as deposits
                        if (medianDate != null && r.createdAt != null && r.createdAt.before(medianDate)) isDeposit = true;
                    }

                    if (isDeposit) deposit = deposit.add(r.amount != null ? r.amount : BigDecimal.ZERO);
                    paid = paid.add(r.amount != null ? r.amount : BigDecimal.ZERO);
                }
            }
        } catch (Exception ignored) {}

        result.put("deposit", deposit);
        result.put("paid", paid);
        return result;
    }
}