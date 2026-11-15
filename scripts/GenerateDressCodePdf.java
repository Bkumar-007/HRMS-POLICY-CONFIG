import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

/** One-off: mvn -q exec:java -Dexec.mainClass=GenerateDressCodePdf (from backend after compile deps). */
public class GenerateDressCodePdf {

    private static final String[] LINES = {
            "Acme Corp — Dress Code Policy (v1.0)",
            "",
            "Purpose",
            "Sets expectations for professional appearance at Acme Corp offices,",
            "client sites, and company events. Fictional demo policy only.",
            "",
            "General Office Attire",
            "- Business casual is the default for office days.",
            "- Acceptable: collared shirts, blouses, sweaters, chinos, dress trousers,",
            "  knee-length skirts/dresses, closed-toe shoes.",
            "- Not acceptable: ripped clothing, beachwear, flip-flops, offensive slogans.",
            "",
            "Client Meeting Days",
            "- Prefer business formal or neat business casual for external clients.",
            "- Prefer blazer or formal shirt; avoid loud patterns on video.",
            "",
            "Hybrid / Work-From-Home Video Calls",
            "- Visible attire on camera should meet business-casual standards.",
            "- Company-branded apparel is encouraged on all-hands calls.",
            "",
            "Safety & Lab Areas",
            "- Closed-toe shoes are mandatory in labs and facilities floors.",
            "- Secure loose jewelry where machinery is present.",
            "",
            "Religious & Cultural Attire",
            "- Reasonable accommodations are supported via HR.",
            "- Safety requirements still apply in restricted areas.",
            "",
            "Enforcement",
            "- Managers may give a private reminder for first-time issues.",
            "- Repeated non-compliance may escalate to HR (Code of Conduct).",
            "",
            "Questions",
            "- Ask your manager or email hr@acme.example (fictional).",
            "- Or ask the HR assistant chat about this dress code policy."
    };

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "../docs/policies/dress-code-policy.pdf")
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(out.getParent());

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = 750;
                float leading = 16;
                for (int i = 0; i < LINES.length; i++) {
                    String line = LINES[i];
                    if (y < 72) {
                        break;
                    }
                    boolean heading = i == 0
                            || line.equals("Purpose")
                            || line.equals("General Office Attire")
                            || line.equals("Client Meeting Days")
                            || line.equals("Hybrid / Work-From-Home Video Calls")
                            || line.equals("Safety & Lab Areas")
                            || line.equals("Religious & Cultural Attire")
                            || line.equals("Enforcement")
                            || line.equals("Questions");
                    PDType1Font use = heading ? bold : font;
                    float size = i == 0 ? 16 : (heading ? 12 : 11);
                    cs.beginText();
                    cs.setFont(use, size);
                    cs.newLineAtOffset(50, y);
                    cs.showText(line.replace("\t", " "));
                    cs.endText();
                    y -= (i == 0 ? 24 : leading);
                }
            }
            doc.save(out.toFile());
        }
        System.out.println("Wrote " + out);
    }
}
