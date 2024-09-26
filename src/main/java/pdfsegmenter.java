
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class pdfsegmenter {


    // this will analyze the vertical gaps between blocks and text,and also store blocks of text
    static class CustomPDFTextStripper extends PDFTextStripper {
        List<List<TextPosition>> textBlocks = new ArrayList<>();

        public CustomPDFTextStripper() throws IOException {
            super();
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            textBlocks.add(new ArrayList<>(textPositions));
        }

        public List<List<TextPosition>> getTextBlocks() {
            return textBlocks;
        }
    }

    public static void main(String[] args) {
        String inputPDF = "C:\\Users\\Lenovo\\Downloads\\DWDM UNIT2.pdf";  // path to your input file
        int numberOfCuts = 4;  // Making exactly 3 cuts to making application versatile and we can chane according to our requirements


        try {
            PDDocument document = PDDocument.load(new File(inputPDF));
            CustomPDFTextStripper stripper = new CustomPDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.getText(document);

            List<Double> yGaps = analyzeVerticalGaps(stripper.getTextBlocks());

                                                  // it will define significant whitespace based on an average gap threshold
            double significantWhitespaceThreshold = calculateSignificantWhitespaceThreshold(yGaps);
            List<Integer> cutPositions = findLargestGaps(yGaps, numberOfCuts, significantWhitespaceThreshold, document.getNumberOfPages());

            splitPDF(document, cutPositions, "output_segment");
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    // this method will calculate the significant whitespace threshold on the basis of average gaps
    //yGaps - list of vertical gaps between text and bocks
    private static double calculateSignificantWhitespaceThreshold(List<Double> yGaps) {
        double sum = 0;
        int count = 0;
        for (double gap : yGaps) {
            sum += gap;
            count++;
        }
        return (count > 0) ? (sum / count) * 1.5 : 0; // Set threshold to 1.5 times the average gap
    }




    // analyzes the vertical gap between the consecutive text blocks
    // and then return the list of gaps
    private static List<Double> analyzeVerticalGaps(List<List<TextPosition>> textBlocks) {
        List<Double> gaps = new ArrayList<>();

        for (int i = 1;i < textBlocks.size();i++) {
            double previousY = textBlocks.get(i - 1).get(0).getY();
            double currentY = textBlocks.get(i).get(0).getY();
            gaps.add(currentY - previousY);
        }
        return gaps;
    }



    // this will find the largest gaps in the document and determines where to make cut on
    //the basis of number of cuts input and whitespace threshold
    //return the list of pages in which cuts should be made
    private static List<Integer> findLargestGaps(List<Double> yGaps, int numberOfCuts, double threshold, int totalPages) {
        List<Integer> cutPos= new ArrayList<>();

        for (int i = 0; i < yGaps.size(); i++) {
            if (yGaps.get(i) > threshold) {
                if (cutPos.size() < numberOfCuts) {
                    cutPos.add(i + 1); // Store the cut position (next page)
                } else {
                    break; // Stop if we have made enough cuts
                }
            }
        }

        return cutPos;
    }


    // this is the final block which basically spilts the pdf into segments ,
    // and save the segment into new pdf
    private static void splitPDF(PDDocument document, List<Integer> cutPositions, String outputPrefix) throws IOException {
        int totalPages = document.getNumberOfPages();
        int startPage = 0;

        for (int i = 0; i < cutPositions.size(); i++) {
            int endPage = cutPositions.get(i);


            if (endPage > totalPages) {
                System.err.println("Error: Cut position " + endPage + " is out of bounds. Total pages: " + totalPages);
                break;
            }

            PDDocument newDoc = new PDDocument();
            for (int j = startPage; j < endPage; j++) {
                newDoc.addPage(document.getPage(j));
            }

            String segmentFileName = outputPrefix + (i + 1) + ".pdf";
            newDoc.save(segmentFileName);
            System.out.println("Saved segment: " + segmentFileName);
            newDoc.close();

            startPage = endPage;
        }

        // it will save the last segment if pages remain
        if (startPage < totalPages) {
            PDDocument lastDoc = new PDDocument();
            for (int i = startPage; i < totalPages; i++) {
                lastDoc.addPage(document.getPage(i));
            }
            String lastSegmentFileName = outputPrefix + (cutPositions.size() + 1) + ".pdf";
            lastDoc.save(lastSegmentFileName);
            System.out.println("Saved segment: " + lastSegmentFileName);
            lastDoc.close();
        }
    }
}
