import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

public class GenerateDressCodeDocx {

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "../docs/policies/dress-code-policy.docx")
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(out.getParent());

        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph title = doc.createParagraph();
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.setText("Acme Corp - Dress Code Policy (v1.0)");

            for (String line : List.of(
                    "",
                    "Purpose",
                    "Sets expectations for professional appearance at Acme Corp offices, client sites, and company events.",
                    "",
                    "General Office Attire",
                    "Business casual is the default. Acceptable: collared shirts, blouses, chinos, closed-toe shoes.",
                    "Not acceptable: ripped clothing, beachwear, flip-flops, offensive slogans.",
                    "",
                    "Client Meeting Days",
                    "Prefer business formal or neat business casual for external clients.",
                    "",
                    "Hybrid / WFH Video Calls",
                    "Visible attire on camera should meet business-casual standards.",
                    "",
                    "Safety and Lab Areas",
                    "Closed-toe shoes are mandatory in labs and facilities floors.",
                    "",
                    "Religious and Cultural Attire",
                    "Reasonable accommodations are supported via HR.",
                    "",
                    "Questions",
                    "Ask your manager or the HR assistant chat about this dress code policy.")) {
                XWPFParagraph p = doc.createParagraph();
                XWPFRun r = p.createRun();
                if (isHeading(line)) {
                    r.setBold(true);
                    r.setFontSize(12);
                }
                r.setText(line);
            }

            try (var os = Files.newOutputStream(out)) {
                doc.write(os);
            }
        }
        System.out.println("Wrote " + out);
    }

    private static boolean isHeading(String line) {
        return line.equals("Purpose")
                || line.equals("General Office Attire")
                || line.equals("Client Meeting Days")
                || line.equals("Hybrid / WFH Video Calls")
                || line.equals("Safety and Lab Areas")
                || line.equals("Religious and Cultural Attire")
                || line.equals("Questions");
    }
}
