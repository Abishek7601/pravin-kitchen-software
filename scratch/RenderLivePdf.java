import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class RenderLivePdf {
    public static void main(String[] args) throws Exception {
        File pdfFile = new File("target/live_qt10825.pdf");
        PDDocument doc = Loader.loadPDF(pdfFile);
        PDFRenderer renderer = new PDFRenderer(doc);
        BufferedImage image = renderer.renderImageWithDPI(1, 150); // page index 1 = page 2
        File out = new File("C:/Users/klass/.gemini/antigravity/brain/7ede9cf4-5043-4814-b009-91d49692bee9/live_qt10825.png");
        ImageIO.write(image, "PNG", out);
        doc.close();
        System.out.println("Rendered to " + out.getAbsolutePath());
    }
}

