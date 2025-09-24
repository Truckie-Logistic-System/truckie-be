package capstone_project.common.utils;
import com.itextpdf.layout.Document;

import static capstone_project.common.constants.PdfSettingConstants.*;

public class PdfUtil {
    public void applyStandardBaseFontSize(Document document) {
        // Set a global base font size for the document so child elements inherit unless overridden
        document.setFontSize(BASE_FONT_SIZE);
    }
}
